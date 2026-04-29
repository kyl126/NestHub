package com.openisle.repository;

import com.openisle.model.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {
    
    @Query("SELECT p FROM Prize p WHERE p.status = 1 ORDER BY p.sortOrder ASC, p.id ASC")
    List<Prize> findEnabledPrizes();
    
    @Query("SELECT SUM(p.probability) FROM Prize p WHERE p.status = 1")
    BigDecimal getTotalProbability();
    
    List<Prize> findByStatusOrderBySortOrderAsc(Integer status);
}