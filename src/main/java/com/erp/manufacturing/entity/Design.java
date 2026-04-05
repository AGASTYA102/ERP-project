package com.erp.manufacturing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    private String designFilePath;

    private String confirmationFilePath;

    @Builder.Default
    private Boolean urgent = false;

    private String assignedTo;

    private String dieType;

    private String dieId;

    private String dieMaker;
}
