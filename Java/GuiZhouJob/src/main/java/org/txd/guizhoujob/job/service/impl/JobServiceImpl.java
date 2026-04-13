package org.txd.guizhoujob.job.service.impl;

import org.springframework.stereotype.Service;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.entity.JobInfo;
import org.txd.guizhoujob.job.mapper.JobInfoMapper;
import org.txd.guizhoujob.job.service.JobService;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobInfoMapper jobInfoMapper;

    public JobServiceImpl(JobInfoMapper jobInfoMapper) {
        this.jobInfoMapper = jobInfoMapper;
    }

    @Override
    public PageResult<JobInfo> page(JobQueryDTO dto) {
        if (dto.getPageNum() == null || dto.getPageNum() < 1) {
            dto.setPageNum(1);
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1) {
            dto.setPageSize(10);
        }

        Long total = jobInfoMapper.countByCondition(dto);
        List<JobInfo> list = jobInfoMapper.selectPageByCondition(dto);

        return new PageResult<>(total, dto.getPageNum(), dto.getPageSize(), list);
    }

    @Override
    public JobInfo getById(Long id) {
        JobInfo jobInfo = jobInfoMapper.selectById(id);
        if (jobInfo == null) {
            throw new BusinessException("岗位不存在");
        }
        return jobInfo;
    }
}
