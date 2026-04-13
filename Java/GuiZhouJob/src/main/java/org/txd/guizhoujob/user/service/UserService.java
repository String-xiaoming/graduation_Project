package org.txd.guizhoujob.user.service;

import org.txd.guizhoujob.user.dto.UserLoginDTO;
import org.txd.guizhoujob.user.dto.UserProfileUpdateDTO;
import org.txd.guizhoujob.user.dto.UserRegisterDTO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.vo.UserLoginVO;

public interface UserService {
    void register(UserRegisterDTO dto);

    UserLoginVO login(UserLoginDTO dto);

    SysUser getById(Long id);

    void updateProfile(UserProfileUpdateDTO dto);
}
