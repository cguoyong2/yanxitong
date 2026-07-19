package com.yanxitong.favor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yanxitong.favor.dto.CreateFamilyBookRequest;
import com.yanxitong.favor.dto.FamilyBookSummary;
import com.yanxitong.favor.dto.FamilyFavorManualEntryRequest;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.dto.FavorDetailResult;
import com.yanxitong.favor.dto.InviteFamilyMemberRequest;
import com.yanxitong.favor.entity.FavorContact;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.entity.FavorFamilyBook;
import com.yanxitong.favor.entity.FavorFamilyMember;
import com.yanxitong.favor.mapper.FavorContactMapper;
import com.yanxitong.favor.mapper.FavorEntryMapper;
import com.yanxitong.favor.mapper.FavorFamilyBookMapper;
import com.yanxitong.favor.mapper.FavorFamilyMemberMapper;
import com.yanxitong.operationlog.OperationLogService;
import com.yanxitong.operationlog.OperationModule;
import com.yanxitong.miniapp.MiniappPrincipalContext;
import com.yanxitong.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyFavorService {
    private final FavorFamilyBookMapper familyBookMapper;
    private final FavorFamilyMemberMapper familyMemberMapper;
    private final FavorContactMapper favorContactMapper;
    private final FavorEntryMapper favorEntryMapper;
    private final OperationLogService operationLogService;

    public FamilyFavorService(
            FavorFamilyBookMapper familyBookMapper,
            FavorFamilyMemberMapper familyMemberMapper,
            FavorContactMapper favorContactMapper,
            FavorEntryMapper favorEntryMapper,
            OperationLogService operationLogService
    ) {
        this.familyBookMapper = familyBookMapper;
        this.familyMemberMapper = familyMemberMapper;
        this.favorContactMapper = favorContactMapper;
        this.favorEntryMapper = favorEntryMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public FamilyBookSummary create(CreateFamilyBookRequest request) {
        FavorFamilyBook book = new FavorFamilyBook();
        book.tenantId = TenantContext.getTenantId();
        book.creatorUserId = MiniappPrincipalContext.currentUserId();
        book.bookName = request.bookName.trim();
        book.description = trimToNull(request.description);
        book.status = "ACTIVE";
        familyBookMapper.insert(book);

        FavorFamilyMember owner = new FavorFamilyMember();
        owner.tenantId = TenantContext.getTenantId();
        owner.familyBookId = book.id;
        owner.userId = MiniappPrincipalContext.currentUserId();
        owner.memberName = trimToNull(request.ownerName) == null ? "我" : request.ownerName.trim();
        owner.phone = trimToNull(request.ownerPhone);
        owner.relationship = "户主";
        owner.role = "OWNER";
        owner.permissions = "MANAGE,WRITE,READ";
        owner.inviteStatus = "JOINED";
        owner.joinedAt = LocalDateTime.now();
        familyMemberMapper.insert(owner);

        operationLogService.record(OperationModule.FAVOR, "CREATE_FAMILY_BOOK", "favor_family_book", book.id, "create family favor book");
        operationLogService.record(OperationModule.FAVOR, "ADD_FAMILY_MEMBER", "favor_family_member", owner.id, "add family owner");
        return summary(book.id);
    }

    public List<FamilyBookSummary> list() {
        QueryWrapper<FavorFamilyBook> query = tenantScoped(new QueryWrapper<FavorFamilyBook>())
                .eq("status", "ACTIVE")
                .orderByDesc("updated_at");
        Long userId = MiniappPrincipalContext.currentUserId();
        if (userId != null) {
            List<Long> joinedBookIds = familyMemberMapper.selectList(tenantScoped(new QueryWrapper<FavorFamilyMember>())
                            .eq("user_id", userId)
                            .eq("invite_status", "JOINED"))
                    .stream()
                    .map(member -> member.familyBookId)
                    .distinct()
                    .toList();
            query.and(wrapper -> {
                wrapper.eq("creator_user_id", userId);
                if (!joinedBookIds.isEmpty()) {
                    wrapper.or().in("id", joinedBookIds);
                }
            });
        }
        return familyBookMapper.selectList(query).stream()
                .map(book -> summary(book.id))
                .toList();
    }

    public FamilyBookSummary summary(Long familyBookId) {
        FavorFamilyBook book = requireBook(familyBookId);
        List<FavorFamilyMember> members = members(familyBookId);
        List<FavorContactSummary> contacts = contacts(familyBookId, null);
        BigDecimal received = contacts.stream().map(FavorContactSummary::receivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal given = contacts.stream().map(FavorContactSummary::givenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FamilyBookSummary(book, members, received, given, received.subtract(given), contacts.size());
    }

    @Transactional
    public FavorFamilyMember inviteMember(Long familyBookId, InviteFamilyMemberRequest request) {
        requireBook(familyBookId);
        FavorFamilyMember member = new FavorFamilyMember();
        member.tenantId = TenantContext.getTenantId();
        member.familyBookId = familyBookId;
        member.memberName = request.memberName.trim();
        member.phone = trimToNull(request.phone);
        member.relationship = trimToNull(request.relationship);
        member.role = trimToNull(request.role) == null ? "MEMBER" : request.role.trim();
        member.permissions = "WRITE,READ";
        member.inviteStatus = "JOINED";
        member.joinedAt = LocalDateTime.now();
        familyMemberMapper.insert(member);
        operationLogService.record(OperationModule.FAVOR, "ADD_FAMILY_MEMBER", "favor_family_member", member.id, "add family member");
        return member;
    }

    @Transactional
    public FavorEntry manualEntry(Long familyBookId, FamilyFavorManualEntryRequest request) {
        requireBook(familyBookId);
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
        entry.bookScope = "FAMILY";
        entry.bookId = familyBookId;
        entry.familyMemberId = request.familyMemberId;
        entry.operatorMemberId = request.operatorMemberId;
        entry.amount = request.amount;
        entry.occurredAt = request.occurredAt == null ? LocalDateTime.now() : request.occurredAt;
        entry.note = request.note;
        favorEntryMapper.insert(entry);
        operationLogService.record(OperationModule.FAVOR, "FAMILY_MANUAL_ENTRY", "favor_entry", entry.id, "family favor manual entry");
        return entry;
    }

    public List<FavorContactSummary> contacts(Long familyBookId, String keyword) {
        requireBook(familyBookId);
        QueryWrapper<FavorContact> query = tenantScoped(new QueryWrapper<FavorContact>());
        if (keyword != null && !keyword.isBlank()) {
            query.like("contact_name", keyword);
        }
        query.orderByDesc("updated_at");
        return favorContactMapper.selectList(query).stream()
                .map(contact -> summaryForContact(familyBookId, contact))
                .filter(summary -> summary.receivedAmount().compareTo(BigDecimal.ZERO) != 0
                        || summary.givenAmount().compareTo(BigDecimal.ZERO) != 0)
                .toList();
    }

    public FavorDetailResult detail(Long familyBookId, Long contactId) {
        requireBook(familyBookId);
        FavorContact contact = favorContactMapper.selectById(contactId);
        if (contact == null) {
            throw new IllegalArgumentException("Favor contact not found");
        }
        List<FavorEntry> entries = familyEntries(familyBookId, contactId);
        BigDecimal received = sum(entries, "RECEIVED");
        BigDecimal given = sum(entries, "GIVEN");
        return new FavorDetailResult(contact, received, given, received.subtract(given), entries);
    }

    public FavorDetailResult compareByName(Long familyBookId, String contactName) {
        FavorContact contact = favorContactMapper.selectOne(tenantScoped(new QueryWrapper<FavorContact>())
                .eq("contact_name", contactName)
                .last("LIMIT 1"));
        if (contact == null) {
            throw new IllegalArgumentException("Favor contact not found");
        }
        return detail(familyBookId, contact.id);
    }

    private FavorContactSummary summaryForContact(Long familyBookId, FavorContact contact) {
        List<FavorEntry> entries = familyEntries(familyBookId, contact.id);
        BigDecimal received = sum(entries, "RECEIVED");
        BigDecimal given = sum(entries, "GIVEN");
        return new FavorContactSummary(contact.id, contact.contactName, received, given, received.subtract(given));
    }

    private List<FavorEntry> familyEntries(Long familyBookId, Long contactId) {
        return favorEntryMapper.selectList(tenantScoped(new QueryWrapper<FavorEntry>())
                .eq("contact_id", contactId)
                .eq("book_scope", "FAMILY")
                .eq("book_id", familyBookId)
                .orderByDesc("occurred_at"));
    }

    private FavorFamilyBook requireBook(Long familyBookId) {
        FavorFamilyBook book = familyBookMapper.selectById(familyBookId);
        Long userId = MiniappPrincipalContext.currentUserId();
        if (book == null || !"ACTIVE".equals(book.status)
                || (userId != null && !userId.equals(book.creatorUserId) && !isJoinedMember(familyBookId, userId))) {
            throw new IllegalArgumentException("Family favor book not found");
        }
        return book;
    }

    private List<FavorFamilyMember> members(Long familyBookId) {
        return familyMemberMapper.selectList(tenantScoped(new QueryWrapper<FavorFamilyMember>())
                .eq("family_book_id", familyBookId)
                .orderByAsc("id"));
    }

    private boolean isJoinedMember(Long familyBookId, Long userId) {
        return familyMemberMapper.selectCount(tenantScoped(new QueryWrapper<FavorFamilyMember>())
                .eq("family_book_id", familyBookId)
                .eq("user_id", userId)
                .eq("invite_status", "JOINED")) > 0;
    }

    private FavorContact findOrCreateContact(String contactName, String phone) {
        QueryWrapper<FavorContact> query = tenantScoped(new QueryWrapper<FavorContact>())
                .eq("contact_name", contactName)
                .last("LIMIT 1");
        FavorContact contact = favorContactMapper.selectOne(query);
        if (contact != null) {
            return contact;
        }
        FavorContact created = new FavorContact();
        created.tenantId = TenantContext.getTenantId();
        created.contactName = contactName.trim();
        created.phone = trimToNull(phone);
        favorContactMapper.insert(created);
        return created;
    }

    private <T> QueryWrapper<T> tenantScoped(QueryWrapper<T> query) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            query.and(wrapper -> wrapper.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
        return query;
    }

    private BigDecimal sum(List<FavorEntry> entries, String direction) {
        return entries.stream()
                .filter(entry -> direction.equals(entry.direction))
                .map(entry -> entry.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
