package com.yanxitong.invitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.invitation.mapper.InvitationShareMapper;
import com.yanxitong.invitation.mapper.InvitationVisitLogMapper;
import com.yanxitong.operationlog.OperationLogService;
import org.junit.jupiter.api.Test;

class InvitationLifecycleServiceTests {
    @Test
    void baseInvitationStartsAsDraftAndPublishActivationIsExplicit() {
        InvitationMapper invitationMapper = mock(InvitationMapper.class);
        InvitationService service = service(invitationMapper);
        Banquet banquet = new Banquet();
        banquet.id = 3L;
        banquet.name = "测试宴席";

        Invitation created = service.createBaseInvitation(banquet, null);

        assertEquals("DRAFT", created.status);

        when(invitationMapper.selectOne(any(Wrapper.class))).thenReturn(created);
        service.activateByBanquetId(3L);
        assertEquals("ACTIVE", created.status);
        verify(invitationMapper).updateById(created);
    }

    private InvitationService service(InvitationMapper invitationMapper) {
        return new InvitationService(
                invitationMapper,
                mock(InvitationShareMapper.class),
                mock(InvitationVisitLogMapper.class),
                new ObjectMapper(),
                mock(OperationLogService.class),
                mock(BanquetAccessService.class)
        );
    }
}
