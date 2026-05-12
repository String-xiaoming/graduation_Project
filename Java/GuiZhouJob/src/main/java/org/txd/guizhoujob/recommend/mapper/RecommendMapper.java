package org.txd.guizhoujob.recommend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.recommend.entity.RecommendResult;
import org.txd.guizhoujob.recommend.vo.RecommendJobVO;

import java.util.List;

@Mapper
public interface RecommendMapper {
    List<JobInfoVO> selectCandidateJobs(@Param("city") String city, @Param("limit") Integer limit);

    int deleteByUserId(@Param("userId") Long userId);

    int batchInsert(@Param("list") List<RecommendResult> list);

    List<RecommendJobVO> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
}
