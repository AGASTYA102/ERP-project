package com.erp.manufacturing.entity;

import com.erp.manufacturing.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "erp_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotNull(message = "Client is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String poNumber;

    private LocalDate deliveryDate;

    private LocalDate orderDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private com.erp.manufacturing.enums.PrintStatus printStatus = com.erp.manufacturing.enums.PrintStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private com.erp.manufacturing.enums.CorruStatus corruStatus = com.erp.manufacturing.enums.CorruStatus.PENDING;

    private Integer totalSheetsRequired;

    private String deliveryTruckNumber;

    private String billNo;
}
