package com.beplepay.weadk.welfare.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.beplepay.weadk.welfare")
public class WeAdkWelfareBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeAdkWelfareBatchApplication.class, args);
    }

}
