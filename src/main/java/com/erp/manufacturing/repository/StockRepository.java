package com.erp.manufacturing.repository;

import com.erp.manufacturing.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Stock> findByMaterialName(String materialName);
}
