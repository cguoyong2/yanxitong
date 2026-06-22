package com.yanxitong.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.gift.mapper.GiftRecordMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.order.PlanOrderService;
import com.yanxitong.order.dto.RightsCheckResult;
import com.yanxitong.rsvp.entity.RsvpRecord;
import com.yanxitong.rsvp.mapper.RsvpRecordMapper;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class ExportServiceTests {
    private static final Long BANQUET_ID = 12L;

    private final GiftRecordMapper giftRecordMapper = mock(GiftRecordMapper.class);
    private final RsvpRecordMapper rsvpRecordMapper = mock(RsvpRecordMapper.class);
    private final FavorEntryMapper favorEntryMapper = mock(FavorEntryMapper.class);
    private final FavorContactMapper favorContactMapper = mock(FavorContactMapper.class);
    private final PlanOrderService planOrderService = mock(PlanOrderService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final ExportService service = new ExportService(
            giftRecordMapper,
            rsvpRecordMapper,
            favorEntryMapper,
            favorContactMapper,
            planOrderService,
            operationLogService
    );

    @Test
    void rejectsAllExportFormatsWhenBanquetDoesNotHaveExportRight() {
        when(planOrderService.checkBanquetRight(BANQUET_ID, "EXCEL_EXPORT"))
                .thenReturn(new RightsCheckResult(false, "EXCEL_EXPORT", null));

        assertForbidden(() -> service.exportGifts(BANQUET_ID));
        assertForbidden(() -> service.exportGiftsXlsx(BANQUET_ID));
        assertForbidden(() -> service.exportRsvp(BANQUET_ID));
        assertForbidden(() -> service.exportRsvpXlsx(BANQUET_ID));
        assertForbidden(() -> service.exportFavor(BANQUET_ID));
        assertForbidden(() -> service.exportFavorXlsx(BANQUET_ID));

        verify(operationLogService, never()).record(eq(OperationModule.EXPORT), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsGiftExportWhenRowCapWouldBeExceeded() {
        allowExport();
        when(giftRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(giftRows(10001));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.exportGifts(BANQUET_ID));

        assertEquals(413, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("超过导出上限10000行"));
        verify(operationLogService, never()).record(eq(OperationModule.EXPORT), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsRsvpExportWhenRowCapWouldBeExceeded() {
        allowExport();
        when(rsvpRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(rsvpRows(10001));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.exportRsvpXlsx(BANQUET_ID));

        assertEquals(413, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("超过导出上限10000行"));
        verify(operationLogService, never()).record(eq(OperationModule.EXPORT), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsFavorExportWhenRowCapWouldBeExceeded() {
        allowExport();
        when(favorEntryMapper.selectList(any(QueryWrapper.class))).thenReturn(favorRows(10001));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.exportFavor(BANQUET_ID));

        assertEquals(413, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("超过导出上限10000行"));
        verify(operationLogService, never()).record(eq(OperationModule.EXPORT), any(), any(), any(), any(), any());
    }

    @Test
    void successfulCsvExportWritesOperationLogWithFormatTypeAndRowCount() {
        allowExport();
        when(giftRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(giftRows(2));

        ExportFile file = service.exportGifts(BANQUET_ID);

        assertEquals("banquet-12-gifts.csv", file.filename());
        assertTrue(new String(file.content()).contains("来宾姓名"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(operationLogService).record(
                eq(OperationModule.EXPORT),
                eq("EXPORT_GIFTS_CSV"),
                eq("banquet"),
                eq(BANQUET_ID),
                eq("export gift records csv"),
                detailCaptor.capture()
        );
        Map<String, Object> detail = detailCaptor.getValue();
        assertEquals("csv", detail.get("format"));
        assertEquals("gifts", detail.get("type"));
        assertEquals(2, detail.get("rowCount"));
        assertEquals(10000, detail.get("maxRows"));
    }

    @Test
    void successfulXlsxExportContainsChineseHeadersAndRows() throws Exception {
        allowExport();
        when(rsvpRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(rsvpRows(1));

        ExportFile file = service.exportRsvpXlsx(BANQUET_ID);

        assertEquals("banquet-12-rsvp.xlsx", file.filename());
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.content()))) {
            var sheet = workbook.getSheet("回执 RSVP");
            assertEquals("姓名", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals("回执来宾0", sheet.getRow(1).getCell(2).getStringCellValue());
        }
        verify(operationLogService).record(eq(OperationModule.EXPORT), eq("EXPORT_RSVP_XLSX"), eq("banquet"), eq(BANQUET_ID), any(), any());
    }

    private void assertForbidden(ExportCall call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("当前宴席版本不支持导出", ex.getReason());
    }

    private void allowExport() {
        when(planOrderService.checkBanquetRight(BANQUET_ID, "EXCEL_EXPORT"))
                .thenReturn(new RightsCheckResult(true, "EXCEL_EXPORT", "1"));
    }

    private List<GiftRecord> giftRows(int count) {
        List<GiftRecord> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GiftRecord record = new GiftRecord();
            record.id = (long) i + 1;
            record.banquetId = BANQUET_ID;
            record.giftSource = "ONLINE_GIFT";
            record.guestName = "礼金来宾" + i;
            record.amount = BigDecimal.valueOf(100 + i);
            record.blessing = "祝福" + i;
            record.receivedAt = LocalDateTime.of(2026, 6, 22, 12, 0).plusMinutes(i);
            rows.add(record);
        }
        return rows;
    }

    private List<RsvpRecord> rsvpRows(int count) {
        List<RsvpRecord> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RsvpRecord record = new RsvpRecord();
            record.id = (long) i + 1;
            record.banquetId = BANQUET_ID;
            record.guestName = "回执来宾" + i;
            record.phone = "13800000000";
            record.attendanceStatus = "ATTEND";
            record.mealRequired = 1;
            record.accommodationRequired = 0;
            record.guestCount = 2;
            record.message = "留言" + i;
            record.createdAt = LocalDateTime.of(2026, 6, 22, 13, 0).plusMinutes(i);
            rows.add(record);
        }
        return rows;
    }

    private List<FavorEntry> favorRows(int count) {
        List<FavorEntry> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FavorEntry record = new FavorEntry();
            record.id = (long) i + 1;
            record.banquetId = BANQUET_ID;
            record.contactId = (long) i + 100;
            record.direction = "RECEIVED";
            record.sourceType = "GIFT";
            record.amount = BigDecimal.valueOf(200 + i);
            record.occurredAt = LocalDateTime.of(2026, 6, 22, 14, 0).plusMinutes(i);
            record.note = "备注" + i;
            rows.add(record);
        }
        return rows;
    }

    @FunctionalInterface
    private interface ExportCall {
        void run();
    }
}
