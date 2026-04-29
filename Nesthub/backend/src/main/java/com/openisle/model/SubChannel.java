package com.openisle.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "sub_channels")
@Getter
@Setter
public class SubChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "channel_id", nullable = false)
    @JsonIgnore
    private Category category;  // 👈 改成 Category

    @Column(nullable = false)
    private String name;
    
    private String icon = "💬";  // 👈 添加 icon

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubChannelType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostPermission postPermission = PostPermission.ALL;

    private Integer sortOrder = 0;
    
    private Boolean enabled = true;  // 👈 添加启用状态

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}