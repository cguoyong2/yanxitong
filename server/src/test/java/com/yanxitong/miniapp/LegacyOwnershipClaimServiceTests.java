package com.yanxitong.miniapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.entity.BanquetMember;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.banquet.mapper.BanquetMemberMapper;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorFamilyBook;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorFamilyBookMapper;
import com.yanxitong.miniapp.entity.MiniappUser;
import com.yanxitong.miniapp.mapper.MiniappUserMapper;
import com.yanxitong.operationlog.OperationLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LegacyOwnershipClaimServiceTests {
    @Test
    void adminClaimAssignsOnlyPreviouslyUnownedRecordsAndCreatesHostMembership() {
        MiniappUserMapper userMapper = mock(MiniappUserMapper.class);
        BanquetMapper banquetMapper = mock(BanquetMapper.class);
        BanquetMemberMapper memberMapper = mock(BanquetMemberMapper.class);
        FavorContactMapper contactMapper = mock(FavorContactMapper.class);
        FavorFamilyBookMapper familyBookMapper = mock(FavorFamilyBookMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        MiniappUser user = new MiniappUser();
        user.id = 7L;
        user.status = "ACTIVE";
        Banquet banquet = new Banquet();
        banquet.id = 11L;
        banquet.ownerUserId = 7L;
        when(userMapper.selectById(7L)).thenReturn(user);
        when(banquetMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(banquetMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(banquet));
        when(memberMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(contactMapper.update(isNull(), any(Wrapper.class))).thenReturn(2);
        when(familyBookMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        LegacyOwnershipClaimService service = new LegacyOwnershipClaimService(
                userMapper,
                banquetMapper,
                memberMapper,
                contactMapper,
                familyBookMapper,
                operationLogService
        );

        LegacyOwnershipClaimResult result = service.claimForUser(7L);

        assertEquals(1, result.banquetCount());
        assertEquals(1, result.banquetMemberCount());
        assertEquals(2, result.favorContactCount());
        assertEquals(1, result.familyBookCount());
        ArgumentCaptor<BanquetMember> member = ArgumentCaptor.forClass(BanquetMember.class);
        verify(memberMapper).insert(member.capture());
        assertEquals(7L, member.getValue().userId);
        assertEquals("HOST", member.getValue().roleCode);
    }
}
