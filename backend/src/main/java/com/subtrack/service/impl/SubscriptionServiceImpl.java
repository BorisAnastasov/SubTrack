package com.subtrack.service.impl;

import com.subtrack.constant.MessageConstants;
import com.subtrack.dto.request.CreateSubscriptionRequest;
import com.subtrack.dto.request.UpdateSubscriptionRequest;
import com.subtrack.dto.response.CategorySpendingResponse;
import com.subtrack.dto.response.CurrencyTotalResponse;
import com.subtrack.dto.response.SubscriptionResponse;
import com.subtrack.dto.response.SubscriptionSummaryResponse;
import com.subtrack.dto.response.SubscriptionStatusChangeResponse;
import com.subtrack.service.ExchangeRateService;
import com.subtrack.model.Category;
import com.subtrack.model.Subscription;
import com.subtrack.model.SubscriptionStatusChange;
import com.subtrack.model.User;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.repository.CategoryRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.repository.SubscriptionStatusChangeRepository;
import com.subtrack.repository.UserRepository;
import com.subtrack.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusChangeRepository statusChangeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptions(Long userId) {
        return searchSubscriptions(userId, null, null, null, null, null, null, "nextBillingDate", "asc");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> searchSubscriptions(
            Long userId,
            String query,
            SubscriptionStatus status,
            Long categoryId,
            BillingCycle billingCycle,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDirection) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromDate must not be after toDate");
        }
        Comparator<Subscription> comparator = subscriptionComparator(sortBy, sortDirection);
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);

        return subscriptionRepository.findByUserId(userId).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> categoryId == null
                        || (item.getCategory() != null && item.getCategory().getId().equals(categoryId)))
                .filter(item -> billingCycle == null || item.getBillingCycle() == billingCycle)
                .filter(item -> fromDate == null || !item.getNextBillingDate().isBefore(fromDate))
                .filter(item -> toDate == null || !item.getNextBillingDate().isAfter(toDate))
                .filter(item -> matchesQuery(item, normalizedQuery))
                .sorted(comparator)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUpcomingSubscriptions(Long userId, int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be 7, 30, or 90");
        }
        LocalDate today = LocalDate.now();
        return searchSubscriptions(
                userId, null, SubscriptionStatus.ACTIVE, null, null, today, today.plusDays(days),
                "nextBillingDate", "asc");
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSummaryResponse getSummary(Long userId) {
        return getSummary(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSummaryResponse getSummary(Long userId, String displayCurrency) {
        String normalizedDisplayCurrency = displayCurrency == null || displayCurrency.isBlank()
                ? null : displayCurrency.trim().toUpperCase(Locale.ROOT);
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> annualTotals = new LinkedHashMap<>();
        Map<CategoryCurrencyKey, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<CategoryCurrencyKey, BigDecimal> categoryAnnualTotals = new LinkedHashMap<>();

        for (Subscription subscription : subscriptionRepository.findByUserIdAndStatus(
                userId, SubscriptionStatus.ACTIVE)) {
            BigDecimal monthlyAmount = monthlyAmount(subscription);
            BigDecimal annualAmount = annualAmount(subscription);
            String currency = subscription.getCurrency();
            if (normalizedDisplayCurrency != null) {
                monthlyAmount = exchangeRateService.convert(
                        monthlyAmount, currency, normalizedDisplayCurrency).convertedAmount();
                annualAmount = exchangeRateService.convert(
                        annualAmount, currency, normalizedDisplayCurrency).convertedAmount();
                currency = normalizedDisplayCurrency;
            }
            monthlyTotals.merge(currency, monthlyAmount, BigDecimal::add);
            annualTotals.merge(currency, annualAmount, BigDecimal::add);

            Category category = subscription.getCategory();
            CategoryCurrencyKey key = new CategoryCurrencyKey(
                    category == null ? null : category.getId(),
                    category == null ? "Uncategorized" : category.getName(),
                    category == null ? null : category.getColor(),
                    currency);
            categoryTotals.merge(key, monthlyAmount, BigDecimal::add);
            categoryAnnualTotals.merge(key, annualAmount, BigDecimal::add);
        }

        List<CategorySpendingResponse> categorySpending = categoryTotals.entrySet().stream()
                .map(entry -> new CategorySpendingResponse(
                        entry.getKey().categoryId(),
                        entry.getKey().categoryName(),
                        entry.getKey().categoryColor(),
                        entry.getKey().currency(),
                        entry.getValue(),
                        categoryAnnualTotals.get(entry.getKey())))
                .toList();

        return new SubscriptionSummaryResponse(
                normalizedDisplayCurrency,
                toCurrencyTotals(monthlyTotals),
                toCurrencyTotals(annualTotals),
                categorySpending);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportSubscriptions(Long userId) {
        StringBuilder csv = new StringBuilder(
                "name,price,currency,billingCycle,nextBillingDate,paymentMethod,category,status,notes\n");
        for (SubscriptionResponse item : getSubscriptions(userId)) {
            csv.append(csvValue(item.name())).append(',')
                    .append(csvValue(item.price().toPlainString())).append(',')
                    .append(csvValue(item.currency())).append(',')
                    .append(csvValue(item.billingCycle().name())).append(',')
                    .append(csvValue(item.nextBillingDate().toString())).append(',')
                    .append(csvValue(item.paymentMethod())).append(',')
                    .append(csvValue(item.categoryName())).append(',')
                    .append(csvValue(item.status().name())).append(',')
                    .append(csvValue(item.notes())).append('\n');
        }
        return csv.toString();
    }

    @Override
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MessageConstants.ERROR_USER_NOT_FOUND + userId));

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        apply(subscription, request.categoryId(), request.name(), request.price(), request.currency(),
                request.billingCycle(), request.nextBillingDate(), request.paymentMethod(), request.notes());
        subscription = subscriptionRepository.save(subscription);
        recordStatusChange(subscription, null, SubscriptionStatus.ACTIVE);
        return toResponse(subscription);
    }

    @Override
    public SubscriptionResponse editSubscription(
            Long userId, Long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = getOwnedSubscription(userId, subscriptionId);
        subscription.setCategory(
                request.categoryId() == null ? null : getCategory(userId, request.categoryId()));
        if (request.name() != null) {
            subscription.setName(request.name());
        }
        if (request.price() != null) {
            subscription.setPrice(request.price());
        }
        if (request.currency() != null) {
            subscription.setCurrency(request.currency());
        }
        if (request.billingCycle() != null) {
            subscription.setBillingCycle(request.billingCycle());
        }
        if (request.nextBillingDate() != null) {
            subscription.setNextBillingDate(request.nextBillingDate());
        }
        if (request.paymentMethod() != null) {
            subscription.setPaymentMethod(request.paymentMethod().isBlank() ? null : request.paymentMethod());
        }
        if (request.notes() != null) {
            subscription.setNotes(request.notes().isBlank() ? null : request.notes());
        }
        if (request.status() != null && subscription.getStatus() != request.status()) {
            SubscriptionStatus previousStatus = subscription.getStatus();
            subscription.setStatus(request.status());
            recordStatusChange(subscription, previousStatus, request.status());
        }
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    public SubscriptionResponse updateStatus(Long userId, Long subscriptionId, SubscriptionStatus status) {
        Subscription subscription = getOwnedSubscription(userId, subscriptionId);
        if (subscription.getStatus() != status) {
            SubscriptionStatus previousStatus = subscription.getStatus();
            subscription.setStatus(status);
            subscriptionRepository.save(subscription);
            recordStatusChange(subscription, previousStatus, status);
        }
        return toResponse(subscription);
    }

    private void apply(
            Subscription subscription,
            Long categoryId,
            String name,
            BigDecimal price,
            String currency,
            BillingCycle billingCycle,
            LocalDate nextBillingDate,
            String paymentMethod,
            String notes) {
        subscription.setCategory(categoryId == null ? null : getCategory(subscription.getUser().getId(), categoryId));
        subscription.setName(name);
        subscription.setPrice(price);
        subscription.setCurrency(currency);
        subscription.setBillingCycle(billingCycle);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setNotes(notes);
    }

    private Category getCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MessageConstants.ERROR_CATEGORY_NOT_FOUND + categoryId));
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    MessageConstants.ERROR_CATEGORY_NOT_FOUND + categoryId);
        }
        return category;
    }

    private Subscription getOwnedSubscription(Long userId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MessageConstants.ERROR_SUBSCRIPTION_NOT_FOUND + subscriptionId));
    }

    private void recordStatusChange(
            Subscription subscription, SubscriptionStatus previousStatus, SubscriptionStatus nextStatus) {
        SubscriptionStatusChange change = new SubscriptionStatusChange();
        change.setSubscription(subscription);
        change.setFromStatus(previousStatus);
        change.setToStatus(nextStatus);
        statusChangeRepository.save(change);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        Category category = subscription.getCategory();
        List<SubscriptionStatusChangeResponse> history =
                statusChangeRepository.findBySubscriptionIdOrderByChangedAtDesc(subscription.getId()).stream()
                        .map(change -> new SubscriptionStatusChangeResponse(
                                change.getFromStatus(), change.getToStatus(), change.getChangedAt()))
                        .toList();
        return new SubscriptionResponse(
                subscription.getId(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getColor(),
                subscription.getName(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingCycle(),
                subscription.getNextBillingDate(),
                subscription.getPaymentMethod(),
                subscription.getStatus(),
                subscription.getNotes(),
                history
        );
    }

    private Comparator<Subscription> subscriptionComparator(String sortBy, String sortDirection) {
        String field = sortBy == null ? "nextBillingDate" : sortBy;
        Comparator<Subscription> comparator = switch (field) {
            case "name" -> Comparator.comparing(Subscription::getName, String.CASE_INSENSITIVE_ORDER);
            case "price" -> Comparator.comparing(Subscription::getPrice);
            case "status" -> Comparator.comparing(Subscription::getStatus);
            case "category" -> Comparator.comparing(
                    item -> item.getCategory() == null ? null : item.getCategory().getName(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "billingCycle" -> Comparator.comparing(Subscription::getBillingCycle);
            case "nextBillingDate" -> Comparator.comparing(Subscription::getNextBillingDate);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sortBy value");
        };
        String direction = sortDirection == null ? "asc" : sortDirection;
        if ("desc".equalsIgnoreCase(direction)) {
            return comparator.reversed();
        }
        if (!"asc".equalsIgnoreCase(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDirection must be asc or desc");
        }
        return comparator;
    }

    private boolean matchesQuery(Subscription subscription, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        Category category = subscription.getCategory();
        return contains(subscription.getName(), query)
                || contains(subscription.getPaymentMethod(), query)
                || contains(subscription.getNotes(), query)
                || (category != null && contains(category.getName(), query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private BigDecimal monthlyAmount(Subscription subscription) {
        BigDecimal price = subscription.getPrice();
        return switch (subscription.getBillingCycle()) {
            case WEEKLY -> price.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case MONTHLY -> price;
            case YEARLY -> price.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal annualAmount(Subscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case WEEKLY -> subscription.getPrice().multiply(BigDecimal.valueOf(52));
            case MONTHLY -> subscription.getPrice().multiply(BigDecimal.valueOf(12));
            case YEARLY -> subscription.getPrice();
        };
    }

    private List<CurrencyTotalResponse> toCurrencyTotals(Map<String, BigDecimal> totals) {
        return totals.entrySet().stream()
                .map(entry -> new CurrencyTotalResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record CategoryCurrencyKey(Long categoryId, String categoryName, String categoryColor, String currency) {
    }
}
