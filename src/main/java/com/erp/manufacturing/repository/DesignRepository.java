package com.erp.manufacturing.repository;

import com.erp.manufacturing.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {
    Optional<Design> findByOrderId(Long orderId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.dieId FROM Design d WHERE d.dieId IS NOT NULL AND d.dieId != ''")
    java.util.List<String> findDistinctDieIds();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d.palleteId FROM Design d WHERE d.palleteId IS NOT NULL AND d.palleteId != ''")
    java.util.List<String> findDistinctPalleteIds();
}
