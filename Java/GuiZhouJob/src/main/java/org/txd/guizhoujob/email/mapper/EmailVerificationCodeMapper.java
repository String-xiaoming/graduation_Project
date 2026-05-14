package org.txd.guizhoujob.email.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.txd.guizhoujob.email.entity.EmailVerificationCode;

import java.time.LocalDateTime;

@Mapper
public interface EmailVerificationCodeMapper {
    int insert(EmailVerificationCode code);

    LocalDateTime selectLastCreateTime(@Param("email") String email, @Param("scene") String scene);

    EmailVerificationCode selectLatestAvailable(
            @Param("email") String email,
            @Param("scene") String scene,
            @Param("now") LocalDateTime now
    );

    int markUsed(@Param("id") Long id);
}
