package org.txd.guizhoujob.job.support;

import org.springframework.util.StringUtils;

import java.util.Map;

public final class JobTableResolver {
    public static final String MASTER_TABLE = "job_info";

    private static final Map<String, String> CITY_TABLE_MAP = Map.of(
            "贵阳", "job_info_guiyang",
            "遵义", "job_info_zunyi",
            "安顺", "job_info_anshun",
            "六盘水", "job_info_liupanshui",
            "毕节", "job_info_bijie",
            "铜仁", "job_info_tongren",
            "黔东南", "job_info_qiandongnan",
            "黔南", "job_info_qiannan",
            "黔西南", "job_info_qianxinan"
    );

    private JobTableResolver() {
    }

    public static String tableForQueryCity(String city) {
        if (!StringUtils.hasText(city)) {
            return MASTER_TABLE;
        }
        return CITY_TABLE_MAP.getOrDefault(city.trim(), MASTER_TABLE);
    }

    public static String tableForExactCity(String city) {
        if (!StringUtils.hasText(city)) {
            return null;
        }
        return CITY_TABLE_MAP.get(city.trim());
    }
}
