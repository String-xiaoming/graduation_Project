package org.txd.guizhoujob.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.txd.guizhoujob.auth.AuthContext;
import org.txd.guizhoujob.admin.dto.AdminJobQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminJobUpdateDTO;
import org.txd.guizhoujob.admin.dto.AdminUserQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminUserSaveDTO;
import org.txd.guizhoujob.admin.service.AdminService;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.common.Result;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.user.entity.SysUser;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Result<PageResult<SysUser>> listUsers(
            AdminUserQueryDTO dto
    ) {
        return Result.success(adminService.listUsers(AuthContext.requireUserId(), dto));
    }

    @PostMapping("/users")
    public Result<Void> createUser(
            @RequestBody AdminUserSaveDTO dto
    ) {
        adminService.createUser(AuthContext.requireUserId(), dto);
        return Result.success();
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserSaveDTO dto
    ) {
        adminService.updateUser(AuthContext.requireUserId(), id, dto);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(
            @PathVariable Long id
    ) {
        adminService.deleteUser(AuthContext.requireUserId(), id);
        return Result.success();
    }

    @GetMapping("/jobs")
    public Result<PageResult<JobInfoVO>> listJobs(
            AdminJobQueryDTO dto
    ) {
        return Result.success(adminService.listJobs(AuthContext.requireUserId(), dto));
    }

    @PutMapping("/jobs/{id}")
    public Result<Void> updateJob(
            @PathVariable Long id,
            @RequestBody AdminJobUpdateDTO dto
    ) {
        adminService.updateJob(AuthContext.requireUserId(), id, dto);
        return Result.success();
    }

    @DeleteMapping("/jobs/{id}")
    public Result<Void> deleteJob(
            @PathVariable Long id
    ) {
        adminService.deleteJob(AuthContext.requireUserId(), id);
        return Result.success();
    }
}
