package com.openisle.service;

import com.openisle.dto.LotteryResultDTO;
import com.openisle.model.LotteryRecord;
import com.openisle.model.Prize;
import java.util.List;

public interface LotteryService {
    
    LotteryResultDTO doLottery(Integer userId, Integer activityValue, Integer postValue);
    
    List<Prize> getEnabledPrizes();
    
    List<LotteryRecord> getUserRecords(Integer userId, int limit);
    
    boolean checkQualification(Integer userId, Integer activityValue, Integer postValue);
    
    Integer getRemainingChances(Integer userId);
}