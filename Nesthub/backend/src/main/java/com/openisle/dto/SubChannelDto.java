package com.openisle.dto;

import com.openisle.model.PostPermission;
import com.openisle.model.SubChannelType;
import lombok.Data;

@Data
public class SubChannelDto {
    private Long id;
    private Long categoryId;
    private String name;
    private String icon;
    private SubChannelType type;
    private PostPermission postPermission;
    private Integer sortOrder;
    private Boolean enabled;
}