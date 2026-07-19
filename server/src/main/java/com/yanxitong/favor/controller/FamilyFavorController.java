package com.yanxitong.favor.controller;

import com.yanxitong.common.ApiResponse;
import com.yanxitong.banquet.BanquetAccessService;
import com.yanxitong.favor.FamilyFavorService;
import com.yanxitong.favor.dto.CreateFamilyBookRequest;
import com.yanxitong.favor.dto.FamilyBookSummary;
import com.yanxitong.favor.dto.FamilyFavorManualEntryRequest;
import com.yanxitong.favor.dto.FavorContactSummary;
import com.yanxitong.favor.dto.FavorDetailResult;
import com.yanxitong.favor.dto.InviteFamilyMemberRequest;
import com.yanxitong.favor.entity.FavorEntry;
import com.yanxitong.favor.entity.FavorFamilyMember;
import com.yanxitong.miniapp.MiniappAuthenticated;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favor/family-books")
@MiniappAuthenticated
public class FamilyFavorController {
    private final FamilyFavorService familyFavorService;
    private final BanquetAccessService banquetAccessService;

    public FamilyFavorController(FamilyFavorService familyFavorService, BanquetAccessService banquetAccessService) {
        this.familyFavorService = familyFavorService;
        this.banquetAccessService = banquetAccessService;
    }

    @GetMapping
    public ApiResponse<List<FamilyBookSummary>> list() {
        return ApiResponse.ok(familyFavorService.list());
    }

    @PostMapping
    public ApiResponse<FamilyBookSummary> create(@Valid @RequestBody CreateFamilyBookRequest request) {
        return ApiResponse.ok(familyFavorService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<FamilyBookSummary> detail(@PathVariable Long id) {
        return ApiResponse.ok(familyFavorService.summary(id));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<FavorFamilyMember> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteFamilyMemberRequest request) {
        return ApiResponse.ok(familyFavorService.inviteMember(id, request));
    }

    @GetMapping("/{id}/contacts")
    public ApiResponse<List<FavorContactSummary>> contacts(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(familyFavorService.contacts(id, keyword));
    }

    @GetMapping("/{id}/contacts/{contactId}")
    public ApiResponse<FavorDetailResult> contactDetail(@PathVariable Long id, @PathVariable Long contactId) {
        return ApiResponse.ok(familyFavorService.detail(id, contactId));
    }

    @GetMapping("/{id}/compare")
    public ApiResponse<FavorDetailResult> compare(@PathVariable Long id, @RequestParam String contactName) {
        return ApiResponse.ok(familyFavorService.compareByName(id, contactName));
    }

    @PostMapping("/{id}/manual")
    public ApiResponse<FavorEntry> manual(
            @PathVariable Long id,
            @Valid @RequestBody FamilyFavorManualEntryRequest request) {
        if (request.banquetId != null) {
            banquetAccessService.requireAccessible(request.banquetId);
        }
        return ApiResponse.ok(familyFavorService.manualEntry(id, request));
    }
}
