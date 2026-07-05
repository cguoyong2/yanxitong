package com.yanxitong.rsvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.rsvp.dto.RsvpSubmitRequest;
import com.yanxitong.rsvp.entity.RsvpRecord;
import com.yanxitong.rsvp.mapper.RsvpRecordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RsvpServiceTests {
    @Test
    void submitNormalizesGuestDataAndLegacyAttendStatus() {
        RsvpRecordMapper mapper = mock(RsvpRecordMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
        RsvpService service = new RsvpService(mapper, mock(OperationLogService.class));

        RsvpRecord result = service.submit(request(" 张三 ", " 13800000001 ", " ATTEND ", 2, " 准时到 "));

        ArgumentCaptor<RsvpRecord> captor = ArgumentCaptor.forClass(RsvpRecord.class);
        verify(mapper).insert(captor.capture());
        RsvpRecord inserted = captor.getValue();
        assertEquals("张三", inserted.guestName);
        assertEquals("13800000001", inserted.phone);
        assertEquals("ATTENDING", inserted.attendanceStatus);
        assertEquals("准时到", inserted.message);
        assertTrue(result.created);
    }

    @Test
    void repeatSubmitUpdatesExistingRecordAfterNormalization() {
        RsvpRecord existing = new RsvpRecord();
        existing.id = 9L;
        existing.banquetId = 100L;
        existing.guestName = "张三";
        existing.phone = "13800000001";
        existing.guestCount = 2;
        RsvpRecordMapper mapper = mock(RsvpRecordMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        RsvpService service = new RsvpService(mapper, mock(OperationLogService.class));

        RsvpRecord result = service.submit(request(" 张三 ", " 13800000001 ", "ATTENDING", 3, " 改为三人 "));

        verify(mapper, never()).insert(any(RsvpRecord.class));
        verify(mapper).updateById(existing);
        assertEquals(9L, result.id);
        assertEquals("张三", result.guestName);
        assertEquals("13800000001", result.phone);
        assertEquals(3, result.guestCount);
        assertEquals("改为三人", result.message);
        assertFalse(result.created);
    }

    @Test
    void blankPhoneIsStoredAsNullSoNameFallbackCanDeduplicate() {
        RsvpRecordMapper mapper = mock(RsvpRecordMapper.class);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
        RsvpService service = new RsvpService(mapper, mock(OperationLogService.class));

        service.submit(request(" 李四 ", "   ", "PENDING", 1, " "));

        ArgumentCaptor<RsvpRecord> captor = ArgumentCaptor.forClass(RsvpRecord.class);
        verify(mapper).insert(captor.capture());
        RsvpRecord inserted = captor.getValue();
        assertEquals("李四", inserted.guestName);
        assertNull(inserted.phone);
        assertNull(inserted.message);
    }

    private RsvpSubmitRequest request(String guestName, String phone, String status, int guestCount, String message) {
        RsvpSubmitRequest request = new RsvpSubmitRequest();
        request.banquetId = 100L;
        request.invitationId = 200L;
        request.guestName = guestName;
        request.phone = phone;
        request.attendanceStatus = status;
        request.mealRequired = 1;
        request.accommodationRequired = 0;
        request.guestCount = guestCount;
        request.message = message;
        return request;
    }
}
