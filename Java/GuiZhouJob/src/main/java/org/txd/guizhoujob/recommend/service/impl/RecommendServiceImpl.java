package org.txd.guizhoujob.recommend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.recommend.entity.RecommendResult;
import org.txd.guizhoujob.recommend.mapper.RecommendMapper;
import org.txd.guizhoujob.recommend.service.RecommendService;
import org.txd.guizhoujob.recommend.support.ContentMatch;
import org.txd.guizhoujob.recommend.support.RecommendScore;
import org.txd.guizhoujob.recommend.support.RecommendScorer;
import org.txd.guizhoujob.recommend.vo.RecommendJobVO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.mapper.SysUserMapper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int CANDIDATE_LIMIT = 3000;

    private final SysUserMapper sysUserMapper;
    private final RecommendMapper recommendMapper;
    private final RecommendScorer recommendScorer;

    public RecommendServiceImpl(
            SysUserMapper sysUserMapper,
            RecommendMapper recommendMapper,
            RecommendScorer recommendScorer
    ) {
        this.sysUserMapper = sysUserMapper;
        this.recommendMapper = recommendMapper;
        this.recommendScorer = recommendScorer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RecommendJobVO> refresh(Long userId, Integer limit) {
        int resultLimit = normalizeLimit(limit);
        SysUser user = getActiveUser(userId);
        List<JobInfoVO> candidates = recommendMapper.selectCandidateJobs(user.getLocalCity(), CANDIDATE_LIMIT);
        Map<Long, ContentMatch> contentMatches = recommendScorer.contentMatches(user, candidates);
        String batchNo = "recommend-" + System.currentTimeMillis();
        AtomicInteger rank = new AtomicInteger(1);

        List<RecommendResult> results = candidates.stream()
                .map(job -> toResult(user, job, recommendScorer.score(user, job, contentMatches.get(job.getId()))))
                .sorted(Comparator.comparing(RecommendResult::getRecommendScore).reversed())
                .limit(resultLimit)
                .peek(result -> {
                    result.setRankNo(rank.getAndIncrement());
                    result.setBatchNo(batchNo);
                })
                .toList();

        recommendMapper.deleteByUserId(userId);
        if (!results.isEmpty()) {
            recommendMapper.batchInsert(results);
        }
        return list(userId, resultLimit);
    }

    @Override
    public List<RecommendJobVO> list(Long userId, Integer limit) {
        getActiveUser(userId);
        List<RecommendJobVO> list = recommendMapper.selectByUserId(userId, normalizeLimit(limit));
        list.forEach(this::fillReasons);
        return list;
    }

    private RecommendResult toResult(SysUser user, JobInfoVO job, RecommendScore score) {
        RecommendResult result = new RecommendResult();
        result.setUserId(user.getId());
        result.setJobId(job.getId());
        result.setRecommendScore(score.getRecommendScore());
        result.setContentScore(score.getContentScore());
        result.setCityScore(score.getCityScore());
        result.setSalaryScore(score.getSalaryScore());
        result.setEducationScore(score.getEducationScore());
        result.setExperienceScore(score.getExperienceScore());
        result.setReasonText(score.reasonText());
        return result;
    }

    private SysUser getActiveUser(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException("请先登录后再查看推荐岗位");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        return user;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void fillReasons(RecommendJobVO vo) {
        if (vo.getReasonText() == null || vo.getReasonText().isBlank()) {
            vo.setReasons(List.of());
            return;
        }
        vo.setReasons(Arrays.stream(vo.getReasonText().split("；"))
                .map(String::trim)
                .filter(reason -> !reason.isEmpty())
                .toList());
    }
}
