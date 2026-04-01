package com.erp.manufacturing.repository;

import com.erp.manufacturing.entity.Production;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionRepository extends JpaRepository<Production, Long> {
    Optional<Production> findByOrderId(Long orderId);
}
