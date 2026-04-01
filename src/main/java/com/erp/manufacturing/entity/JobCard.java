package com.erp.manufacturing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    private String boxDimensions;
    private String sheetSize;
    private Integer sheetQuantity;
    private String dieNo;
    private String plateId;
    private Integer copies;
}
