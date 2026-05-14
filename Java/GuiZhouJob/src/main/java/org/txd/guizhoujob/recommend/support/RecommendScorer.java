package org.txd.guizhoujob.recommend.support;

import org.springframework.stereotype.Component;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.user.entity.SysUser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecommendScorer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9+#.]+|[\\u4e00-\\u9fa5]+");

    public Map<Long, ContentMatch> contentMatches(SysUser user, List<JobInfoVO> jobs) {
        Map<String, Integer> userTf = termFrequency(userProfileText(user));
        Map<Long, Map<String, Integer>> jobTfMap = new LinkedHashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();

        for (JobInfoVO job : jobs) {
            Map<String, Integer> jobTf = termFrequency(jobProfileText(job));
            jobTfMap.put(job.getId(), jobTf);
            for (String term : jobTf.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        int documentCount = Math.max(jobs.size(), 1);
        Map<String, Double> userVector = tfidfVector(userTf, documentFrequency, documentCount);
        double userNorm = norm(userVector);

        Map<Long, ContentMatch> matches = new HashMap<>();
        for (JobInfoVO job : jobs) {
            Map<String, Integer> jobTf = jobTfMap.getOrDefault(job.getId(), Map.of());
            Map<String, Double> jobVector = tfidfVector(jobTf, documentFrequency, documentCount);
            double similarity = cosine(userVector, userNorm, jobVector);
            List<String> matchedTerms = matchedTerms(userVector, jobTf);
            matches.put(job.getId(), new ContentMatch(round(similarity * 100), matchedTerms));
        }
        return matches;
    }

    public RecommendScore score(SysUser user, JobInfoVO job, ContentMatch contentMatch) {
        double contentScore = contentMatch != null ? contentMatch.getScore() : 0;
        List<String> matchedTerms = contentMatch != null ? contentMatch.getMatchedTerms() : List.of();
        double cityScore = cityScore(user, job);
        double salaryScore = salaryScore(user, job);
        double educationScore = educationScore(user, job);
        double experienceScore = experienceScore(job);

        double total = contentScore * 0.45
                + cityScore * 0.20
                + salaryScore * 0.15
                + educationScore * 0.10
                + experienceScore * 0.10;

        RecommendScore score = new RecommendScore();
        score.setContentScore(round(contentScore));
        score.setCityScore(round(cityScore));
        score.setSalaryScore(round(salaryScore));
        score.setEducationScore(round(educationScore));
        score.setExperienceScore(round(experienceScore));
        score.setRecommendScore(round(total));
        score.setReasons(reasons(score, matchedTerms));
        return score;
    }

    private String userProfileText(SysUser user) {
        return joinText(
                user.getExpectedPosition(),
                user.getExpectedPosition(),
                user.getSkillInputText(),
                user.getSkillInputText(),
                user.getEducationText(),
                user.getLocalCity()
        );
    }

    private String jobProfileText(JobInfoVO job) {
        return joinText(
                job.getJobTitle(),
                job.getJobTitle(),
                job.getJobDescription(),
                job.getEducationText(),
                job.getExperienceText(),
                job.getCity()
        );
    }

    private String joinText(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (hasText(part)) {
                builder.append(part).append(' ');
            }
        }
        return builder.toString();
    }

    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String token : tokenize(text)) {
            frequency.merge(token, 1, Integer::sum);
        }
        return frequency;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalize(text));
        while (matcher.find()) {
            String token = matcher.group();
            if (isAsciiToken(token)) {
                addAsciiToken(tokens, token);
            } else {
                addChineseTokens(tokens, token);
            }
        }
        return tokens;
    }

    private void addAsciiToken(List<String> tokens, String token) {
        if (token.length() >= 2 && !isStopWord(token)) {
            tokens.add(token);
        }
    }

    private void addChineseTokens(List<String> tokens, String text) {
        if (text.length() < 2) {
            return;
        }
        if (text.length() <= 6 && !isStopWord(text)) {
            tokens.add(text);
        }
        for (int size = 2; size <= 4; size++) {
            if (text.length() < size) {
                continue;
            }
            for (int i = 0; i <= text.length() - size; i++) {
                String token = text.substring(i, i + size);
                if (!isStopWord(token)) {
                    tokens.add(token);
                }
            }
        }
    }

    private Map<String, Double> tfidfVector(
            Map<String, Integer> termFrequency,
            Map<String, Integer> documentFrequency,
            int documentCount
    ) {
        Map<String, Double> vector = new HashMap<>();
        int maxFrequency = termFrequency.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue() / (double) maxFrequency;
            int df = documentFrequency.getOrDefault(term, 0);
            double idf = Math.log((documentCount + 1.0) / (df + 1.0)) + 1.0;
            vector.put(term, tf * idf);
        }
        return vector;
    }

    private double cosine(Map<String, Double> userVector, double userNorm, Map<String, Double> jobVector) {
        double jobNorm = norm(jobVector);
        if (userVector.isEmpty() || jobVector.isEmpty() || userNorm == 0 || jobNorm == 0) {
            return 0;
        }
        double dot = 0;
        for (Map.Entry<String, Double> entry : userVector.entrySet()) {
            dot += entry.getValue() * jobVector.getOrDefault(entry.getKey(), 0.0);
        }
        return dot / (userNorm * jobNorm);
    }

    private double norm(Map<String, Double> vector) {
        double sum = 0;
        for (double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private List<String> matchedTerms(Map<String, Double> userVector, Map<String, Integer> jobTf) {
        Set<String> jobTerms = new HashSet<>(jobTf.keySet());
        return userVector.entrySet().stream()
                .filter(entry -> jobTerms.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .limit(4)
                .toList();
    }

    private double cityScore(SysUser user, JobInfoVO job) {
        if (!hasText(user.getLocalCity())) {
            return 60;
        }
        if (normalize(user.getLocalCity()).equals(normalize(job.getCity()))) {
            return 100;
        }
        return 35;
    }

    private double salaryScore(SysUser user, JobInfoVO job) {
        BigDecimal userMin = user.getExpectedSalaryMin();
        BigDecimal userMax = user.getExpectedSalaryMax();
        BigDecimal jobMin = job.getSalaryMin();
        BigDecimal jobMax = job.getSalaryMax();

        if (userMin == null && userMax == null) {
            return 60;
        }
        if (jobMin == null || jobMax == null) {
            return 50;
        }

        BigDecimal expectedMin = userMin != null ? userMin : userMax;
        BigDecimal expectedMax = userMax != null ? userMax : userMin;
        if (jobMax.compareTo(expectedMin) >= 0 && jobMin.compareTo(expectedMax) <= 0) {
            return 100;
        }
        if (jobMax.compareTo(expectedMin) < 0) {
            BigDecimal gap = expectedMin.subtract(jobMax);
            return gap.compareTo(BigDecimal.valueOf(1000)) <= 0 ? 75 : 55;
        }
        return 85;
    }

    private double educationScore(SysUser user, JobInfoVO job) {
        int jobLevel = educationLevel(job.getEducationText());
        int userLevel = educationLevel(user.getEducationText());
        if (jobLevel <= 0) {
            return 90;
        }
        if (userLevel < 0) {
            return 60;
        }
        if (userLevel >= jobLevel) {
            return 100;
        }
        if (jobLevel - userLevel == 1) {
            return 65;
        }
        return 45;
    }

    private double experienceScore(JobInfoVO job) {
        String experience = normalize(job.getExperienceText());
        if (!hasText(experience)) {
            return 60;
        }
        if (experience.contains("不限") || experience.contains("应届") || experience.contains("无经验")) {
            return 90;
        }
        return 60;
    }

    private int educationLevel(String educationText) {
        String text = normalize(educationText);
        if (!hasText(text)) {
            return -1;
        }
        if (text.contains("不限") || text.contains("无要求")) {
            return 0;
        }
        if (text.contains("博士")) {
            return 7;
        }
        if (text.contains("硕士") || text.contains("研究生")) {
            return 6;
        }
        if (text.contains("本科")) {
            return 5;
        }
        if (text.contains("大专") || text.contains("专科")) {
            return 4;
        }
        if (text.contains("中专") || text.contains("高中")) {
            return 3;
        }
        if (text.contains("初中")) {
            return 2;
        }
        return -1;
    }

    private List<String> reasons(RecommendScore score, List<String> matchedTerms) {
        List<String> reasons = new ArrayList<>();
        if (score.getCityScore() >= 90) {
            reasons.add("同城岗位");
        }
        if (!matchedTerms.isEmpty()) {
            reasons.add("TF-IDF匹配词：" + String.join("、", matchedTerms));
        }
        if (score.getSalaryScore() >= 90) {
            reasons.add("薪资符合期望");
        }
        if (score.getEducationScore() >= 90) {
            reasons.add("学历要求符合");
        }
        if (score.getContentScore() >= 20) {
            reasons.add("岗位内容与求职画像相似");
        }
        if (reasons.isEmpty()) {
            reasons.add("岗位信息与当前求职画像有一定相关性");
        }
        return reasons;
    }

    private boolean isAsciiToken(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private boolean isStopWord(String token) {
        return token.length() < 2
                || "岗位".equals(token)
                || "职位".equals(token)
                || "工作".equals(token)
                || "要求".equals(token)
                || "负责".equals(token)
                || "经验".equals(token)
                || "学历".equals(token)
                || "公司".equals(token)
                || "使用".equals(token)
                || "了解".equals(token)
                || "熟悉".equals(token)
                || "优先".equals(token)
                || "the".equals(token)
                || "and".equals(token)
                || "with".equals(token);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
