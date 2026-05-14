package org.txd.guizhoujob.recommend.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentMatch {
    private double score;
    private List<String> matchedTerms = new ArrayList<>();
}
