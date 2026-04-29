package com.openisle.controller;

import com.openisle.dto.ApiResponse;
import com.openisle.dto.PrizeDTO;
import com.openisle.model.Prize;
import com.openisle.service.PrizeManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/prize")
public class PrizeManageController {
    
    @Autowired
    private PrizeManageService prizeManageService;
    
    /**
     * 获取所有奖品（管理用）
     */
    @GetMapping("/list")
    public ApiResponse<List<Prize>> getAllPrizes() {
        try {
            List<Prize> prizes = prizeManageService.getAllPrizes();
            return ApiResponse.success(prizes);
        } catch (Exception e) {
            log.error("获取奖品列表失败", e);
            return ApiResponse.error("获取奖品列表失败");
        }
    }
    
    /**
     * 添加奖品
     */
    @PostMapping("/add")
    public ApiResponse<Boolean> addPrize(@RequestBody PrizeDTO prizeDTO) {
        try {
            boolean result = prizeManageService.addPrize(prizeDTO);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("添加奖品失败", e);
            return ApiResponse.error("添加奖品失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新奖品
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Boolean> updatePrize(@PathVariable Integer id, @RequestBody PrizeDTO prizeDTO) {
        try {
            boolean result = prizeManageService.updatePrize(id, prizeDTO);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("更新奖品失败", e);
            return ApiResponse.error("更新奖品失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除奖品
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Boolean> deletePrize(@PathVariable Integer id) {
        try {
            boolean result = prizeManageService.deletePrize(id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("删除奖品失败", e);
            return ApiResponse.error("删除奖品失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量更新概率
     */
    @PutMapping("/probability/batch")
    public ApiResponse<Boolean> batchUpdateProbability(@RequestBody List<PrizeDTO> prizes) {
        try {
            boolean result = prizeManageService.batchUpdateProbability(prizes);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("批量更新概率失败", e);
            return ApiResponse.error("批量更新概率失败：" + e.getMessage());
        }
    }
}