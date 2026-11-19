package com.subtrack.service.impl;

import com.subtrack.model.Subscription;
import com.subtrack.model.SubscriptionReminder;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.repository.SubscriptionReminderRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.service.EmailService;
import com.subtrack.service.RenewalReminderEmailItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReminderScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionReminderRepository reminderRepository;
    private final EmailService emailService;

    @Value("${app.reminders.days-before:3}")
    private int daysBefore;

    @Value("${app.reminders.zone:UTC}")
    private String reminderZone;

    @Scheduled(
            cron = "${app.reminders.cron:0 0 9 * * *}",
            zone = "${app.reminders.zone:UTC}"
    )
    public void sendUpcomingRenewalReminders() {
        if (daysBefore < 0) {
            throw new IllegalStateException("app.reminders.days-before must be zero or greater");
        }

        LocalDate today = LocalDate.now(ZoneId.of(reminderZone));
        List<Subscription> upcomingSubscriptions =
                subscriptionRepository.findByStatusAndNextBillingDateBetween(
                        SubscriptionStatus.ACTIVE, today, today.plusDays(daysBefore));

        Map<Long, List<Subscription>> subscriptionsByUser = new LinkedHashMap<>();
        for (Subscription subscription : upcomingSubscriptions) {
            subscriptionsByUser.computeIfAbsent(subscription.getUser().getId(), ignored -> new ArrayList<>())
                    .add(subscription);
        }

        for (List<Subscription> userSubscriptions : subscriptionsByUser.values()) {
            sendDigestIfNeeded(userSubscriptions);
        }
    }

    private void sendDigestIfNeeded(List<Subscription> subscriptions) {
        List<Subscription> unsentSubscriptions = subscriptions.stream()
                .filter(subscription -> !reminderRepository.existsBySubscriptionIdAndBillingDate(
                        subscription.getId(), subscription.getNextBillingDate()))
                .toList();
        if (unsentSubscriptions.isEmpty()) {
            return;
        }

        Subscription firstSubscription = unsentSubscriptions.get(0);

        try {
            List<RenewalReminderEmailItem> renewals = unsentSubscriptions.stream()
                    .map(subscription -> new RenewalReminderEmailItem(
                            subscription.getName(),
                            subscription.getPrice(),
                            subscription.getCurrency(),
                            subscription.getNextBillingDate()))
                    .toList();
            emailService.sendRenewalDigestEmail(firstSubscription.getUser().getEmail(), renewals);
            reminderRepository.saveAll(unsentSubscriptions.stream()
                    .map(subscription -> new SubscriptionReminder(subscription, subscription.getNextBillingDate()))
                    .toList());
        } catch (RuntimeException exception) {
            log.error("Could not send renewal digest for user {}",
                    firstSubscription.getUser().getId(), exception);
        }
    }
}
