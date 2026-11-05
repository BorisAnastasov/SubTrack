package com.subtrack.model;

import com.subtrack.constant.DbConstants;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_obligations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaxObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = DbConstants.TAX_TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, precision = DbConstants.DECIMAL_PRECISION, scale = DbConstants.DECIMAL_SCALE)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid = false;

    @Column(name = "reminder_days_before")
    private Integer reminderDaysBefore;
}