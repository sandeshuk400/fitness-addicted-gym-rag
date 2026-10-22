package com.fitnessaddicted.gym.member.api;

import java.util.List;

import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.common.PageResponse;
import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.service.ConsentService;
import com.fitnessaddicted.gym.member.service.MemberProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberProfileService memberService;
    private final ConsentService consentService;

    public MemberController(MemberProfileService memberService, ConsentService consentService) {
        this.memberService = memberService;
        this.consentService = consentService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse me(@AuthenticationPrincipal Jwt jwt) {
        return memberService.getOwn(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMemberRequest request) {
        return memberService.updateOwn(CurrentUser.id(jwt), request);
    }

    @GetMapping("/me/consents")
    @PreAuthorize("hasRole('MEMBER')")
    public List<ConsentResponse> myConsents(@AuthenticationPrincipal Jwt jwt) {
        return consentService.current(CurrentUser.id(jwt));
    }

    @PutMapping("/me/consents/{purpose}")
    @PreAuthorize("hasRole('MEMBER')")
    public ConsentResponse updateConsent(@AuthenticationPrincipal Jwt jwt, @PathVariable ConsentPurpose purpose,
                                         @RequestBody ConsentRequest request) {
        return consentService.update(CurrentUser.id(jwt), purpose, request.granted());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<MemberResponse> search(@RequestParam(required = false) String search,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return memberService.search(search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public MemberResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return memberService.getForStaff(CurrentUser.id(jwt), CurrentUser.hasRole(jwt, "ADMIN"), id);
    }
}
