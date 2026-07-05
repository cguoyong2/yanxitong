package com.yanxitong.favor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.favor.dto.FavorManualEntryRequest;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.operationlog.OperationLogService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FavorServiceTests {
    @Test
    void contactsFilterAmountsByBanquetId() {
        FavorContact contact = new FavorContact();
        contact.id = 1L;
        contact.contactName = "张三";
        FavorContactMapper contactMapper = mock(FavorContactMapper.class);
        FavorEntryMapper entryMapper = mock(FavorEntryMapper.class);
        when(contactMapper.selectList(any(Wrapper.class))).thenReturn(List.of(contact));
        when(entryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                entry(100L, "RECEIVED", "300.00"),
                entry(200L, "RECEIVED", "500.00"),
                entry(100L, "GIVEN", "120.00")
        ));
        FavorService service = new FavorService(contactMapper, entryMapper, mock(BanquetMapper.class), mock(OperationLogService.class));

        List<FavorContactSummary> summaries = service.contacts(null, 100L);

        assertEquals(1, summaries.size());
        FavorContactSummary summary = summaries.get(0);
        assertEquals(new BigDecimal("300.00"), summary.receivedAmount());
        assertEquals(new BigDecimal("120.00"), summary.givenAmount());
        assertEquals(new BigDecimal("180.00"), summary.balance());
    }

    @Test
    void contactsWithBanquetFilterHideContactsWithoutMatchingEntries() {
        FavorContact contact = new FavorContact();
        contact.id = 1L;
        contact.contactName = "张三";
        FavorContactMapper contactMapper = mock(FavorContactMapper.class);
        FavorEntryMapper entryMapper = mock(FavorEntryMapper.class);
        when(contactMapper.selectList(any(Wrapper.class))).thenReturn(List.of(contact));
        when(entryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(entry(200L, "RECEIVED", "500.00")));
        FavorService service = new FavorService(contactMapper, entryMapper, mock(BanquetMapper.class), mock(OperationLogService.class));

        List<FavorContactSummary> summaries = service.contacts(null, 100L);

        assertEquals(0, summaries.size());
    }

    @Test
    void manualEntryNormalizesContactAndReusesExistingContact() {
        FavorContact existing = new FavorContact();
        existing.id = 8L;
        existing.contactName = "张三";
        FavorContactMapper contactMapper = mock(FavorContactMapper.class);
        FavorEntryMapper entryMapper = mock(FavorEntryMapper.class);
        when(contactMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        FavorService service = new FavorService(contactMapper, entryMapper, mock(BanquetMapper.class), mock(OperationLogService.class));

        FavorEntry result = service.manualEntry(manualRequest(" 张三 ", " 13800000001 ", " GIVEN ", " 回礼备注 "));

        ArgumentCaptor<FavorEntry> captor = ArgumentCaptor.forClass(FavorEntry.class);
        verify(entryMapper).insert(captor.capture());
        FavorEntry inserted = captor.getValue();
        assertEquals(8L, inserted.contactId);
        assertEquals("GIVEN", inserted.direction);
        assertEquals("回礼备注", inserted.note);
        assertEquals(inserted, result);
    }

    private FavorEntry entry(Long banquetId, String direction, String amount) {
        FavorEntry entry = new FavorEntry();
        entry.contactId = 1L;
        entry.banquetId = banquetId;
        entry.direction = direction;
        entry.amount = new BigDecimal(amount);
        return entry;
    }

    private FavorManualEntryRequest manualRequest(String contactName, String phone, String direction, String note) {
        FavorManualEntryRequest request = new FavorManualEntryRequest();
        request.contactName = contactName;
        request.phone = phone;
        request.direction = direction;
        request.amount = new BigDecimal("100.00");
        request.note = note;
        return request;
    }
}
