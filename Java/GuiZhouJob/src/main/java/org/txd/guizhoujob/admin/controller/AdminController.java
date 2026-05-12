package org.txd.guizhoujob.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            AdminUserQueryDTO dto
    ) {
        return Result.success(adminService.listUsers(adminUserId, dto));
    }

    @PostMapping("/users")
    public Result<Void> createUser(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            @RequestBody AdminUserSaveDTO dto
    ) {
        adminService.createUser(adminUserId, dto);
        return Result.success();
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            @PathVariable Long id,
            @RequestBody AdminUserSaveDTO dto
    ) {
        adminService.updateUser(adminUserId, id, dto);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            @PathVariable Long id
    ) {
        adminService.deleteUser(adminUserId, id);
        return Result.success();
    }

    @GetMapping("/jobs")
    public Result<PageResult<JobInfoVO>> listJobs(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            AdminJobQueryDTO dto
    ) {
        return Result.success(adminService.listJobs(adminUserId, dto));
    }

    @PutMapping("/jobs/{id}")
    public Result<Void> updateJob(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            @PathVariable Long id,
            @RequestBody AdminJobUpdateDTO dto
    ) {
        adminService.updateJob(adminUserId, id, dto);
        return Result.success();
    }

    @DeleteMapping("/jobs/{id}")
    public Result<Void> deleteJob(
            @RequestHeader(value = "X-User-Id", required = false) Long adminUserId,
            @PathVariable Long id
    ) {
        adminService.deleteJob(adminUserId, id);
        return Result.success();
    }
}
