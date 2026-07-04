package com.yanxitong.favor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yanxitong.favor.dto.FamilyFavorManualEntryRequest;
import com.yanxitong.favor.entity.FavorFamilyBook;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.favor.mapper.FavorFamilyBookMapper;
import com.yanxitong.favor.mapper.FavorFamilyMemberMapper;
import com.yanxitong.operationlog.OperationLogService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FamilyFavorServiceTests {
    @Test
    void manualEntryWritesFamilyBookScope() {
        FavorFamilyBook book = new FavorFamilyBook();
        book.id = 7L;
        book.status = "ACTIVE";
        FavorFamilyBookMapper bookMapper = mock(FavorFamilyBookMapper.class);
        when(bookMapper.selectById(7L)).thenReturn(book);
        FavorContactMapper contactMapper = mock(FavorContactMapper.class);
        when(contactMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        FavorEntryMapper entryMapper = mock(FavorEntryMapper.class);
        FamilyFavorService service = new FamilyFavorService(
                bookMapper,
                mock(FavorFamilyMemberMapper.class),
                contactMapper,
                entryMapper,
                mock(OperationLogService.class)
        );
        FamilyFavorManualEntryRequest request = new FamilyFavorManualEntryRequest();
        request.contactName = "张三";
        request.direction = "RECEIVED";
        request.amount = new BigDecimal("600.00");

        FavorEntry entry = service.manualEntry(7L, request);

        assertEquals("FAMILY", entry.bookScope);
        assertEquals(7L, entry.bookId);
        assertEquals("MANUAL", entry.sourceType);
        assertEquals(new BigDecimal("600.00"), entry.amount);
    }
}
