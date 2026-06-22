package com.beplepay.weadk.welfare.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.beplepay.weadk.welfare")
@EnableJpaRepositories("com.beplepay.weadk.welfare")
public class WeAdkWelfareUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeAdkWelfareUserApplication.class, args);
    }

}
