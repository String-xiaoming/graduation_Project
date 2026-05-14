package org.txd.guizhoujob.analysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisDashboardVO {
    private Overview overview;
    private List<NameValue> cityJobDistribution;
    private List<NameValue> salaryBuckets;
    private List<SalaryBox> citySalaryBoxes;
    private List<HeatmapItem> educationExperienceHeatmap;
    private List<CategoryItem> categoryDistribution;
    private List<SkillTfidfVO> skillKeywords;

    @Data
    public static class Overview {
        private Long totalJobs;
        private Long activeJobs;
        private Long salaryReadyJobs;
        private Long descriptionReadyJobs;
        private Long cityReadyJobs;
        private Long companyCount;
        private Long cityCount;
        private Long batchCount;
        private String minPublishTime;
        private String maxPublishTime;
        private Integer avgSalary;
        private Integer medianSalary;
        private String topCity;
        private String topCategory;
    }

    @Data
    public static class NameValue {
        private String name;
        private Long value;
    }

    @Data
    public static class HeatmapItem {
        private String x;
        private String y;
        private Long value;
    }

    @Data
    public static class SalaryBox {
        private String city;
        private Integer min;
        private Integer q1;
        private Integer median;
        private Integer q3;
        private Integer max;
    }

    @Data
    public static class CategoryItem {
        private String name;
        private Long value;
        private Integer avgSalary;
    }
}
