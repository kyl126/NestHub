package com.openisle.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data  // 确保有这个注解
@Entity
@Table(name = "prize")
public class Prize {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String value;
    
    @Column(precision = 5, scale = 4)
    private BigDecimal probability;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    private Integer status;
    
    @Column(length = 200)
    private String icon;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}