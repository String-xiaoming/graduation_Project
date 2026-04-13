import csv
import hashlib
import json
import random
import re
import time
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from urllib.parse import quote

from DrissionPage import ChromiumPage


SOURCE_NAME = "boss_zhipin"
LISTEN_URL = "https://www.zhipin.com/wapi/zpgeek/search/joblist.json"

DEFAULT_CITY = "贵阳"
DEFAULT_TARGET_COUNT = 100

MAX_PAGES_PER_KEYWORD = 3
MAX_ROUNDS = 4
MAX_NEW_PER_KEYWORD_PER_ROUND = 10
PAGE_SLEEP_SECONDS = (0.8, 1.6)

EXPANSION_LIMIT_PER_ROUND = 12
MAX_EXPANSION_KEYWORDS = 80
MAX_TITLE_SAMPLES_PER_KEYWORD = 5

LOW_NOVELTY_THRESHOLD = 0.08
LOW_NOVELTY_STREAK_LIMIT = 6
MIN_PAGES_BEFORE_STOP = 12

BASE_DIR = Path(__file__).resolve().parent
CACHE_DIR = BASE_DIR / "cache"
OUTPUT_DIR = BASE_DIR / "output"

CSV_FIELDS = [
    "job_title",
    "company_name",
    "city",
    "work_address",
    "education_text",
    "experience_text",
    "salary_text",
    "salary_min",
    "salary_max",
    "job_description",
    "publish_time",
    "data_batch_no",
    "job_hash",
    "status",
]

CITY_CODE_MAP = {
    "贵阳": "101260100",
    "guiyang": "101260100",
    "遵义": "101260200",
    "zunyi": "101260200",
    "安顺": "101260300",
    "anshun": "101260300",
    "六盘水": "101260400",
    "liupanshui": "101260400",
    "毕节": "101260500",
    "bijie": "101260500",
    "铜仁": "101260600",
    "tongren": "101260600",
    "黔东南": "101260700",
    "qiandongnan": "101260700",
    "黔南": "101260800",
    "qiannan": "101260800",
    "黔西南": "101260900",
    "qianxinan": "101260900",
}

KEYWORD_CATEGORIES = {
    "tech": ["Java", "Python", "前端", "后端", "测试", "运维", "数据分析", "大数据"],
    "sales": ["销售", "客户经理", "电话销售", "渠道销售", "招商", "商务"],
    "admin": ["行政", "文员", "人事", "前台", "助理", "内勤"],
    "finance": ["会计", "出纳", "财务", "审计", "税务"],
    "education": ["教师", "老师", "培训", "教务", "辅导老师"],
    "medical": ["护士", "医生", "药师", "医助", "康复"],
    "service": ["客服", "收银", "服务员", "店员", "营业员", "导购"],
    "factory": ["操作工", "普工", "质检", "生产", "仓管"],
    "logistics": ["司机", "配送", "仓库", "装卸", "物流"],
    "design_ops": ["设计师", "平面设计", "新媒体", "运营", "剪辑", "文案"],
}

NOISE_PATTERNS = [
    r"急聘",
    r"诚聘",
    r"招聘",
    r"高薪",
    r"双休",
    r"五险一金",
    r"五险",
    r"包吃",
    r"包住",
    r"可兼职",
    r"接受小白",
    r"就近分配",
    r"无需经验",
    r"无经验",
    r"可居家",
    r"可实习",
]

COMMON_SUFFIXES = [
    "助理",
    "专员",
    "主管",
    "经理",
    "工程师",
    "老师",
    "顾问",
    "店长",
    "文员",
    "客服",
    "销售",
    "设计师",
    "运营",
]


def ask_with_default(prompt: str, default: str) -> str:
    value = input(f"{prompt}（默认：{default}）：").strip()
    return value or default


def now_str() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def build_batch_name(city_name: str) -> str:
    return f"{city_name}_{datetime.now().strftime('%y%m%d%H%M')}"


def sanitize_filename(text: str) -> str:
    text = re.sub(r'[<>:"/\\\\|?*]', "_", text).strip()
    return text or "output"


def md5_text(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def parse_salary(salary_text: str):
    if not salary_text:
        return None, None

    text = (
        salary_text.upper()
        .replace("Ｋ", "K")
        .replace("元/月", "")
        .replace("/月", "")
        .replace("·", "")
        .strip()
    )
    match = re.search(r"(\d+(?:\.\d+)?)\s*K\s*-\s*(\d+(?:\.\d+)?)\s*K", text)
    if not match:
        return None, None

    return float(match.group(1)) * 1000, float(match.group(2)) * 1000


def normalize_city(city_input: str) -> tuple[str, str]:
    city_input = city_input.strip()
    city_code = CITY_CODE_MAP.get(city_input)
    if city_code:
        if re.search(r"[\u4e00-\u9fff]", city_input):
            return city_input, city_code

        chinese_name = next(
            name
            for name, code in CITY_CODE_MAP.items()
            if code == city_code and re.search(r"[\u4e00-\u9fff]", name)
        )
        return chinese_name, city_code

    raise ValueError(
        f"暂不支持城市：{city_input}。当前支持：贵阳、遵义、安顺、六盘水、毕节、铜仁、黔东南、黔南、黔西南"
    )


def build_seed_keyword_plan() -> list[str]:
    buckets = []
    for keywords in KEYWORD_CATEGORIES.values():
        shuffled = keywords[:]
        random.shuffle(shuffled)
        buckets.append(shuffled)

    plan = []
    while any(buckets):
        random.shuffle(buckets)
        for bucket in buckets:
            if bucket:
                plan.append(bucket.pop())
    return plan


def build_search_url(city_code: str, keyword: str, page_no: int) -> str:
    return (
        "https://www.zhipin.com/web/geek/job"
        f"?query={quote(keyword)}&city={city_code}&page={page_no}"
    )


def build_work_address(job: dict) -> str:
    parts = [
        job.get("cityName", ""),
        job.get("areaDistrict", ""),
        job.get("businessDistrict", ""),
    ]
    return "-".join([part for part in parts if part])


def build_source_url(job: dict, city_code: str, keyword: str, page_no: int) -> str:
    security_id = job.get("securityId", "")
    encrypt_job_id = job.get("encryptJobId", "")
    if security_id and encrypt_job_id:
        return f"https://www.zhipin.com/job_detail/{encrypt_job_id}.html?securityId={security_id}"
    return build_search_url(city_code, keyword, page_no)


def build_job_hash(job: dict, work_address: str, salary_text: str) -> str:
    primary_key = "|".join(
        [
            job.get("securityId", ""),
            job.get("encryptJobId", ""),
            job.get("jobName", ""),
            job.get("brandName", ""),
            work_address,
            salary_text,
        ]
    )
    return md5_text(primary_key)


def should_keep_job(job: dict) -> bool:
    if not job.get("jobName") or not job.get("brandName"):
        return False
    if job.get("jobValidStatus") not in (None, 1):
        return False
    if job.get("proxyJob") == 1:
        return False
    return True


def normalize_title(title: str) -> str:
    if not title:
        return ""

    text = title.strip()
    text = re.sub(r"[\(\（【\[].*?[\)\）】\]]", " ", text)
    for pattern in NOISE_PATTERNS:
        text = re.sub(pattern, " ", text, flags=re.IGNORECASE)
    text = re.sub(r"[+\-_/|｜、,，;；·]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def split_title_variants(title: str) -> list[str]:
    parts = re.split(r"[+/|｜、,，;；·\s]+", normalize_title(title))
    return [part.strip() for part in parts if part.strip()]


def derive_base_term(term: str) -> list[str]:
    results = []
    for suffix in COMMON_SUFFIXES:
        if term.endswith(suffix) and len(term) > len(suffix) + 1:
            results.append(term[: -len(suffix)])
    return results


def is_good_keyword(term: str) -> bool:
    if not term:
        return False
    if len(term) < 2 or len(term) > 10:
        return False
    if re.fullmatch(r"\d+", term):
        return False
    if re.search(r"[年天月周Kk]", term):
        return False
    if re.search(r"(经验不限|学历不限|本科|大专|硕士|博士|中专|高中)", term):
        return False
    if not re.search(r"[\u4e00-\u9fffA-Za-z]", term):
        return False
    return True


def extract_title_candidates(title: str) -> list[str]:
    candidates = []
    normalized = normalize_title(title)
    if is_good_keyword(normalized):
        candidates.append(normalized)

    for part in split_title_variants(title):
        if is_good_keyword(part):
            candidates.append(part)
        for base in derive_base_term(part):
            if is_good_keyword(base):
                candidates.append(base)

    deduped = []
    seen = set()
    for item in candidates:
        key = item.lower()
        if key in seen:
            continue
        seen.add(key)
        deduped.append(item)
    return deduped


def update_discovered_keywords(title: str, discovered_keywords: dict):
    candidates = extract_title_candidates(title)
    normalized_title = normalize_title(title)

    for term in candidates:
        key = term.lower()
        info = discovered_keywords.setdefault(
            key,
            {
                "keyword": term,
                "count": 0,
                "score": 0,
                "source_titles": [],
            },
        )
        info["count"] += 1
        info["score"] += 3 if term == normalized_title else 1

        if title not in info["source_titles"] and len(info["source_titles"]) < MAX_TITLE_SAMPLES_PER_KEYWORD:
            info["source_titles"].append(title)


def rank_expansion_keywords(discovered_keywords: dict, blocked_keywords: set[str]) -> list[str]:
    ranked = []
    for info in discovered_keywords.values():
        keyword = info["keyword"]
        if keyword.lower() in blocked_keywords:
            continue
        if info["count"] < 2 and info["score"] < 3:
            continue
        ranked.append((info["score"], info["count"], len(keyword), keyword))

    ranked.sort(key=lambda item: (-item[0], -item[1], item[2], item[3]))
    return [item[3] for item in ranked[:MAX_EXPANSION_KEYWORDS]]


def interleave_keywords(seed_keywords: list[str], expansion_keywords: list[str]) -> list[str]:
    seed_queue = seed_keywords[:]
    expansion_queue = expansion_keywords[:EXPANSION_LIMIT_PER_ROUND]
    random.shuffle(seed_queue)

    merged = []
    while seed_queue or expansion_queue:
        if seed_queue:
            merged.append(seed_queue.pop(0))
        if expansion_queue:
            merged.append(expansion_queue.pop(0))
    return merged


def fetch_job_list(dp: ChromiumPage, city_code: str, keyword: str, page_no: int) -> dict:
    search_url = build_search_url(city_code, keyword, page_no)
    dp.listen.clear()
    dp.get(search_url)
    time.sleep(random.uniform(*PAGE_SLEEP_SECONDS))
    dp.scroll.to_bottom()

    for _ in range(4):
        packet = dp.listen.wait(timeout=20)
        if not packet:
            continue
        body = packet.response.body
        if isinstance(body, dict):
            return body

    raise ValueError(f"关键词={keyword} 第 {page_no} 页未拿到有效 JSON")


def normalize_job(job: dict, city_name: str, city_code: str, keyword: str, page_no: int, batch_name: str) -> tuple[dict, dict]:
    work_address = build_work_address(job)
    salary_text = job.get("salaryDesc", "") or ""
    salary_min, salary_max = parse_salary(salary_text)
    job_hash = build_job_hash(job, work_address, salary_text)
    source_url = build_source_url(job, city_code, keyword, page_no)
    skills_text = " ".join(job.get("skills", []) or [])

    csv_row = {
        "job_title": job.get("jobName", ""),
        "company_name": job.get("brandName", ""),
        "city": job.get("cityName", city_name) or city_name,
        "work_address": work_address,
        "education_text": job.get("jobDegree", ""),
        "experience_text": job.get("jobExperience", ""),
        "salary_text": salary_text,
        "salary_min": salary_min,
        "salary_max": salary_max,
        "job_description": skills_text,
        "publish_time": None,
        "data_batch_no": batch_name,
        "job_hash": job_hash,
        "status": 1,
    }

    json_row = {
        **csv_row,
        "source_name": SOURCE_NAME,
        "search_keyword": keyword,
        "source_url": source_url,
        "job_title_raw": job.get("jobName", ""),
        "company_name_raw": job.get("brandName", ""),
        "city_raw": job.get("cityName", city_name),
        "address_raw": work_address,
        "salary_raw": salary_text,
        "education_raw": job.get("jobDegree", ""),
        "experience_raw": job.get("jobExperience", ""),
        "job_description_raw": skills_text,
        "publish_time_raw": "",
        "boss_name": job.get("bossName", ""),
        "boss_title": job.get("bossTitle", ""),
        "company_industry": job.get("brandIndustry", ""),
        "company_scale": job.get("brandScaleName", ""),
        "job_labels": ",".join(job.get("jobLabels", []) or []),
        "skill_keywords_hint": ",".join(job.get("skills", []) or []),
        "welfare_tags": ",".join(job.get("welfareList", []) or []),
        "security_id": job.get("securityId", ""),
        "encrypt_job_id": job.get("encryptJobId", ""),
        "job_valid_status": job.get("jobValidStatus"),
        "proxy_job": job.get("proxyJob"),
        "crawl_time": now_str(),
        "raw_json": json.dumps(job, ensure_ascii=False),
    }
    return csv_row, json_row


def read_seen_hashes(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return {line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()}


def append_seen_hashes(path: Path, hashes: set[str]):
    if not hashes:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        for item in sorted(hashes):
            f.write(item + "\n")


def load_keyword_pool_cache(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def save_keyword_pool_cache(path: Path, city_name: str, discovered_keywords: dict):
    cache = {
        "city_name": city_name,
        "updated_at": now_str(),
        "keywords": {
            info["keyword"]: {
                "count": info["count"],
                "score": info["score"],
                "source_titles": info["source_titles"],
            }
            for info in sorted(
                discovered_keywords.values(),
                key=lambda item: (-item["score"], -item["count"], item["keyword"]),
            )[:MAX_EXPANSION_KEYWORDS]
        },
    }
    path.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")


def save_jsonl(rows: list[dict], path: Path):
    with path.open("w", encoding="utf-8") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")


def save_csv(rows: list[dict], path: Path):
    with path.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def save_meta(path: Path, data: dict):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def main():
    random.seed()

    city_input = ask_with_default("请输入城市", DEFAULT_CITY)
    target_count = int(ask_with_default("请输入目标岗位数量", str(DEFAULT_TARGET_COUNT)))
    city_name, city_code = normalize_city(city_input)

    batch_name = build_batch_name(city_name)
    safe_batch_name = sanitize_filename(batch_name)
    out_dir = OUTPUT_DIR / safe_batch_name
    debug_dir = out_dir / "debug"
    out_dir.mkdir(parents=True, exist_ok=True)
    debug_dir.mkdir(parents=True, exist_ok=True)

    csv_path = out_dir / f"{safe_batch_name}.csv"
    jsonl_path = out_dir / f"{safe_batch_name}.jsonl"
    meta_path = out_dir / "meta.json"

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    seen_cache_path = CACHE_DIR / f"{sanitize_filename(city_name)}_seen_hashes.txt"
    keyword_pool_cache_path = CACHE_DIR / f"{sanitize_filename(city_name)}_keyword_pool.json"

    seen_hashes = read_seen_hashes(seen_cache_path)
    new_hashes = set()

    cached_keywords = load_keyword_pool_cache(keyword_pool_cache_path)
    discovered_keywords = {}
    for keyword, info in cached_keywords.get("keywords", {}).items():
        discovered_keywords[keyword.lower()] = {
            "keyword": keyword,
            "count": int(info.get("count", 0)),
            "score": int(info.get("score", 0)),
            "source_titles": list(info.get("source_titles", []))[:MAX_TITLE_SAMPLES_PER_KEYWORD],
        }

    csv_rows = []
    json_rows = []
    stats = defaultdict(int)
    used_keywords = []
    recent_novelty_rates = []
    low_novelty_streak = 0
    stop_due_to_low_novelty = False

    dp = ChromiumPage()
    dp.listen.start(LISTEN_URL)
    start_time = now_str()

    try:
        for round_no in range(1, MAX_ROUNDS + 1):
            if len(csv_rows) >= target_count or stop_due_to_low_novelty:
                break

            seed_keywords = build_seed_keyword_plan()
            blocked_keywords = {keyword.lower() for keyword in seed_keywords + used_keywords}
            expansion_keywords = rank_expansion_keywords(discovered_keywords, blocked_keywords)
            keyword_plan = interleave_keywords(seed_keywords, expansion_keywords)

            print(
                f"开始第 {round_no} 轮采集，"
                f"种子词 {len(seed_keywords)} 个，扩展词 {min(len(expansion_keywords), EXPANSION_LIMIT_PER_ROUND)} 个"
            )

            for keyword in keyword_plan:
                if len(csv_rows) >= target_count or stop_due_to_low_novelty:
                    break

                used_keywords.append(keyword)
                keyword_new_count = 0
                print(f"当前关键词：{keyword}")

                for page_no in range(1, MAX_PAGES_PER_KEYWORD + 1):
                    if len(csv_rows) >= target_count or stop_due_to_low_novelty:
                        break
                    if keyword_new_count >= MAX_NEW_PER_KEYWORD_PER_ROUND:
                        break

                    try:
                        page_body = fetch_job_list(dp, city_code, keyword, page_no)
                    except Exception as e:
                        print(f"关键词={keyword} 第 {page_no} 页采集失败：{e}")
                        break

                    debug_file = debug_dir / f"{sanitize_filename(keyword)}_page_{page_no}.json"
                    debug_file.write_text(
                        json.dumps(page_body, ensure_ascii=False, indent=2),
                        encoding="utf-8",
                    )

                    code = page_body.get("code")
                    if code != 0:
                        print(f"关键词={keyword} 第 {page_no} 页返回异常：code={code} message={page_body.get('message')}")
                        break

                    job_list = page_body.get("zpData", {}).get("jobList", [])
                    if not job_list:
                        print(f"关键词={keyword} 第 {page_no} 页无数据")
                        break

                    stats["pages_fetched"] += 1
                    stats["jobs_seen"] += len(job_list)

                    new_count_this_page = 0
                    for job in job_list:
                        if not should_keep_job(job):
                            stats["filtered_jobs"] += 1
                            continue

                        csv_row, json_row = normalize_job(
                            job=job,
                            city_name=city_name,
                            city_code=city_code,
                            keyword=keyword,
                            page_no=page_no,
                            batch_name=batch_name,
                        )

                        job_hash = csv_row["job_hash"]
                        if job_hash in seen_hashes or job_hash in new_hashes:
                            stats["duplicate_jobs"] += 1
                            continue

                        csv_rows.append(csv_row)
                        json_rows.append(json_row)
                        new_hashes.add(job_hash)
                        new_count_this_page += 1
                        keyword_new_count += 1

                        update_discovered_keywords(csv_row["job_title"], discovered_keywords)

                        if len(csv_rows) >= target_count:
                            break
                        if keyword_new_count >= MAX_NEW_PER_KEYWORD_PER_ROUND:
                            break

                    novelty_rate = new_count_this_page / max(len(job_list), 1)
                    recent_novelty_rates.append(round(novelty_rate, 4))

                    if stats["pages_fetched"] >= MIN_PAGES_BEFORE_STOP:
                        if novelty_rate < LOW_NOVELTY_THRESHOLD:
                            low_novelty_streak += 1
                        else:
                            low_novelty_streak = 0

                        if low_novelty_streak >= LOW_NOVELTY_STREAK_LIMIT:
                            stop_due_to_low_novelty = True

                    print(
                        f"关键词={keyword} 第 {page_no} 页完成："
                        f"抓到 {len(job_list)} 条，新增 {new_count_this_page} 条，"
                        f"新增率 {novelty_rate:.2%}，"
                        f"当前累计 {len(csv_rows)}/{target_count}，"
                        f"该关键词累计新增 {keyword_new_count}"
                    )

                    if stop_due_to_low_novelty:
                        print("连续低新增率，判定当前城市本轮采集接近饱和，提前停止。")
                        break

                    if new_count_this_page == 0 and page_no >= 2:
                        break

    finally:
        try:
            dp.quit()
        except Exception:
            pass

    append_seen_hashes(seen_cache_path, new_hashes)
    save_keyword_pool_cache(keyword_pool_cache_path, city_name, discovered_keywords)
    save_csv(csv_rows, csv_path)
    save_jsonl(json_rows, jsonl_path)

    end_time = now_str()
    meta = {
        "source_name": SOURCE_NAME,
        "city_name": city_name,
        "city_code": city_code,
        "target_count": target_count,
        "actual_count": len(csv_rows),
        "batch_name": batch_name,
        "output_dir": str(out_dir),
        "csv_path": str(csv_path),
        "jsonl_path": str(jsonl_path),
        "debug_dir": str(debug_dir),
        "seen_cache_path": str(seen_cache_path),
        "keyword_pool_cache_path": str(keyword_pool_cache_path),
        "start_time": start_time,
        "end_time": end_time,
        "max_pages_per_keyword": MAX_PAGES_PER_KEYWORD,
        "max_rounds": MAX_ROUNDS,
        "max_new_per_keyword_per_round": MAX_NEW_PER_KEYWORD_PER_ROUND,
        "expansion_limit_per_round": EXPANSION_LIMIT_PER_ROUND,
        "low_novelty_threshold": LOW_NOVELTY_THRESHOLD,
        "low_novelty_streak_limit": LOW_NOVELTY_STREAK_LIMIT,
        "stop_due_to_low_novelty": stop_due_to_low_novelty,
        "keyword_categories": KEYWORD_CATEGORIES,
        "used_keywords": used_keywords,
        "recent_novelty_rates": recent_novelty_rates[-30:],
        "top_expansion_keywords": rank_expansion_keywords(discovered_keywords, set())[:30],
        "stats": dict(stats),
    }
    save_meta(meta_path, meta)

    print(f"采集完成，城市：{city_name}")
    print(f"目标数量：{target_count}，实际新增：{len(csv_rows)}")
    print(f"CSV  : {csv_path}")
    print(f"JSONL: {jsonl_path}")
    print(f"META : {meta_path}")
    print(f"DEBUG: {debug_dir}")

    if csv_rows:
        print("\n前 5 条示例：")
        for row in csv_rows[:5]:
            print(
                row["job_title"],
                "|",
                row["company_name"],
                "|",
                row["work_address"],
                "|",
                row["experience_text"],
                "|",
                row["education_text"],
            )


if __name__ == "__main__":
    main()
