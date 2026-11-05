package com.subtrack.model;

import com.subtrack.constant.DbConstants;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = DbConstants.SUBSCRIPTION_NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, precision = DbConstants.DECIMAL_PRECISION, scale = DbConstants.DECIMAL_SCALE)
    private BigDecimal price;

    @Column(nullable = false, length = DbConstants.CURRENCY_CODE_LENGTH)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = DbConstants.ENUM_COLUMN_MAX_LENGTH)
    private BillingCycle billingCycle;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "payment_method", length = DbConstants.PAYMENT_METHOD_MAX_LENGTH)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = DbConstants.ENUM_COLUMN_MAX_LENGTH)
    private SubscriptionStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;
}