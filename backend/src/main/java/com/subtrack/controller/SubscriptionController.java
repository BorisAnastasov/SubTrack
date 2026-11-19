package com.subtrack.controller;

import com.subtrack.dto.request.CreateSubscriptionRequest;
import com.subtrack.dto.request.UpdateSubscriptionRequest;
import com.subtrack.dto.request.UpdateSubscriptionStatusRequest;
import com.subtrack.dto.response.CategoryOptionResponse;
import com.subtrack.dto.response.EnumOptionResponse;
import com.subtrack.dto.response.SubscriptionOptionsResponse;
import com.subtrack.dto.response.SubscriptionResponse;
import com.subtrack.dto.response.SubscriptionSummaryResponse;
import com.subtrack.model.Category;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.repository.CategoryRepository;
import com.subtrack.security.CustomUserDetails;
import com.subtrack.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BillingCycle billingCycle,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "nextBillingDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(subscriptionService.searchSubscriptions(
                user.getId(), query, status, categoryId, billingCycle, fromDate, toDate, sortBy, sortDirection));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<SubscriptionResponse>> getUpcomingSubscriptions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(subscriptionService.getUpcomingSubscriptions(user.getId(), days));
    }

    @GetMapping("/summary")
    public ResponseEntity<SubscriptionSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(subscriptionService.getSummary(user.getId(), currency));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportSubscriptions(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subtrack-subscriptions.csv")
                .contentType(new MediaType("text", "csv"))
                .body(subscriptionService.exportSubscriptions(user.getId()));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.createSubscription(user.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> editSubscription(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.editSubscription(user.getId(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubscriptionResponse> updateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionStatusRequest request) {
        return ResponseEntity.ok(subscriptionService.updateStatus(user.getId(), id, request.status()));
    }

    @GetMapping("/options")
    public ResponseEntity<SubscriptionOptionsResponse> getOptions(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<EnumOptionResponse> billingCycles = Arrays.stream(BillingCycle.values())
                .map(cycle -> new EnumOptionResponse(cycle.name(), toTitleCase(cycle.name())))
                .toList();

        List<EnumOptionResponse> statuses = Arrays.stream(SubscriptionStatus.values())
                .map(status -> new EnumOptionResponse(status.name(), toTitleCase(status.name())))
                .toList();

        List<CategoryOptionResponse> categories = categoryRepository.findByUserIdOrUserIsNull(user.getId())
                .stream()
                .map(this::toCategoryOption)
                .toList();

        return ResponseEntity.ok(new SubscriptionOptionsResponse(billingCycles, statuses, categories));
    }

    private CategoryOptionResponse toCategoryOption(Category category) {
        return new CategoryOptionResponse(category.getId(), category.getName(), category.getColor());
    }

    private String toTitleCase(String value) {
        String lowerCase = value.toLowerCase();
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
