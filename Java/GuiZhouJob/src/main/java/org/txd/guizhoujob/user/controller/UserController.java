package org.txd.guizhoujob.user.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.txd.guizhoujob.auth.AuthContext;
import org.txd.guizhoujob.common.Result;
import org.txd.guizhoujob.user.dto.UserEmailLoginDTO;
import org.txd.guizhoujob.user.dto.UserLoginDTO;
import org.txd.guizhoujob.user.dto.UserPasswordChangeDTO;
import org.txd.guizhoujob.user.dto.UserPasswordResetDTO;
import org.txd.guizhoujob.user.dto.UserProfileUpdateDTO;
import org.txd.guizhoujob.user.dto.UserRegisterDTO;
import org.txd.guizhoujob.user.entity.SysUser;
import org.txd.guizhoujob.user.service.UserService;
import org.txd.guizhoujob.user.vo.UserLoginVO;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/login/code")
    public Result<UserLoginVO> loginByEmailCode(@RequestBody @Valid UserEmailLoginDTO dto) {
        return Result.success(userService.loginByEmailCode(dto));
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid UserPasswordResetDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
    }

    @PostMapping("/password/change")
    public Result<Void> changePassword(
            @RequestBody @Valid UserPasswordChangeDTO dto
    ) {
        userService.changePassword(AuthContext.requireUserId(), dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getById(AuthContext.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody @Valid UserProfileUpdateDTO dto) {
        dto.setId(AuthContext.requireUserId());
        userService.updateProfile(dto);
        return Result.success();
    }
}
