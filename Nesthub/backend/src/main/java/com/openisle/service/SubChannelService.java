package com.openisle.service;

import com.openisle.dto.SubChannelDto;
import com.openisle.model.Category;
import com.openisle.model.PostPermission;
import com.openisle.model.SubChannel;
import com.openisle.model.SubChannelType;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.SubChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubChannelService {

    @Autowired
    private SubChannelRepository subChannelRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ========== 查询 ==========
    
    public List<SubChannelDto> findByCategoryId(Long categoryId) {
        List<SubChannel> subChannels = subChannelRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);
        return subChannels.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private SubChannelDto toDto(SubChannel subChannel) {
        SubChannelDto dto = new SubChannelDto();
        dto.setId(subChannel.getId());
        dto.setCategoryId(subChannel.getCategory().getId());
        dto.setName(subChannel.getName());
        dto.setIcon(subChannel.getIcon());  // 👈 添加 icon
        dto.setType(subChannel.getType());
        dto.setPostPermission(subChannel.getPostPermission());
        dto.setSortOrder(subChannel.getSortOrder());
        dto.setEnabled(subChannel.getEnabled());  // 👈 添加 enabled
        return dto;
    }

    // ========== 创建 ==========
    
    public SubChannelDto create(Long categoryId, String name, String icon, String type, String postPermission) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        
        SubChannel subChannel = new SubChannel();
        subChannel.setCategory(category);
        subChannel.setName(name);
        subChannel.setIcon(icon != null ? icon : "💬");  // 👈 添加 icon
        subChannel.setType(SubChannelType.valueOf(type));
        subChannel.setPostPermission(PostPermission.valueOf(postPermission));
        subChannel.setSortOrder(99);
        subChannel.setEnabled(true);  // 👈 添加 enabled
        
        subChannel = subChannelRepository.save(subChannel);
        return toDto(subChannel);
    }

    public void createDefaultSubChannels(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        
        // 最新
        SubChannel latest = new SubChannel();
        latest.setCategory(category);  // 👈 改成 setCategory
        latest.setName("最新");
        latest.setIcon("🆕");  // 👈 添加 icon
        latest.setType(SubChannelType.LATEST);
        latest.setPostPermission(PostPermission.ALL);
        latest.setSortOrder(1);
        latest.setEnabled(true);
        subChannelRepository.save(latest);
        
        // 火
        SubChannel hot = new SubChannel();
        hot.setCategory(category);  // 👈 改成 setCategory
        hot.setName("火");
        hot.setIcon("🔥");  // 👈 添加 icon
        hot.setType(SubChannelType.HOT);
        hot.setPostPermission(PostPermission.ALL);
        hot.setSortOrder(2);
        hot.setEnabled(true);
        subChannelRepository.save(hot);
        
        // 官方
        SubChannel official = new SubChannel();
        official.setCategory(category);  // 👈 改成 setCategory
        official.setName("官方");
        official.setIcon("📢");  // 👈 添加 icon
        official.setType(SubChannelType.OFFICIAL);
        official.setPostPermission(PostPermission.ADMIN_ONLY);
        official.setSortOrder(3);
        official.setEnabled(true);
        subChannelRepository.save(official);

    
        SubChannel featured = new SubChannel();
        featured.setCategory(category);
        featured.setName("精华");
        featured.setIcon("⭐");
        featured.setType(SubChannelType.CUSTOM);
        featured.setPostPermission(PostPermission.ALL);
        featured.setSortOrder(4);
        featured.setEnabled(true);
        subChannelRepository.save(featured);
    }

    // ========== 更新 ==========
    
    public SubChannelDto update(Long id, String name, String icon, String postPermission) {
        SubChannel subChannel = subChannelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("子频道不存在"));

        // 默认子频道不允许修改名称
        if (subChannel.getType() != SubChannelType.CUSTOM) {
            throw new IllegalArgumentException("默认子频道不可修改名称");
        }

        if (name != null)
            subChannel.setName(name);
        if (icon != null)
            subChannel.setIcon(icon); // 👈 添加 icon 更新
        if (postPermission != null)
            subChannel.setPostPermission(PostPermission.valueOf(postPermission));

        subChannel = subChannelRepository.save(subChannel);
        return toDto(subChannel);
    }
    
        public SubChannelDto update(Long id, Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String icon = (String) payload.get("icon");
        String postPermission = (String) payload.get("postPermission");
        return update(id, name, icon, postPermission);
        }
    // ========== 状态切换 ==========
    
    public void toggleEnabled(Long id) {
        SubChannel subChannel = subChannelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子频道不存在"));
        subChannel.setEnabled(!subChannel.getEnabled());
        subChannelRepository.save(subChannel);
    }
    
    // ========== 排序 ==========
    
    public void updateSortOrder(Long id, Integer sortOrder) {
        SubChannel subChannel = subChannelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子频道不存在"));
        subChannel.setSortOrder(sortOrder);
        subChannelRepository.save(subChannel);
    }
    
    public void batchUpdateSortOrder(List<Long> subChannelIds) {
        for (int i = 0; i < subChannelIds.size(); i++) {
            SubChannel subChannel = subChannelRepository.findById(subChannelIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("子频道不存在"));
            subChannel.setSortOrder(i);
            subChannelRepository.save(subChannel);
        }
    }
    
        public long countByCategoryId(Long categoryId) {
        return subChannelRepository.countByCategoryId(categoryId);
    }

    public void deleteByCategoryId(Long categoryId) {
        List<SubChannel> subs = subChannelRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);
        subChannelRepository.deleteAll(subs);
    }

    public void reorder(List<Long> orderedIds) {
        batchUpdateSortOrder(orderedIds);
    }

    // ========== 删除 ==========
    
    public void deleteById(Long id) {
        SubChannel subChannel = subChannelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子频道不存在"));

        // 默认子频道不允许删除
        if (subChannel.getType() != SubChannelType.CUSTOM) {
            throw new IllegalArgumentException("默认子频道不可删除");
        }

        subChannelRepository.deleteById(id);
    }
}