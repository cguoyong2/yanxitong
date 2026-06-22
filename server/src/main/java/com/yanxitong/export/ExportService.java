package com.yanxitong.export;

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
import com.yanxitong.tenant.TenantContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExportService {
    private static final String RIGHT_CODE = "EXCEL_EXPORT";
    private static final int MAX_ROWS = 10000;
    private static final int FETCH_LIMIT = MAX_ROWS + 1;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GiftRecordMapper giftRecordMapper;
    private final RsvpRecordMapper rsvpRecordMapper;
    private final FavorEntryMapper favorEntryMapper;
    private final FavorContactMapper favorContactMapper;
    private final PlanOrderService planOrderService;
    private final OperationLogService operationLogService;

    public ExportService(
            GiftRecordMapper giftRecordMapper,
            RsvpRecordMapper rsvpRecordMapper,
            FavorEntryMapper favorEntryMapper,
            FavorContactMapper favorContactMapper,
            PlanOrderService planOrderService,
            OperationLogService operationLogService
    ) {
        this.giftRecordMapper = giftRecordMapper;
        this.rsvpRecordMapper = rsvpRecordMapper;
        this.favorEntryMapper = favorEntryMapper;
        this.favorContactMapper = favorContactMapper;
        this.planOrderService = planOrderService;
        this.operationLogService = operationLogService;
    }

    public ExportFile exportGifts(Long banquetId) {
        ExportTable table = giftTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_GIFTS_CSV", "banquet", banquetId, "export gift records csv", exportDetail("csv", table));
        return csvFile(banquetId, table);
    }

    public ExportFile exportGiftsXlsx(Long banquetId) {
        ExportTable table = giftTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_GIFTS_XLSX", "banquet", banquetId, "export gift records xlsx", exportDetail("xlsx", table));
        return xlsxFile(banquetId, table);
    }

    public ExportFile exportRsvp(Long banquetId) {
        ExportTable table = rsvpTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_RSVP_CSV", "banquet", banquetId, "export rsvp records csv", exportDetail("csv", table));
        return csvFile(banquetId, table);
    }

    public ExportFile exportRsvpXlsx(Long banquetId) {
        ExportTable table = rsvpTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_RSVP_XLSX", "banquet", banquetId, "export rsvp records xlsx", exportDetail("xlsx", table));
        return xlsxFile(banquetId, table);
    }

    public ExportFile exportFavor(Long banquetId) {
        ExportTable table = favorTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_FAVOR_CSV", "banquet", banquetId, "export favor entries csv", exportDetail("csv", table));
        return csvFile(banquetId, table);
    }

    public ExportFile exportFavorXlsx(Long banquetId) {
        ExportTable table = favorTable(banquetId);
        operationLogService.record(OperationModule.EXPORT, "EXPORT_FAVOR_XLSX", "banquet", banquetId, "export favor entries xlsx", exportDetail("xlsx", table));
        return xlsxFile(banquetId, table);
    }

    private ExportTable giftTable(Long banquetId) {
        requireExportRight(banquetId);
        List<GiftRecord> records = giftRecordMapper.selectList(tenantScoped(new QueryWrapper<GiftRecord>())
                .eq("banquet_id", banquetId)
                .orderByDesc("received_at")
                .last("LIMIT " + FETCH_LIMIT));
        ensureWithinRowLimit("礼金记录", records.size());
        List<List<Object>> rows = new ArrayList<>();
        for (GiftRecord record : records) {
            rows.add(List.of(
                    record.id,
                    record.banquetId,
                    record.giftSource,
                    record.guestName,
                    value(record.amount),
                    value(record.blessing),
                    value(record.receivedAt)
            ));
        }
        return new ExportTable("gifts", "礼金记录", List.of("礼金ID", "宴席ID", "来源", "来宾姓名", "金额", "祝福/备注", "收礼时间"), rows);
    }

    private ExportTable rsvpTable(Long banquetId) {
        requireExportRight(banquetId);
        List<RsvpRecord> records = rsvpRecordMapper.selectList(tenantScoped(new QueryWrapper<RsvpRecord>())
                .eq("banquet_id", banquetId)
                .orderByDesc("created_at")
                .last("LIMIT " + FETCH_LIMIT));
        ensureWithinRowLimit("回执记录", records.size());
        List<List<Object>> rows = new ArrayList<>();
        for (RsvpRecord record : records) {
            rows.add(List.of(
                    record.id,
                    record.banquetId,
                    record.guestName,
                    value(record.phone),
                    record.attendanceStatus,
                    yesNo(record.mealRequired),
                    yesNo(record.accommodationRequired),
                    record.guestCount,
                    value(record.message),
                    value(record.createdAt)
            ));
        }
        return new ExportTable("rsvp", "回执 RSVP", List.of("回执ID", "宴席ID", "姓名", "手机", "出席状态", "用餐", "住宿", "人数", "留言", "提交时间"), rows);
    }

    private ExportTable favorTable(Long banquetId) {
        requireExportRight(banquetId);
        List<FavorEntry> records = favorEntryMapper.selectList(tenantScoped(new QueryWrapper<FavorEntry>())
                .eq("banquet_id", banquetId)
                .orderByDesc("occurred_at")
                .last("LIMIT " + FETCH_LIMIT));
        ensureWithinRowLimit("人情账本", records.size());
        List<Long> contactIds = records.stream()
                        .map(record -> record.contactId)
                        .filter(id -> id != null)
                        .distinct()
                        .toList();
        Map<Long, FavorContact> contacts = contactIds.isEmpty()
                ? Map.of()
                : favorContactMapper.selectBatchIds(contactIds)
                        .stream()
                        .collect(Collectors.toMap(contact -> contact.id, Function.identity()));

        List<List<Object>> rows = new ArrayList<>();
        for (FavorEntry record : records) {
            FavorContact contact = contacts.get(record.contactId);
            rows.add(List.of(
                    record.id,
                    record.banquetId,
                    contact == null ? "" : contact.contactName,
                    record.direction,
                    record.sourceType,
                    value(record.amount),
                    value(record.occurredAt),
                    value(record.note)
            ));
        }
        return new ExportTable("favor", "人情账本", List.of("人情ID", "宴席ID", "联系人", "方向", "来源", "金额", "发生时间", "备注"), rows);
    }

    private ExportFile csvFile(Long banquetId, ExportTable table) {
        CsvWriter csv = new CsvWriter(table.headers());
        for (List<Object> row : table.rows()) {
            csv.row(row.toArray());
        }
        return csv.toFile(filename(banquetId, table.type(), "csv"));
    }

    private ExportFile xlsxFile(Long banquetId, ExportTable table) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(table.sheetName());
            CreationHelper helper = workbook.getCreationHelper();
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));
            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(30, Math.max(12, table.headers().get(i).length() + 6)) * 256);
            }

            for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<Object> values = table.rows().get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    writeCell(row.createCell(columnIndex), values.get(columnIndex), moneyStyle, timeStyle);
                }
            }

            workbook.write(output);
            return new ExportFile(filename(banquetId, table.type(), "xlsx"), output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create xlsx export", ex);
        }
    }

    private void writeCell(Cell cell, Object value, CellStyle moneyStyle, CellStyle timeStyle) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof BigDecimal money) {
            cell.setCellValue(money.doubleValue());
            cell.setCellStyle(moneyStyle);
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof LocalDateTime time) {
            cell.setCellValue(time);
            cell.setCellStyle(timeStyle);
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private void requireExportRight(Long banquetId) {
        RightsCheckResult result = planOrderService.checkBanquetRight(banquetId, RIGHT_CODE);
        if (!result.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前宴席版本不支持导出");
        }
    }

    private void ensureWithinRowLimit(String exportName, int fetchedRows) {
        if (fetchedRows > MAX_ROWS) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, exportName + "超过导出上限" + MAX_ROWS + "行，请缩小范围后重试");
        }
    }

    private <T> QueryWrapper<T> tenantScoped(QueryWrapper<T> query) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return query;
    }

    private String filename(Long banquetId, String type, String extension) {
        return "banquet-" + banquetId + "-" + type + "." + extension;
    }

    private String time(LocalDateTime value) {
        return value == null ? "" : TIME_FORMAT.format(value);
    }

    private String money(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String yesNo(Integer value) {
        return Integer.valueOf(1).equals(value) ? "是" : "否";
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> exportDetail(String format, ExportTable table) {
        return Map.of(
                "format", format,
                "type", table.type(),
                "rowCount", table.rows().size(),
                "maxRows", MAX_ROWS
        );
    }

    private record ExportTable(String type, String sheetName, List<String> headers, List<List<Object>> rows) {
    }

    private class CsvWriter {
        private final StringBuilder builder = new StringBuilder("\uFEFF");

        CsvWriter(List<String> headers) {
            row(headers.toArray());
        }

        void row(Object... values) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escape(values[i]));
            }
            builder.append('\n');
        }

        ExportFile toFile(String filename) {
            return new ExportFile(filename, builder.toString().getBytes(StandardCharsets.UTF_8));
        }

        private String escape(Object value) {
            String text;
            if (value instanceof BigDecimal money) {
                text = money(money);
            } else if (value instanceof LocalDateTime time) {
                text = time(time);
            } else {
                text = value == null ? "" : String.valueOf(value);
            }
            if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
                return "\"" + text.replace("\"", "\"\"") + "\"";
            }
            return text;
        }
    }
}
