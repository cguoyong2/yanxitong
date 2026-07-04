package com.yanxitong.favor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.banquet.entity.Banquet;
import com.yanxitong.banquet.mapper.BanquetMapper;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.dto.FavorDetailResult;
import com.yanxitong.favor.dto.FavorManualEntryRequest;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.gift.entity.GiftRecord;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FavorService {
    private final FavorContactMapper favorContactMapper;
    private final FavorEntryMapper favorEntryMapper;
    private final BanquetMapper banquetMapper;
    private final OperationLogService operationLogService;

    public FavorService(
            FavorContactMapper favorContactMapper,
            FavorEntryMapper favorEntryMapper,
            BanquetMapper banquetMapper,
            OperationLogService operationLogService
    ) {
        this.favorContactMapper = favorContactMapper;
        this.favorEntryMapper = favorEntryMapper;
        this.banquetMapper = banquetMapper;
        this.operationLogService = operationLogService;
    }

    public FavorEntry recordReceivedGift(GiftRecord giftRecord) {
        FavorContact contact = findOrCreateContact(giftRecord.guestName, null);
        FavorEntry entry = new FavorEntry();
        entry.tenantId = TenantContext.getTenantId();
        entry.contactId = contact.id;
        entry.banquetId = giftRecord.banquetId;
        entry.giftRecordId = giftRecord.id;
        entry.direction = "RECEIVED";
        entry.sourceType = giftRecord.giftSource;
        applyBookScope(entry, giftRecord.banquetId);
        entry.amount = giftRecord.amount;
        entry.occurredAt = giftRecord.receivedAt;
        entry.note = giftRecord.blessing;
        favorEntryMapper.insert(entry);
        return entry;
    }

    public FavorEntry manualEntry(FavorManualEntryRequest request) {
        if (!"RECEIVED".equals(request.direction) && !"GIVEN".equals(request.direction)) {
            throw new IllegalArgumentException("Unsupported favor direction");
        }
        FavorContact contact = findOrCreateContact(request.contactName, request.phone);
        FavorEntry entry = new FavorEntry();
        entry.tenantId = TenantContext.getTenantId();
        entry.contactId = contact.id;
        entry.banquetId = request.banquetId;
        entry.direction = request.direction;
        entry.sourceType = "MANUAL";
        entry.bookScope = request.bookScope == null || request.bookScope.isBlank() ? "PERSONAL" : request.bookScope;
        entry.bookId = "FAMILY".equals(entry.bookScope) ? request.familyBookId : null;
        entry.amount = request.amount;
        entry.occurredAt = request.occurredAt == null ? LocalDateTime.now() : request.occurredAt;
        entry.note = request.note;
        favorEntryMapper.insert(entry);
        operationLogService.record(OperationModule.FAVOR, "MANUAL_ENTRY", "favor_entry", entry.id, "manual favor entry");
        return entry;
    }

    private void applyBookScope(FavorEntry entry, Long banquetId) {
        entry.bookScope = "PERSONAL";
        if (banquetId == null) {
            return;
        }
        Banquet banquet = banquetMapper.selectById(banquetId);
        if (banquet != null && "FAMILY".equals(banquet.favorBookScope) && banquet.favorFamilyBookId != null) {
            entry.bookScope = "FAMILY";
            entry.bookId = banquet.favorFamilyBookId;
        }
    }

    public List<FavorContactSummary> contacts(String keyword) {
        return contacts(keyword, null);
    }

    public List<FavorContactSummary> contacts(String keyword, Long banquetId) {
        QueryWrapper<FavorContact> query = new QueryWrapper<FavorContact>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like("contact_name", keyword);
        }
        query.orderByDesc("updated_at");
        List<FavorContact> contacts = favorContactMapper.selectList(query);
        return contacts.stream().map(contact -> {
            List<FavorEntry> entries = banquetId == null ? entries(contact.id) : entries(contact.id).stream()
                    .filter(entry -> banquetId.equals(entry.banquetId))
                    .toList();
            BigDecimal received = sum(entries, "RECEIVED");
            BigDecimal given = sum(entries, "GIVEN");
            return new FavorContactSummary(contact.id, contact.contactName, received, given, received.subtract(given));
        }).filter(summary -> banquetId == null
                || summary.receivedAmount().compareTo(BigDecimal.ZERO) != 0
                || summary.givenAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    public FavorDetailResult detail(Long contactId) {
        FavorContact contact = favorContactMapper.selectById(contactId);
        if (contact == null) {
            throw new IllegalArgumentException("Favor contact not found");
        }
        List<FavorEntry> entries = entries(contactId);
        BigDecimal received = sum(entries, "RECEIVED");
        BigDecimal given = sum(entries, "GIVEN");
        return new FavorDetailResult(contact, received, given, received.subtract(given), entries);
    }

    public FavorDetailResult compareByName(String contactName) {
        FavorContact contact = favorContactMapper.selectOne(new QueryWrapper<FavorContact>()
                .eq("contact_name", contactName)
                .last("LIMIT 1"));
        if (contact == null) {
            throw new IllegalArgumentException("Favor contact not found");
        }
        return detail(contact.id);
    }

    private FavorContact findOrCreateContact(String contactName, String phone) {
        QueryWrapper<FavorContact> query = new QueryWrapper<FavorContact>()
                .eq("contact_name", contactName)
                .last("LIMIT 1");
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        FavorContact contact = favorContactMapper.selectOne(query);
        if (contact != null) {
            return contact;
        }
        FavorContact created = new FavorContact();
        created.tenantId = TenantContext.getTenantId();
        created.contactName = contactName;
        created.phone = phone;
        favorContactMapper.insert(created);
        return created;
    }

    private List<FavorEntry> entries(Long contactId) {
        QueryWrapper<FavorEntry> query = new QueryWrapper<FavorEntry>()
                .eq("contact_id", contactId);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        query.orderByDesc("occurred_at");
        return favorEntryMapper.selectList(query);
    }

    private BigDecimal sum(List<FavorEntry> entries, String direction) {
        return entries.stream()
                .filter(entry -> direction.equals(entry.direction))
                .map(entry -> entry.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
