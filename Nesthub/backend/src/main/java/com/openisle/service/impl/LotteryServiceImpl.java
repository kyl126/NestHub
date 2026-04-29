package com.openisle.service.impl;

import com.openisle.dto.LotteryResultDTO;
import com.openisle.model.LotteryRecord;
import com.openisle.model.Prize;
import com.openisle.repository.LotteryRecordRepository;
import com.openisle.repository.PrizeRepository;
import com.openisle.service.LotteryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LotteryServiceImpl implements LotteryService {
    
    @Autowired
    private PrizeRepository prizeRepository;
    
    @Autowired
    private LotteryRecordRepository lotteryRecordRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_QUALIFICATION_KEY = "lottery:qualification:";
    private static final String DAILY_LOTTERY_LIMIT = "lottery:daily:limit:";
    private static final int DEFAULT_MIN_ACTIVITY = 100;
    private static final int DEFAULT_MIN_POSTS = 5;
    private static final int DEFAULT_MAX_DAILY = 10;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryResultDTO doLottery(Integer userId, Integer activityValue, Integer postValue) {
        LotteryResultDTO result = new LotteryResultDTO();
        
        try {
            if (!checkQualification(userId, activityValue, postValue)) {
                result.setIsWin(false);
                result.setMessage("不满足抽奖资格要求");
                return result;
            }
            
            if (!checkDailyLimit(userId)) {
                result.setIsWin(false);
                result.setMessage("今日抽奖次数已达上限");
                return result;
            }
            
            List<Prize> prizes = prizeRepository.findEnabledPrizes();
            if (CollectionUtils.isEmpty(prizes)) {
                log.error("没有可用的奖品配置");
                result.setIsWin(false);
                result.setMessage("系统配置错误");
                return result;
            }
            
            Prize winPrize = lotteryByProbability(prizes);
            
            LotteryRecord record = new LotteryRecord();
            record.setUserId(userId);
            record.setPrizeId(winPrize.getId());
            record.setPrizeName(winPrize.getName());
            record.setActivityValue(activityValue);
            record.setPostValue(postValue);
            lotteryRecordRepository.save(record);
            
            consumeQualification(userId);
            incrementDailyCount(userId);
            
            result.setPrizeId(winPrize.getId());
            result.setPrizeName(winPrize.getName());
            result.setPrizeValue(winPrize.getValue());
            result.setIsWin(!"谢谢参与".equals(winPrize.getName()));
            result.setMessage(result.getIsWin() ? "恭喜中奖！" : "再接再厉");
            result.setRecordId(record.getId());
            
            log.info("用户{}抽中奖品: {}", userId, winPrize.getName());
            
        } catch (Exception e) {
            log.error("抽奖失败", e);
            result.setIsWin(false);
            result.setMessage("抽奖失败，请稍后重试");
        }
        
        return result;
    }
    
    @Override
    public List<Prize> getEnabledPrizes() {
        return prizeRepository.findEnabledPrizes();
    }
    
    @Override
    public List<LotteryRecord> getUserRecords(Integer userId, int limit) {
        List<LotteryRecord> allRecords = lotteryRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        if (allRecords == null || allRecords.isEmpty()) {
            return new ArrayList<>();
        }
        if (allRecords.size() > limit) {
            return allRecords.subList(0, limit);
        }
        return allRecords;
    }
    
    @Override
    public boolean checkQualification(Integer userId, Integer activityValue, Integer postValue) {
        boolean meetsRequirement = activityValue >= DEFAULT_MIN_ACTIVITY && postValue >= DEFAULT_MIN_POSTS;
        if (!meetsRequirement) {
            return false;
        }
        Integer remainingChances = getRemainingChances(userId);
        return remainingChances == null || remainingChances > 0;
    }
    
    @Override
    public Integer getRemainingChances(Integer userId) {
        String key = USER_QUALIFICATION_KEY + userId;
        Object chances = redisTemplate.opsForValue().get(key);
        if (chances == null) {
            return 1;
        }
        return (Integer) chances;
    }
    
    private Prize lotteryByProbability(List<Prize> prizes) {
        List<Prize> prizeList = new ArrayList<>(prizes);
        
        BigDecimal totalProb = prizeList.stream()
            .map(Prize::getProbability)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalProb.compareTo(BigDecimal.ONE) < 0) {
            Prize thanks = new Prize();
            thanks.setId(0);
            thanks.setName("谢谢参与");
            thanks.setValue("再接再厉");
            thanks.setProbability(BigDecimal.ONE.subtract(totalProb));
            prizeList.add(thanks);
        }
        
        double random = Math.random();
        double cumulative = 0.0;
        
        for (Prize prize : prizeList) {
            cumulative += prize.getProbability().doubleValue();
            if (random <= cumulative) {
                return prize;
            }
        }
        
        return prizeList.get(prizeList.size() - 1);
    }
    
    private boolean checkDailyLimit(Integer userId) {
        String key = DAILY_LOTTERY_LIMIT + userId + ":" + 
            LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Integer todayCount = (Integer) redisTemplate.opsForValue().get(key);
        return todayCount == null || todayCount < DEFAULT_MAX_DAILY;
    }
    
    private void consumeQualification(Integer userId) {
        String key = USER_QUALIFICATION_KEY + userId;
        Integer remaining = getRemainingChances(userId);
        if (remaining != null && remaining > 0) {
            redisTemplate.opsForValue().set(key, remaining - 1, 24, TimeUnit.HOURS);
        }
    }
    
    private void incrementDailyCount(Integer userId) {
        String key = DAILY_LOTTERY_LIMIT + userId + ":" + 
            LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }
}