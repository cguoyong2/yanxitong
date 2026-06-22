package com.yanxitong.favor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.operationlog.OperationLogService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

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
        FavorService service = new FavorService(contactMapper, entryMapper, mock(OperationLogService.class));

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
        FavorService service = new FavorService(contactMapper, entryMapper, mock(OperationLogService.class));

        List<FavorContactSummary> summaries = service.contacts(null, 100L);

        assertEquals(0, summaries.size());
    }

    private FavorEntry entry(Long banquetId, String direction, String amount) {
        FavorEntry entry = new FavorEntry();
        entry.contactId = 1L;
        entry.banquetId = banquetId;
        entry.direction = direction;
        entry.amount = new BigDecimal(amount);
        return entry;
    }
}
