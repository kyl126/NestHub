package com.openisle.dto;

import lombok.Data;

@Data
public class LotteryResultDTO {
    private Integer prizeId;
    private String prizeName;
    private String prizeValue;
    private Boolean isWin;
    private String message;
    private Integer recordId;
}