package com.openisle.controller;

import com.openisle.dto.SubChannelDto;
import com.openisle.model.Category;
import com.openisle.model.Post;
import com.openisle.repository.PostRepository;
import com.openisle.service.CategoryService;
import com.openisle.service.SubChannelService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final SubChannelService subChannelService;
    private final PostRepository postRepository;
    private final CacheManager cacheManager;

    // ========== 频道管理 ==========

    @GetMapping("/channels")
    public List<Category> listChannels() {
        return categoryService.findAll();
    }

    @PostMapping("/channels")
    public Category createChannel(@Valid @RequestBody Category category) {
        return categoryService.create(category);
    }

    @PutMapping("/channels/{id}")
    public Category updateChannel(@PathVariable Long id, @Valid @RequestBody Category category) {
        category.setId(id);
        return categoryService.update(category);
    }

    @DeleteMapping("/channels/{id}")
    public ResponseEntity<?> deleteChannel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cascade) {
        if (cascade) {
            categoryService.deleteWithAssociations(id);
        } else {
            long subCount = subChannelService.countByCategoryId(id);
            long postCount = categoryService.countPostsByCategoryId(id);
            if (subCount > 0 || postCount > 0) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "该频道下存在 " + subCount + " 个子频道和 " + postCount + " 个帖子，无法删除。请先清空或勾选级联删除。");
                return ResponseEntity.badRequest().body(error);
            }
            categoryService.deleteById(id);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/channels/{id}/posts-count")
    public ResponseEntity<Map<String, Long>> getPostsCount(@PathVariable Long id) {
        long count = categoryService.countPostsByCategoryId(id);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/channels/{id}/posts")
    public List<Post> listPostsByChannel(@PathVariable Long id) {
        Category category = categoryService.getCategory(id);
        return postRepository.findByCategory(category);
    }

    // ========== 子频道管理 ==========

    @GetMapping("/channels/{channelId}/sub-channels")
    public List<SubChannelDto> listSubChannels(@PathVariable Long channelId) {
        return subChannelService.findByCategoryId(channelId);
    }

    @GetMapping("/sub-channels/by-channel/{channelId}")
    public List<SubChannelDto> getSubChannelsByChannel(@PathVariable Long channelId) {
        return subChannelService.findByCategoryId(channelId);
    }

    @PostMapping("/channels/{channelId}/sub-channels")
    public SubChannelDto createSubChannel(
            @PathVariable Long channelId,
            @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String type = (String) payload.getOrDefault("type", "CUSTOM");
        String postPermission = (String) payload.getOrDefault("postPermission", "ALL");
        String icon = (String) payload.getOrDefault("icon", "💬");
        return subChannelService.create(channelId, name, icon, type, postPermission);
    }

    @PutMapping("/sub-channels/{id}")
    public SubChannelDto updateSubChannel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return subChannelService.update(id, payload);
    }

    @DeleteMapping("/sub-channels/{id}")
    public ResponseEntity<Void> deleteSubChannel(@PathVariable Long id) {
        subChannelService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sub-channels/reorder")
    public ResponseEntity<Void> reorderSubChannels(@RequestBody List<Long> orderedIds) {
        subChannelService.reorder(orderedIds);
        return ResponseEntity.ok().build();
    }
        @PostMapping("/cache/clear-categories")
    public ResponseEntity<Map<String, String>> clearCategoryCache() {
        categoryService.clearCategoryCache();
        return ResponseEntity.ok(Map.of("message", "Category cache cleared"));
    }
    
}