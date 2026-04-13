package org.txd.guizhoujob.job.service;

import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.entity.JobInfo;

public interface JobService {
    PageResult<JobInfo> page(JobQueryDTO dto);

    JobInfo getById(Long id);
}
