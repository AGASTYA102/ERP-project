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

    // Designer Workflow Fields
    private String designerFileUrl;
    private String confirmationSsUrl;
    
    @Builder.Default
    private Boolean isNewDie = false;
    private String dieId;
    private String dieMaker;

    @Builder.Default
    private Boolean isNewPallete = false;
    private String palleteId;
    private String palleteMaker;
}
