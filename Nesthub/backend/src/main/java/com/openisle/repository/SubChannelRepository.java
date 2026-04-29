package com.openisle.repository;

import com.openisle.model.SubChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubChannelRepository extends JpaRepository<SubChannel, Long> {

    List<SubChannel> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    Optional<SubChannel> findByName(String name);

    List<SubChannel> findByCategoryIdAndEnabledTrueOrderBySortOrderAsc(Long categoryId);

    long countByCategoryId(Long categoryId);
}