package com.openisle.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Data  // 确保有这个注解
@Entity
@Table(name = "lottery_record")
public class LotteryRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "prize_id")
    private Integer prizeId;
    
    @Column(name = "prize_name", length = 50)
    private String prizeName;
    
    @Column(name = "activity_value")
    private Integer activityValue;
    
    @Column(name = "post_value")
    private Integer postValue;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}