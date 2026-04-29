package com.openisle.service;

import com.openisle.dto.PrizeDTO;
import com.openisle.model.Prize;
import java.util.List;

public interface PrizeManageService {
    
    /**
     * 获取所有奖品（包括禁用的）
     */
    List<Prize> getAllPrizes();
    
    /**
     * 添加奖品
     */
    boolean addPrize(PrizeDTO prizeDTO);
    
    /**
     * 更新奖品
     */
    boolean updatePrize(Integer id, PrizeDTO prizeDTO);
    
    /**
     * 删除奖品
     */
    boolean deletePrize(Integer id);
    
    /**
     * 批量更新概率
     */
    boolean batchUpdateProbability(List<PrizeDTO> prizes);
}