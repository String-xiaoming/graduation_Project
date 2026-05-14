package org.txd.guizhoujob.analysis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.txd.guizhoujob.analysis.dto.SkillTfidfQueryDTO;
import org.txd.guizhoujob.analysis.service.AnalysisDashboardService;
import org.txd.guizhoujob.analysis.service.SkillTfidfService;
import org.txd.guizhoujob.analysis.vo.AnalysisDashboardVO;
import org.txd.guizhoujob.analysis.vo.SkillTfidfVO;
import org.txd.guizhoujob.common.Result;

import java.util.List;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final SkillTfidfService skillTfidfService;
    private final AnalysisDashboardService analysisDashboardService;

    public AnalysisController(SkillTfidfService skillTfidfService, AnalysisDashboardService analysisDashboardService) {
        this.skillTfidfService = skillTfidfService;
        this.analysisDashboardService = analysisDashboardService;
    }

    @GetMapping("/dashboard")
    public Result<AnalysisDashboardVO> dashboard() {
        return Result.success(analysisDashboardService.getDashboard());
    }

    @GetMapping("/skill-tfidf")
    public Result<List<SkillTfidfVO>> listSkillTfidf(SkillTfidfQueryDTO dto) {
        return Result.success(skillTfidfService.listLatest(dto));
    }

    @GetMapping("/skill-tfidf/cities")
    public Result<List<String>> listSkillTfidfCities(Integer limit) {
        return Result.success(skillTfidfService.listLatestCities(limit));
    }
}
