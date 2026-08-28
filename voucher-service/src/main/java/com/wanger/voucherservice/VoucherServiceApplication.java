package com.wanger.voucherservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.wanger.voucherservice", "com.wanger.common"})
@MapperScan("com.wanger.voucherservice.mapper")
public class VoucherServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoucherServiceApplication.class, args);
    }
}
