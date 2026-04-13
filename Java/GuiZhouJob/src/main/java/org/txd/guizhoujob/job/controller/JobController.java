package org.txd.guizhoujob.job.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.common.Result;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.entity.JobInfo;
import org.txd.guizhoujob.job.service.JobService;

@RestController
@RequestMapping("/job")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/page")
    public Result<PageResult<JobInfo>> page(JobQueryDTO dto) {
        return Result.success(jobService.page(dto));
    }

    @GetMapping("/{id}")
    public Result<JobInfo> getById(@PathVariable Long id) {
        return Result.success(jobService.getById(id));
    }
}
