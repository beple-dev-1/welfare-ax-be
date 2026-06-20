package com.beplepay.welfareaxbe.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.beplepay.welfareaxbe")
@EnableJpaRepositories("com.beplepay.welfareaxbe")
public class WelfareAxUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(WelfareAxUserApplication.class, args);
    }

}
