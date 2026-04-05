package com.erp.manufacturing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    private Double ratePerUnit;

    private String dimensions;

    private String sheetSize;

    private Integer ups;

    private String dieNo;

    private String plateId;

    @Column(columnDefinition = "TEXT")
    private String materialSpecs;
}
