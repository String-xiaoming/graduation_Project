package org.txd.guizhoujob.analysis.service;

import org.txd.guizhoujob.analysis.dto.SkillTfidfQueryDTO;
import org.txd.guizhoujob.analysis.vo.SkillTfidfVO;

import java.util.List;

public interface SkillTfidfService {
    List<SkillTfidfVO> listLatest(SkillTfidfQueryDTO dto);

    List<String> listLatestCities(Integer limit);
}
