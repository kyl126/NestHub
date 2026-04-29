package com.openisle.service.impl;

import com.openisle.dto.PrizeDTO;
import com.openisle.model.Prize;
import com.openisle.repository.PrizeRepository;
import com.openisle.service.PrizeManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PrizeManageServiceImpl implements PrizeManageService {
    
    @Autowired
    private PrizeRepository prizeRepository;
    
    @Override
    public List<Prize> getAllPrizes() {
        return prizeRepository.findAll();
    }
    
    @Override
    @Transactional
    public boolean addPrize(PrizeDTO prizeDTO) {
        try {
            Prize prize = new Prize();
            BeanUtils.copyProperties(prizeDTO, prize);
            prizeRepository.save(prize);
            log.info("添加奖品成功: {}", prize.getName());
            return true;
        } catch (Exception e) {
            log.error("添加奖品失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updatePrize(Integer id, PrizeDTO prizeDTO) {
        try {
            Optional<Prize> optional = prizeRepository.findById(id);
            if (optional.isPresent()) {
                Prize prize = optional.get();
                BeanUtils.copyProperties(prizeDTO, prize);
                prizeRepository.save(prize);
                log.info("更新奖品成功: id={}, name={}", id, prize.getName());
                return true;
            }
            log.warn("奖品不存在: id={}", id);
            return false;
        } catch (Exception e) {
            log.error("更新奖品失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean deletePrize(Integer id) {
        try {
            prizeRepository.deleteById(id);
            log.info("删除奖品成功: id={}", id);
            return true;
        } catch (Exception e) {
            log.error("删除奖品失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean batchUpdateProbability(List<PrizeDTO> prizes) {
        try {
            for (PrizeDTO dto : prizes) {
                Optional<Prize> optional = prizeRepository.findById(dto.getId());
                if (optional.isPresent()) {
                    Prize prize = optional.get();
                    prize.setProbability(dto.getProbability());
                    prizeRepository.save(prize);
                }
            }
            log.info("批量更新概率成功, 数量: {}", prizes.size());
            return true;
        } catch (Exception e) {
            log.error("批量更新概率失败", e);
            return false;
        }
    }
}