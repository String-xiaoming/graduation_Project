package org.txd.guizhoujob.recommend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.txd.guizhoujob.common.Result;
import org.txd.guizhoujob.recommend.service.RecommendService;
import org.txd.guizhoujob.recommend.vo.RecommendJobVO;

import java.util.List;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/refresh")
    public Result<List<RecommendJobVO>> refresh(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return Result.success(recommendService.refresh(userId, limit));
    }

    @GetMapping("/list")
    public Result<List<RecommendJobVO>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return Result.success(recommendService.list(userId, limit));
    }
}
