package org.txd.guizhoujob.admin.service;

import org.txd.guizhoujob.admin.dto.AdminJobQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminJobUpdateDTO;
import org.txd.guizhoujob.admin.dto.AdminUserQueryDTO;
import org.txd.guizhoujob.admin.dto.AdminUserSaveDTO;
import org.txd.guizhoujob.common.PageResult;
import org.txd.guizhoujob.job.vo.JobInfoVO;
import org.txd.guizhoujob.user.entity.SysUser;

public interface AdminService {
    PageResult<SysUser> listUsers(Long adminUserId, AdminUserQueryDTO dto);

    void createUser(Long adminUserId, AdminUserSaveDTO dto);

    void updateUser(Long adminUserId, Long id, AdminUserSaveDTO dto);

    void deleteUser(Long adminUserId, Long id);

    PageResult<JobInfoVO> listJobs(Long adminUserId, AdminJobQueryDTO dto);

    void updateJob(Long adminUserId, Long id, AdminJobUpdateDTO dto);

    void deleteJob(Long adminUserId, Long id);
}
