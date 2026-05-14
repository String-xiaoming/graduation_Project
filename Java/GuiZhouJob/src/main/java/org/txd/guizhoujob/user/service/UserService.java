package org.txd.guizhoujob.user.service;

import org.txd.guizhoujob.user.dto.UserLoginDTO;
import org.txd.guizhoujob.user.dto.UserEmailLoginDTO;
import org.txd.guizhoujob.user.dto.UserPasswordChangeDTO;
import org.txd.guizhoujob.user.dto.UserPasswordResetDTO;
import org.txd.guizhoujob.user.dto.UserProfileUpdateDTO;
import org.txd.guizhoujob.user.dto.UserRegisterDTO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.vo.UserLoginVO;

public interface UserService {
    void register(UserRegisterDTO dto);

    UserLoginVO login(UserLoginDTO dto);

    UserLoginVO loginByEmailCode(UserEmailLoginDTO dto);

    void resetPassword(UserPasswordResetDTO dto);

    void changePassword(Long userId, UserPasswordChangeDTO dto);

    SysUser getById(Long id);

    void updateProfile(UserProfileUpdateDTO dto);
}
