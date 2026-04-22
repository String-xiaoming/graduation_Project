package org.txd.guizhoujob.admin.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.txd.guizhoujob.admin.dto.AdminJobQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminJobUpdateDTO;
import org.txd.guizhoujob.admin.dto.AdminUserQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminUserSaveDTO;
import org.txd.guizhoujob.admin.service.AdminService;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.mapper.JobInfoMapper;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.mapper.SysUserMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final JobInfoMapper jobInfoMapper;

    public AdminServiceImpl(SysUserMapper sysUserMapper, JobInfoMapper jobInfoMapper) {
        this.sysUserMapper = sysUserMapper;
        this.jobInfoMapper = jobInfoMapper;
    }

    @Override
    public PageResult<SysUser> listUsers(Long adminUserId, AdminUserQueryDTO dto) {
        checkAdmin(adminUserId);
        normalizeUserQuery(dto);

        Long total = sysUserMapper.countByAdminCondition(dto);
        List<SysUser> list = sysUserMapper.selectByAdminCondition(dto);
        list.forEach(user -> user.setPasswordHash(null));
        return new PageResult<>(total, dto.getPageNum(), dto.getPageSize(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(Long adminUserId, AdminUserSaveDTO dto) {
        checkAdmin(adminUserId);
        if (!StringUtils.hasText(dto.getEmail()) || !StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("邮箱和密码不能为空");
        }
        if (!StringUtils.hasText(dto.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }
        if (sysUserMapper.selectByEmail(dto.getEmail()) != null) {
            throw new BusinessException("邮箱已存在");
        }

        SysUser user = buildUser(dto);
        user.setPasswordHash(md5(dto.getPassword()));

        int rows = sysUserMapper.insertByAdmin(user);
        if (rows <= 0) {
            throw new BusinessException("新增用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long adminUserId, Long id, AdminUserSaveDTO dto) {
        checkAdmin(adminUserId);
        SysUser existed = sysUserMapper.selectById(id);
        if (existed == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(dto.getEmail())) {
            SysUser sameEmailUser = sysUserMapper.selectByEmail(dto.getEmail());
            if (sameEmailUser != null && !sameEmailUser.getId().equals(id)) {
                throw new BusinessException("邮箱已被其他用户使用");
            }
        }

        SysUser user = buildUser(dto);
        user.setId(id);
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPasswordHash(md5(dto.getPassword()));
        }

        int rows = sysUserMapper.updateByAdmin(user);
        if (rows <= 0) {
            throw new BusinessException("更新用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long adminUserId, Long id) {
        checkAdmin(adminUserId);
        if (adminUserId.equals(id)) {
            throw new BusinessException("不能删除当前登录的管理员账号");
        }

        int rows = sysUserMapper.disableById(id);
        if (rows <= 0) {
            throw new BusinessException("删除用户失败");
        }
    }

    @Override
    public PageResult<JobInfoVO> listJobs(Long adminUserId, AdminJobQueryDTO dto) {
        checkAdmin(adminUserId);
        normalizeJobQuery(dto);

        Long total = jobInfoMapper.countByAdminCondition(dto);
        List<JobInfoVO> list = jobInfoMapper.selectAdminPageByCondition(dto);
        return new PageResult<>(total, dto.getPageNum(), dto.getPageSize(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(Long adminUserId, Long id, AdminJobUpdateDTO dto) {
        checkAdmin(adminUserId);
        dto.setId(id);

        int rows = jobInfoMapper.updateByAdmin(dto);
        if (rows <= 0) {
            throw new BusinessException("更新岗位失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long adminUserId, Long id) {
        checkAdmin(adminUserId);

        int rows = jobInfoMapper.disableById(id);
        if (rows <= 0) {
            throw new BusinessException("删除岗位失败");
        }
    }

    private void checkAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new BusinessException("请先登录管理员账号");
        }

        SysUser user = sysUserMapper.selectById(adminUserId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("管理员账号不可用");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException("没有管理员权限");
        }
    }

    private SysUser buildUser(AdminUserSaveDTO dto) {
        SysUser user = new SysUser();
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "USER");
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setLocalCity(dto.getLocalCity());
        user.setEducationText(dto.getEducationText());
        user.setExpectedPosition(dto.getExpectedPosition());
        user.setExpectedSalaryMin(dto.getExpectedSalaryMin());
        user.setExpectedSalaryMax(dto.getExpectedSalaryMax());
        user.setSkillInputText(dto.getSkillInputText());
        return user;
    }

    private void normalizeUserQuery(AdminUserQueryDTO dto) {
        dto.setPageNum(dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum());
        dto.setPageSize(dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : Math.min(dto.getPageSize(), 100));
    }

    private void normalizeJobQuery(AdminJobQueryDTO dto) {
        dto.setPageNum(dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum());
        dto.setPageSize(dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : Math.min(dto.getPageSize(), 100));
    }

    private String md5(String source) {
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
