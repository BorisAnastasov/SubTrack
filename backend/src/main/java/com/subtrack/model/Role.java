package com.subtrack.model;

import com.subtrack.constant.DbConstants;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = DbConstants.ROLE_NAME_MAX_LENGTH)
    private String name;
}