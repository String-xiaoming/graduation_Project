package org.txd.guizhoujob.user.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.txd.guizhoujob.auth.JwtService;
import org.txd.guizhoujob.common.BusinessException;
import org.txd.guizhoujob.email.service.EmailCodeService;
import org.txd.guizhoujob.user.dto.UserEmailLoginDTO;
import org.txd.guizhoujob.user.dto.UserLoginDTO;
import org.txd.guizhoujob.user.dto.UserPasswordChangeDTO;
import org.txd.guizhoujob.user.dto.UserPasswordResetDTO;
import org.txd.guizhoujob.user.dto.UserProfileUpdateDTO;
import org.txd.guizhoujob.user.dto.UserRegisterDTO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.mapper.SysUserMapper;
import org.txd.guizhoujob.user.service.UserService;
import org.txd.guizhoujob.user.support.PasswordHashSupport;
import org.txd.guizhoujob.user.vo.UserLoginVO;

import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final EmailCodeService emailCodeService;
    private final JwtService jwtService;

    public UserServiceImpl(SysUserMapper sysUserMapper, EmailCodeService emailCodeService, JwtService jwtService) {
        this.sysUserMapper = sysUserMapper;
        this.emailCodeService = emailCodeService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        SysUser existedUser = sysUserMapper.selectByEmail(email);
        if (existedUser != null) {
            throw new BusinessException("邮箱已注册");
        }

        emailCodeService.verifyAndConsume(email, "REGISTER", dto.getEmailCode());

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setNickname(dto.getNickname());
        user.setPasswordHash(PasswordHashSupport.encode(dto.getPassword()));
        user.setStatus(1);

        int rows = sysUserMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("注册失败");
        }
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        SysUser user = requireUserByEmail(dto.getEmail());
        checkActive(user);

        if (!PasswordHashSupport.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }
        upgradePasswordIfNeeded(user, dto.getPassword());
        return toLoginVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO loginByEmailCode(UserEmailLoginDTO dto) {
        SysUser user = requireUserByEmail(dto.getEmail());
        checkActive(user);
        emailCodeService.verifyAndConsume(user.getEmail(), "EMAIL_LOGIN", dto.getEmailCode());
        return toLoginVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(UserPasswordResetDTO dto) {
        SysUser user = requireUserByEmail(dto.getEmail());
        checkActive(user);
        emailCodeService.verifyAndConsume(user.getEmail(), "RESET_PASSWORD", dto.getEmailCode());
        updatePassword(user.getId(), dto.getNewPassword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, UserPasswordChangeDTO dto) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkActive(user);
        if (!PasswordHashSupport.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("旧密码错误");
        }
        emailCodeService.verifyAndConsume(user.getEmail(), "CHANGE_PASSWORD", dto.getEmailCode());
        updatePassword(user.getId(), dto.getNewPassword());
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

    private SysUser requireUserByEmail(String email) {
        SysUser user = sysUserMapper.selectByEmail(normalizeEmail(email));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void checkActive(SysUser user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
    }

    private void updatePassword(Long userId, String rawPassword) {
        int rows = sysUserMapper.updatePasswordHash(userId, PasswordHashSupport.encode(rawPassword));
        if (rows <= 0) {
            throw new BusinessException("密码更新失败");
        }
    }

    private void upgradePasswordIfNeeded(SysUser user, String rawPassword) {
        if (PasswordHashSupport.needsUpgrade(user.getPasswordHash())) {
            sysUserMapper.updatePasswordHash(user.getId(), PasswordHashSupport.encode(rawPassword));
        }
    }

    private UserLoginVO toLoginVO(SysUser user) {
        JwtService.TokenPair tokenPair = jwtService.issue(user);
        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setToken(tokenPair.token());
        vo.setExpiresAt(tokenPair.expiresAt());
        return vo;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
