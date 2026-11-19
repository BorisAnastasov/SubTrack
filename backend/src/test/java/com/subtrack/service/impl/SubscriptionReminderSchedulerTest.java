package com.subtrack.service.impl;

import com.subtrack.model.Subscription;
import com.subtrack.model.SubscriptionReminder;
import com.subtrack.model.User;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.repository.SubscriptionReminderRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.service.EmailService;
import com.subtrack.service.RenewalReminderEmailItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionReminderSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionReminderRepository reminderRepository;

    @Mock
    private EmailService emailService;

    private SubscriptionReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SubscriptionReminderScheduler(subscriptionRepository, reminderRepository, emailService);
        ReflectionTestUtils.setField(scheduler, "daysBefore", 3);
        ReflectionTestUtils.setField(scheduler, "reminderZone", "UTC");
    }

    @Test
    void sendsOneDigestAndRecordsAllUpcomingRenewalsForAUser() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate firstBillingDate = today.plusDays(2);
        LocalDate secondBillingDate = today.plusDays(3);
        Subscription firstSubscription = createSubscription(12L, firstBillingDate, "Cloud storage");
        Subscription secondSubscription = createSubscription(13L, secondBillingDate, "Music");
        when(subscriptionRepository.findByStatusAndNextBillingDateBetween(
                SubscriptionStatus.ACTIVE, today, today.plusDays(3)))
                .thenReturn(List.of(firstSubscription, secondSubscription));
        when(reminderRepository.existsBySubscriptionIdAndBillingDate(12L, firstBillingDate)).thenReturn(false);
        when(reminderRepository.existsBySubscriptionIdAndBillingDate(13L, secondBillingDate)).thenReturn(false);

        scheduler.sendUpcomingRenewalReminders();

        verify(emailService).sendRenewalDigestEmail(eq("owner@example.com"), eq(List.of(
                new RenewalReminderEmailItem("Cloud storage", new BigDecimal("4.99"), "EUR", firstBillingDate),
                new RenewalReminderEmailItem("Music", new BigDecimal("4.99"), "EUR", secondBillingDate))));
        verify(reminderRepository).saveAll(argThat(reminders -> List.of(firstBillingDate, secondBillingDate)
                .equals(StreamSupport.stream(reminders.spliterator(), false)
                        .map(SubscriptionReminder::getBillingDate)
                        .toList())));
    }

    @Test
    void doesNotSendWhenThereAreNoUpcomingRenewals() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        when(subscriptionRepository.findByStatusAndNextBillingDateBetween(
                SubscriptionStatus.ACTIVE, today, today.plusDays(3))).thenReturn(List.of());

        scheduler.sendUpcomingRenewalReminders();

        verify(emailService, never()).sendRenewalDigestEmail(any(), any());
        verify(reminderRepository, never()).save(any(SubscriptionReminder.class));
        verify(reminderRepository, never()).saveAll(any());
    }

    @Test
    void doesNotResendRenewalsAlreadyIncludedInADigest() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate billingDate = today.plusDays(2);
        Subscription subscription = createSubscription(12L, billingDate, "Cloud storage");
        when(subscriptionRepository.findByStatusAndNextBillingDateBetween(
                SubscriptionStatus.ACTIVE, today, today.plusDays(3))).thenReturn(List.of(subscription));
        when(reminderRepository.existsBySubscriptionIdAndBillingDate(12L, billingDate)).thenReturn(true);

        scheduler.sendUpcomingRenewalReminders();

        verify(emailService, never()).sendRenewalDigestEmail(any(), any());
        verify(reminderRepository, never()).saveAll(any());
    }

    private Subscription createSubscription(Long subscriptionId, LocalDate billingDate, String name) {
        User user = new User();
        user.setId(8L);
        user.setEmail("owner@example.com");
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUser(user);
        subscription.setName(name);
        subscription.setPrice(new BigDecimal("4.99"));
        subscription.setCurrency("EUR");
        subscription.setNextBillingDate(billingDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }
}
