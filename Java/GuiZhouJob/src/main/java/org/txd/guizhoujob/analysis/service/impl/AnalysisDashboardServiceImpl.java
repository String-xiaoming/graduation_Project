package org.txd.guizhoujob.analysis.service.impl;

import org.springframework.stereotype.Service;
import org.txd.guizhoujob.analysis.dto.SkillTfidfQueryDTO;
import org.txd.guizhoujob.analysis.mapper.AnalysisDashboardMapper;
import org.txd.guizhoujob.analysis.mapper.SkillTfidfMapper;
import org.txd.guizhoujob.analysis.service.AnalysisDashboardService;
import org.txd.guizhoujob.analysis.vo.AnalysisDashboardVO;
import org.txd.guizhoujob.analysis.vo.SkillTfidfVO;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisDashboardServiceImpl implements AnalysisDashboardService {

    private final AnalysisDashboardMapper dashboardMapper;
    private final SkillTfidfMapper skillTfidfMapper;

    public AnalysisDashboardServiceImpl(AnalysisDashboardMapper dashboardMapper, SkillTfidfMapper skillTfidfMapper) {
        this.dashboardMapper = dashboardMapper;
        this.skillTfidfMapper = skillTfidfMapper;
    }

    @Override
    public AnalysisDashboardVO getDashboard() {
        AnalysisDashboardVO dashboard = new AnalysisDashboardVO();
        AnalysisDashboardVO.Overview overview = dashboardMapper.selectOverview();
        List<AnalysisDashboardVO.NameValue> cityJobDistribution = dashboardMapper.selectCityJobDistribution();
        List<AnalysisDashboardVO.CategoryItem> categoryDistribution = dashboardMapper.selectCategoryDistribution();

        enrichOverview(overview, cityJobDistribution, categoryDistribution);
        dashboard.setOverview(overview);
        dashboard.setCityJobDistribution(cityJobDistribution);
        dashboard.setSalaryBuckets(dashboardMapper.selectSalaryBuckets());
        dashboard.setCitySalaryBoxes(dashboardMapper.selectCitySalaryBoxes());
        dashboard.setEducationExperienceHeatmap(dashboardMapper.selectEducationExperienceHeatmap());
        dashboard.setCategoryDistribution(categoryDistribution);

        SkillTfidfQueryDTO dto = new SkillTfidfQueryDTO();
        dto.setCity("全省");
        dto.setLimit(100);
        dashboard.setSkillKeywords(dedupeSkills(skillTfidfMapper.selectLatest(dto), 20));
        return dashboard;
    }

    private void enrichOverview(
            AnalysisDashboardVO.Overview overview,
            List<AnalysisDashboardVO.NameValue> cityJobDistribution,
            List<AnalysisDashboardVO.CategoryItem> categoryDistribution) {
        if (overview == null) {
            return;
        }
        overview.setMedianSalary(dashboardMapper.selectMedianSalary());
        if (cityJobDistribution != null && !cityJobDistribution.isEmpty()) {
            overview.setTopCity(cityJobDistribution.get(0).getName());
        }
        if (categoryDistribution != null && !categoryDistribution.isEmpty()) {
            overview.setTopCategory(categoryDistribution.get(0).getName());
        }
    }

    private List<SkillTfidfVO> dedupeSkills(List<SkillTfidfVO> source, int limit) {
        Map<String, SkillTfidfVO> map = new LinkedHashMap<>();
        for (SkillTfidfVO item : source) {
            if (item.getKeyword() == null) {
                continue;
            }
            SkillTfidfVO existed = map.get(item.getKeyword());
            if (existed == null || item.getTfidfScore().compareTo(existed.getTfidfScore()) > 0) {
                map.put(item.getKeyword(), item);
            }
        }
        return map.values().stream()
                .sorted(Comparator.comparing(SkillTfidfVO::getTfidfScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

}
