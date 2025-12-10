package com.librarysystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "borrowers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_borrower_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

}
