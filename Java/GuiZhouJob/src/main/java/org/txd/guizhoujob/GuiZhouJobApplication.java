package org.txd.guizhoujob;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "org.txd.guizhoujob.user.mapper",
        "org.txd.guizhoujob.job.mapper",
        "org.txd.guizhoujob.analysis.mapper",
        "org.txd.guizhoujob.recommend.mapper",
        "org.txd.guizhoujob.email.mapper"
})
public class GuiZhouJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuiZhouJobApplication.class, args);
    }
}
