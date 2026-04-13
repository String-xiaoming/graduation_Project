package org.txd.guizhoujob;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "org.txd.guizhoujob.user.mapper",
        "org.txd.guizhoujob.job.mapper"
})
public class GuiZhouJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuiZhouJobApplication.class, args);
    }
}
