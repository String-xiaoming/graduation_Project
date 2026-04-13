package org.txd.guizhoujob.user.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.user.dto.UserLoginDTO;
import org.txd.guizhoujob.user.dto.UserProfileUpdateDTO;
import org.txd.guizhoujob.user.dto.UserRegisterDTO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.mapper.SysUserMapper;
import org.txd.guizhoujob.user.service.UserService;
import org.txd.guizhoujob.user.vo.UserLoginVO;

import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    public UserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        SysUser existedUser = sysUserMapper.selectByEmail(dto.getEmail());
        if (existedUser != null) {
            throw new BusinessException("邮箱已注册");
        }

        SysUser user = new SysUser();
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setPasswordHash(md5(dto.getPassword()));
        user.setStatus(1);

        int rows = sysUserMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("注册失败");
        }
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        SysUser user = sysUserMapper.selectByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String passwordHash = md5(dto.getPassword());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserProfileUpdateDTO dto) {
        SysUser existedUser = sysUserMapper.selectById(dto.getId());
        if (existedUser == null) {
            throw new BusinessException("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(dto.getId());
        user.setNickname(dto.getNickname());
        user.setLocalCity(dto.getLocalCity());
        user.setEducationText(dto.getEducationText());
        user.setExpectedPosition(dto.getExpectedPosition());
        user.setExpectedSalaryMin(dto.getExpectedSalaryMin());
        user.setExpectedSalaryMax(dto.getExpectedSalaryMax());
        user.setSkillInputText(dto.getSkillInputText());

        int rows = sysUserMapper.updateProfile(user);
        if (rows <= 0) {
            throw new BusinessException("更新失败");
        }
    }

    private String md5(String source) {
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
