package org.txd.guizhoujob.recommend.support;

import org.springframework.stereotype.Component;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.user.entity.SysUser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RecommendScorer {

    public RecommendScore score(SysUser user, JobInfoVO job) {
        List<String> matchedSkills = new ArrayList<>();
        double contentScore = contentScore(user, job, matchedSkills);
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
        score.setReasons(reasons(user, job, score, matchedSkills));
        return score;
    }

    private double contentScore(SysUser user, JobInfoVO job, List<String> matchedSkills) {
        Set<String> tokens = userTokens(user);
        String expectedPosition = normalize(user.getExpectedPosition());
        String title = normalize(job.getJobTitle());
        String description = normalize(job.getJobDescription());
        String jobText = title + " " + description;

        double score = 0;
        if (hasText(expectedPosition)) {
            if (title.contains(expectedPosition)) {
                score += 42;
            } else if (jobText.contains(expectedPosition)) {
                score += 26;
            }
        }

        for (String token : tokens) {
            if (title.contains(token)) {
                score += 16;
                matchedSkills.add(token);
            } else if (description.contains(token)) {
                score += 9;
                matchedSkills.add(token);
            }
            if (score >= 100) {
                break;
            }
        }

        if (score == 0 && tokens.isEmpty() && !hasText(expectedPosition)) {
            return 45;
        }
        return Math.min(100, score);
    }

    private Set<String> userTokens(SysUser user) {
        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, user.getExpectedPosition());
        addTokens(tokens, user.getSkillInputText());
        return tokens;
    }

    private void addTokens(Set<String> tokens, String text) {
        if (!hasText(text)) {
            return;
        }
        String[] parts = text.split("[,，、;；/|\\s]+");
        for (String part : parts) {
            String token = normalize(part);
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
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

    private List<String> reasons(SysUser user, JobInfoVO job, RecommendScore score, List<String> matchedSkills) {
        List<String> reasons = new ArrayList<>();
        if (score.getCityScore() >= 90) {
            reasons.add("同城岗位");
        }
        if (!matchedSkills.isEmpty()) {
            reasons.add("匹配技能：" + String.join("、", matchedSkills.stream().distinct().limit(4).toList()));
        }
        if (score.getSalaryScore() >= 90) {
            reasons.add("薪资符合期望");
        }
        if (score.getEducationScore() >= 90) {
            reasons.add("学历要求符合");
        }
        if (score.getContentScore() >= 70) {
            reasons.add("岗位方向接近期望");
        }
        if (reasons.isEmpty()) {
            reasons.add("岗位信息与当前求职画像有一定相关性");
        }
        return reasons;
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
