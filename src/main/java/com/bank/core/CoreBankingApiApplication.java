package com.bank.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CoreBankingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApiApplication.class, args);
    }
}
