package com.yanxitong.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.config.entity.ConfigItem;
import com.yanxitong.config.mapper.ConfigItemMapper;
import org.junit.jupiter.api.Test;

class CustomerServiceConfigServiceTests {
    @Test
    void returnsConfiguredEnterpriseWechatAcquireLink() {
        ConfigItemMapper mapper = mock(ConfigItemMapper.class);
        ConfigItem item = new ConfigItem();
        item.configValue = " https://work.weixin.qq.com/ca/cawcdeb38645437bb2 ";
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(item);

        var result = new CustomerServiceConfigService(mapper).current();

        assertTrue(result.enabled());
        assertEquals("https://work.weixin.qq.com/ca/cawcdeb38645437bb2", result.acquireLink());
    }

    @Test
    void rejectsLinksOutsideEnterpriseWechatAcquirePath() {
        ConfigItemMapper mapper = mock(ConfigItemMapper.class);
        ConfigItem item = new ConfigItem();
        item.configValue = "https://example.com/ca/not-allowed";
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(item);

        var result = new CustomerServiceConfigService(mapper).current();

        assertFalse(result.enabled());
        assertEquals("", result.acquireLink());
    }

    @Test
    void disablesEntryWhenConfigIsMissing() {
        ConfigItemMapper mapper = mock(ConfigItemMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertFalse(new CustomerServiceConfigService(mapper).current().enabled());
    }
}
