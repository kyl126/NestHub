package com.openisle.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PrizeDTO {
    private Integer id;
    private String name;
    private String value;
    private BigDecimal probability;
    private Integer sortOrder;
    private Integer status;
    private String icon;
}