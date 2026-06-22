package com.yanxitong.order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class OrderNoGenerator {
    public String next(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}

