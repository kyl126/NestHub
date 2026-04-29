package com.openisle.controller;

import com.openisle.dto.*;
import com.openisle.model.LotteryRecord;
import com.openisle.model.Prize;
import com.openisle.service.LotteryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/lottery")
@CrossOrigin(origins = "*")
public class LotteryController {
    
    @Autowired
    private LotteryService lotteryService;
    
    @PostMapping("/draw")
    public ApiResponse<LotteryResultDTO> drawLottery(@RequestBody LotteryRequest request) {
        try {
            LotteryResultDTO result = lotteryService.doLottery(
                request.getUserId(),
                request.getActivityValue(),
                request.getPostValue()
            );
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("抽奖接口异常", e);
            return ApiResponse.error("抽奖失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/prizes")
    public ApiResponse<List<Prize>> getPrizes() {
        try {
            List<Prize> prizes = lotteryService.getEnabledPrizes();
            return ApiResponse.success(prizes);
        } catch (Exception e) {
            log.error("获取奖品列表失败", e);
            return ApiResponse.error("获取奖品列表失败");
        }
    }
    
    @GetMapping("/records/{userId}")
    public ApiResponse<List<LotteryRecord>> getRecords(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LotteryRecord> records = lotteryService.getUserRecords(userId, limit);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("获取抽奖记录失败", e);
            return ApiResponse.error("获取抽奖记录失败");
        }
    }
    
    @GetMapping("/qualification/check")
    public ApiResponse<QualificationCheckDTO> checkQualification(
            @RequestParam Integer userId,
            @RequestParam Integer activity,
            @RequestParam Integer posts) {
        try {
            boolean hasQualification = lotteryService.checkQualification(userId, activity, posts);
            Integer remainingChances = lotteryService.getRemainingChances(userId);
            
            QualificationCheckDTO dto = new QualificationCheckDTO();
            dto.setHasQualification(hasQualification);
            dto.setActivity(activity);
            dto.setPosts(posts);
            dto.setMinActivity(100);
            dto.setMinPosts(5);
            dto.setRemainingChances(remainingChances);
            
            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("检查资格失败", e);
            return ApiResponse.error("检查资格失败");
        }
    }
}