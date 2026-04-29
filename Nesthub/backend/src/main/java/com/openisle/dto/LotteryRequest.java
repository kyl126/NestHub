package com.openisle.dto;

import lombok.Data;

@Data
public class LotteryRequest {
    private Integer userId;
    private Integer activityValue;
    private Integer postValue;
}