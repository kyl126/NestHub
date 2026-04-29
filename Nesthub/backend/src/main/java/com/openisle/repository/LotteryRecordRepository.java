package com.openisle.repository;

import com.openisle.model.LotteryRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LotteryRecordRepository extends JpaRepository<LotteryRecord, Integer> {
    
    @Query("SELECT r FROM LotteryRecord r WHERE r.userId = :userId ORDER BY r.createTime DESC")
    List<LotteryRecord> findRecentRecords(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM LotteryRecord r WHERE r.userId = :userId AND r.prizeId = :prizeId")
    int countUserWinTimes(@Param("userId") Integer userId, @Param("prizeId") Integer prizeId);
    
    // 简化版本：直接使用 JPA 方法名
    List<LotteryRecord> findByUserIdOrderByCreateTimeDesc(Integer userId);
}