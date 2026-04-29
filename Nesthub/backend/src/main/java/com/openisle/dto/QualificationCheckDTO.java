package com.openisle.dto;

import lombok.Data;

@Data
public class QualificationCheckDTO {
    private Boolean hasQualification;
    private Integer activity;
    private Integer posts;
    private Integer minActivity;
    private Integer minPosts;
    private Integer remainingChances;
}