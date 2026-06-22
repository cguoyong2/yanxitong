package com.yanxitong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yanxitong.**.mapper")
@SpringBootApplication
public class YanxitongApplication {
    public static void main(String[] args) {
        SpringApplication.run(YanxitongApplication.class, args);
    }
}
