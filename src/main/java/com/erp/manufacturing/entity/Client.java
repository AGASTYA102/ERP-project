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
    @jakarta.validation.constraints.Size(max = 255, message = "Name too long")
    @Column(nullable = false)
    private String name;

    @Column(name = "gst_no")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GST format")
    private String gstNo;

    @Column(columnDefinition = "TEXT")
    @jakarta.validation.constraints.Size(max = 2000, message = "Address too long")
    private String address;

    @jakarta.validation.constraints.Size(max = 50, message = "Contact too long")
    private String contact;
}
