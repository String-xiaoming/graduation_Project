package org.txd.guizhoujob.analysis.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.txd.guizhoujob.analysis.dto.SkillTfidfQueryDTO;
import org.txd.guizhoujob.analysis.mapper.SkillTfidfMapper;
import org.txd.guizhoujob.analysis.service.SkillTfidfService;
import org.txd.guizhoujob.analysis.vo.SkillTfidfVO;

import java.util.List;

@Service
public class SkillTfidfServiceImpl implements SkillTfidfService {

    private final SkillTfidfMapper skillTfidfMapper;

    public SkillTfidfServiceImpl(SkillTfidfMapper skillTfidfMapper) {
        this.skillTfidfMapper = skillTfidfMapper;
    }

    @Override
    public List<SkillTfidfVO> listLatest(SkillTfidfQueryDTO dto) {
        if (!StringUtils.hasText(dto.getCity())) {
            dto.setCity("全省");
        }
        if (dto.getLimit() == null || dto.getLimit() < 1) {
            dto.setLimit(20);
        }
        dto.setLimit(Math.min(dto.getLimit(), 100));
        return skillTfidfMapper.selectLatest(dto);
    }

    @Override
    public List<String> listLatestCities(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        return skillTfidfMapper.selectLatestCities(safeLimit);
    }
}
