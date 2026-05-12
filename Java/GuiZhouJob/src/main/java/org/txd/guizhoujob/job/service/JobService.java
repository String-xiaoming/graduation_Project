package org.txd.guizhoujob.job.service;

import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.vo.JobInfoVO;

import java.util.List;

public interface JobService {
    PageResult<JobInfoVO> page(JobQueryDTO dto);

    JobInfoVO getById(Long id);

    List<JobInfoVO> hot(Integer limit);
}
