package org.txd.guizhoujob.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.admin.dto.AdminUserQueryDTO;
import org.txd.guizhoujob.user.entity.SysUser;

import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser selectByEmail(@Param("email") String email);

    SysUser selectById(@Param("id") Long id);

    int insert(SysUser user);

    int updateProfile(SysUser user);

    Long countByAdminCondition(AdminUserQueryDTO dto);

    List<SysUser> selectByAdminCondition(AdminUserQueryDTO dto);

    int insertByAdmin(SysUser user);

    int updateByAdmin(SysUser user);

    int disableById(@Param("id") Long id);
}
