package org.txd.guizhoujob.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.txd.guizhoujob.analysis.vo.AnalysisDashboardVO;

import java.util.List;

@Mapper
public interface AnalysisDashboardMapper {
    AnalysisDashboardVO.Overview selectOverview();

    List<AnalysisDashboardVO.NameValue> selectCityJobDistribution();

    Integer selectMedianSalary();

    List<AnalysisDashboardVO.NameValue> selectSalaryBuckets();

    List<AnalysisDashboardVO.SalaryBox> selectCitySalaryBoxes();

    List<AnalysisDashboardVO.HeatmapItem> selectEducationExperienceHeatmap();

    List<AnalysisDashboardVO.CategoryItem> selectCategoryDistribution();
}
