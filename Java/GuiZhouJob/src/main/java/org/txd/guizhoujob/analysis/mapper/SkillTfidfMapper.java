package org.txd.guizhoujob.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.analysis.dto.SkillTfidfQueryDTO;
import org.txd.guizhoujob.analysis.vo.SkillTfidfVO;

import java.util.List;

@Mapper
public interface SkillTfidfMapper {
    List<SkillTfidfVO> selectLatest(SkillTfidfQueryDTO dto);

    List<String> selectLatestCities(@Param("limit") Integer limit);
}
