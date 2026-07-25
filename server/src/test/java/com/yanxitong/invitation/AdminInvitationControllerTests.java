package com.yanxitong.invitation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.device.mapper.DeviceOrderMapper;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.invitation.controller.AdminInvitationController;
import com.yanxitong.invitation.dto.AdminInvitationAnalytics;
import com.yanxitong.invitation.entity.Invitation;
import com.yanxitong.invitation.mapper.InvitationMapper;
import com.yanxitong.invitation.mapper.InvitationShareMapper;
import com.yanxitong.invitation.mapper.InvitationVisitLogMapper;
import com.yanxitong.rsvp.mapper.RsvpRecordMapper;
import com.yanxitong.template.mapper.InvitationTemplateMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminInvitationControllerTests {
    @Test
    void analyticsUsesAdminScopedInvitationLookup() {
        InvitationMapper invitationMapper = mock(InvitationMapper.class);
        InvitationVisitLogMapper visitMapper = mock(InvitationVisitLogMapper.class);
        InvitationShareMapper shareMapper = mock(InvitationShareMapper.class);
        RsvpRecordMapper rsvpMapper = mock(RsvpRecordMapper.class);
        GiftRecordMapper giftMapper = mock(GiftRecordMapper.class);
        DeviceOrderMapper deviceMapper = mock(DeviceOrderMapper.class);
        Invitation invitation = new Invitation();
        invitation.id = 13L;
        invitation.banquetId = 8L;
        when(invitationMapper.selectOne(any())).thenReturn(invitation);
        when(visitMapper.selectList(any())).thenReturn(List.of());
        when(shareMapper.selectList(any())).thenReturn(List.of());
        when(rsvpMapper.selectList(any())).thenReturn(List.of());
        when(giftMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        AdminInvitationController controller = new AdminInvitationController(
                invitationMapper,
                mock(BanquetMapper.class),
                mock(InvitationTemplateMapper.class),
                visitMapper,
                shareMapper,
                rsvpMapper,
                giftMapper,
                deviceMapper,
                mock(InvitationService.class)
        );

        AdminInvitationAnalytics analytics = controller.analytics(13L).data();

        assertEquals(13L, analytics.invitationId());
        assertEquals(8L, analytics.banquetId());
        assertEquals(0, analytics.visitCount());
    }
}
