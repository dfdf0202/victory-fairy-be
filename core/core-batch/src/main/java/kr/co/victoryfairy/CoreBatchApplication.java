package kr.co.victoryfairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoreBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBatchApplication.class, args);
    }

}
