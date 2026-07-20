package com.yanxitong.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.config.dto.CustomerServiceConfig;
import com.yanxitong.config.entity.ConfigItem;
import com.yanxitong.config.mapper.ConfigItemMapper;
import java.net.URI;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceConfigService {
    public static final String ACQUIRE_LINK_KEY = "customer.service.acquire_link";

    private final ConfigItemMapper configItemMapper;

    public CustomerServiceConfigService(ConfigItemMapper configItemMapper) {
        this.configItemMapper = configItemMapper;
    }

    public CustomerServiceConfig current() {
        ConfigItem item = configItemMapper.selectOne(new QueryWrapper<ConfigItem>()
                .isNull("tenant_id")
                .eq("config_key", ACQUIRE_LINK_KEY)
                .eq("enabled", 1)
                .orderByDesc("updated_at")
                .last("LIMIT 1"));
        if (item == null || !isAllowedAcquireLink(item.configValue)) {
            return CustomerServiceConfig.disabled();
        }
        return new CustomerServiceConfig(item.configValue.trim(), true);
    }

    private boolean isAllowedAcquireLink(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "work.weixin.qq.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/ca/");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
