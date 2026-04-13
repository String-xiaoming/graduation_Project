package org.txd.guizhoujob.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.user.entity.SysUser;

@Mapper
public interface SysUserMapper {
    SysUser selectByEmail(@Param("email") String email);

    SysUser selectById(@Param("id") Long id);

    int insert(SysUser user);

    int updateProfile(SysUser user);
}
