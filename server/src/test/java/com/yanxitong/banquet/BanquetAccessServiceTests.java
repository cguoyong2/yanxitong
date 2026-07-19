package com.yanxitong.banquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.banquet.mapper.BanquetMemberMapper;
import com.yanxitong.miniapp.MiniappPrincipal;
import com.yanxitong.miniapp.MiniappPrincipalContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class BanquetAccessServiceTests {
    @AfterEach
    void tearDown() {
        MiniappPrincipalContext.clear();
    }

    @Test
    void ownerCanAccessOwnBanquet() {
        BanquetMapper banquetMapper = mock(BanquetMapper.class);
        BanquetMemberMapper memberMapper = mock(BanquetMemberMapper.class);
        Banquet banquet = banquet(10L, 7L, "DRAFT");
        when(banquetMapper.selectById(10L)).thenReturn(banquet);
        MiniappPrincipalContext.set(new MiniappPrincipal(7L, null, "openid-7", "USER"));

        assertEquals(banquet, new BanquetAccessService(banquetMapper, memberMapper).requireAccessible(10L));
    }

    @Test
    void otherUserCannotAccessBanquet() {
        BanquetMapper banquetMapper = mock(BanquetMapper.class);
        BanquetMemberMapper memberMapper = mock(BanquetMemberMapper.class);
        when(banquetMapper.selectById(10L)).thenReturn(banquet(10L, 7L, "DRAFT"));
        when(memberMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        MiniappPrincipalContext.set(new MiniappPrincipal(8L, null, "openid-8", "USER"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> new BanquetAccessService(banquetMapper, memberMapper).requireAccessible(10L)
        );
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void publicFlowOnlyAcceptsPublishedBanquet() {
        BanquetMapper banquetMapper = mock(BanquetMapper.class);
        BanquetMemberMapper memberMapper = mock(BanquetMemberMapper.class);
        when(banquetMapper.selectById(10L)).thenReturn(banquet(10L, 7L, "DRAFT"));
        BanquetAccessService service = new BanquetAccessService(banquetMapper, memberMapper);

        assertThrows(ResponseStatusException.class, () -> service.requirePublished(10L));

        Banquet published = banquet(10L, 7L, "PUBLISHED");
        when(banquetMapper.selectById(10L)).thenReturn(published);
        assertEquals(published, service.requirePublished(10L));
    }

    private Banquet banquet(Long id, Long ownerUserId, String status) {
        Banquet banquet = new Banquet();
        banquet.id = id;
        banquet.ownerUserId = ownerUserId;
        banquet.status = status;
        return banquet;
    }
}
