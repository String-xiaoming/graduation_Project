package org.txd.guizhoujob.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.entity.JobInfo;

import java.util.List;

@Mapper
public interface JobInfoMapper {
    Long countByCondition(JobQueryDTO dto);

    List<JobInfo> selectPageByCondition(JobQueryDTO dto);

    JobInfo selectById(@Param("id") Long id);
}
