package org.txd.guizhoujob.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.job.dto.JobQueryDTO;
import org.txd.guizhoujob.job.vo.JobInfoVO;

import java.util.List;

@Mapper
public interface JobInfoMapper {
    Long countByCondition(JobQueryDTO dto);

    List<JobInfoVO> selectPageByCondition(JobQueryDTO dto);

    JobInfoVO selectById(@Param("id") Long id);

    List<JobInfoVO> selectHot(@Param("limit") Integer limit);
}
