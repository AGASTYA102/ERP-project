package com.erp.manufacturing.entity;

import com.erp.manufacturing.enums.MaterialType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Material name is required")
    @Column(nullable = false)
    private String materialName;

    @NotNull(message = "Material type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialType type;

    private Double quantity;
}
