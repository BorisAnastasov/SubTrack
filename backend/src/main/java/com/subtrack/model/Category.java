package com.subtrack.model;

import com.subtrack.constant.DbConstants;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = DbConstants.CATEGORY_NAME_MAX_LENGTH)
    private String name;

    @Column(length = DbConstants.COLOR_HEX_MAX_LENGTH)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}