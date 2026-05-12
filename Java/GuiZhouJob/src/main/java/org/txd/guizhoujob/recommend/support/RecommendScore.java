package org.txd.guizhoujob.recommend.support;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecommendScore {
    private double recommendScore;
    private double contentScore;
    private double cityScore;
    private double salaryScore;
    private double educationScore;
    private double experienceScore;
    private List<String> reasons = new ArrayList<>();

    public String reasonText() {
        return String.join("；", reasons);
    }
}
