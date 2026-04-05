package com.erp.manufacturing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Client name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "gst_no")
    private String gstNo;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String contact;
}
