package org.txd.guizhoujob.job.service.impl;

import org.springframework.stereotype.Service;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.mapper.JobInfoMapper;
import org.txd.guizhoujob.job.service.JobService;
import org.txd.guizhoujob.job.vo.JobInfoVO;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobInfoMapper jobInfoMapper;

    public JobServiceImpl(JobInfoMapper jobInfoMapper) {
        this.jobInfoMapper = jobInfoMapper;
    }

    @Override
    public PageResult<JobInfoVO> page(JobQueryDTO dto) {
        if (dto == null) {
            dto = new JobQueryDTO();
        }
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10);
        }
        if (dto.getPageSize() > 50) {
            dto.setPageSize(50);
        }

        Long total = jobInfoMapper.countByCondition(dto);
        List<JobInfoVO> list = jobInfoMapper.selectPageByCondition(dto);

        return new PageResult<>(total, dto.getPageNum(), dto.getPageSize(), list);
    }

    @Override
    public JobInfoVO getById(Long id) {
        if (id == null || id < 1) {
            throw new BusinessException("岗位ID不正确");
        }
        JobInfoVO jobInfo = jobInfoMapper.selectById(id);
        if (jobInfo == null) {
            throw new BusinessException("岗位不存在");
        }
        return jobInfo;
    }

    @Override
    public List<JobInfoVO> hot(Integer limit) {
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 30) {
            limit = 30;
        }
        return jobInfoMapper.selectHot(limit);
    }
}
