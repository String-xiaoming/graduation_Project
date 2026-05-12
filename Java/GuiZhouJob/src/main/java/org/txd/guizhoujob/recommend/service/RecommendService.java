package org.txd.guizhoujob.recommend.service;

import org.txd.guizhoujob.recommend.vo.RecommendJobVO;

import java.util.List;

public interface RecommendService {
    List<RecommendJobVO> refresh(Long userId, Integer limit);

    List<RecommendJobVO> list(Long userId, Integer limit);
}
