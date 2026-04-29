package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.model.Category;
import com.openisle.model.Post;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.PostRepository;
import com.openisle.search.SearchIndexEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SearchIndexEventPublisher searchIndexEventPublisher;
    private final SubChannelService subChannelService;
    private final PostRepository postRepository;
    private final CacheManager cacheManager;
    private final ChatService chatService;

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public Category createCategory(String name, String description, String icon, String smallIcon) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        category.setSmallIcon(smallIcon);
        return create(category);
    }

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public Category updateCategory(
        Long id,
        String name,
        String description,
        String icon,
        String smallIcon
    ) {
        Category category = categoryRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (name != null) {
            category.setName(name);
        }
        if (description != null) {
            category.setDescription(description);
        }
        if (icon != null) {
            category.setIcon(icon);
        }
        if (smallIcon != null) {
            category.setSmallIcon(smallIcon);
        }
        Category saved = categoryRepository.save(category);
        searchIndexEventPublisher.publishCategorySaved(saved);
        return saved;
    }

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
        searchIndexEventPublisher.publishCategoryDeleted(id);
    }

    public Category getCategory(Long id) {
        return categoryRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    
    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    public List<Category> listEnabledCategories() {
        return categoryRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    public List<Long> getSearchCategoryIds(List<Long> categoryIds, Long categoryId) {
        List<Long> ids = categoryIds;
        if (categoryId != null) {
            ids = List.of(categoryId);
        }
        return ids;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }
    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public Category create(Category category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getEnabled() == null) {
            category.setEnabled(true);
        }
        Category saved = categoryRepository.save(category);
        searchIndexEventPublisher.publishCategorySaved(saved);
        subChannelService.createDefaultSubChannels(saved.getId());
        chatService.getOrCreateChannelRoom(saved.getId(), saved.getName());
        return saved;
    }

    public Category update(Category category) {
        Category existing = categoryRepository.findById(category.getId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setIcon(category.getIcon());
        existing.setSmallIcon(category.getSmallIcon());
        if (category.getSortOrder() != null) {
            existing.setSortOrder(category.getSortOrder());
        }
        if (category.getEnabled() != null) {
            existing.setEnabled(category.getEnabled());
        }
        Category saved = categoryRepository.save(existing);
        searchIndexEventPublisher.publishCategorySaved(saved);
        return saved;
    }

    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
        searchIndexEventPublisher.publishCategoryDeleted(id);
    }

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public void toggleCategoryStatus(Long id) {
        Category category = getCategory(id);
        category.setEnabled(!category.getEnabled());
        categoryRepository.save(category);
    }

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public void batchUpdateSortOrder(List<Long> categoryIds) {
        for (int i = 0; i < categoryIds.size(); i++) {
            Category category = getCategory(categoryIds.get(i));
            category.setSortOrder(i);
            categoryRepository.save(category);
        }
    }

    @CacheEvict(value = CachingConfig.CATEGORY_CACHE_NAME, allEntries = true)
    public void deleteWithAssociations(Long id) {
        Category category = getCategory(id);

        // 1. 查找或创建"未分类"频道
        Category uncategorized = categoryRepository.findByName("未分类")
            .orElseGet(() -> {
                Category newCat = new Category();
                newCat.setName("未分类");
                newCat.setDescription("从已删除频道转入的帖子");
                newCat.setIcon("📦");
                newCat.setSortOrder(999);
                newCat.setEnabled(true);
                Category saved = categoryRepository.save(newCat);
                    subChannelService.createDefaultSubChannels(saved.getId());
                chatService.getOrCreateChannelRoom(saved.getId(), saved.getName());
                return saved;
            });

        // 2. 将该频道下所有帖子转移到"未分类"
        List<Post> posts = postRepository.findByCategory(category);
        for (Post post : posts) {
            post.setCategory(uncategorized);
        }
        postRepository.saveAll(posts);

        // 3. 删子频道
        subChannelService.deleteByCategoryId(id);

        // 4. 删频道
        categoryRepository.deleteById(id);
        searchIndexEventPublisher.publishCategoryDeleted(id);
    }

    public long countPostsByCategoryId(Long id) {
        return postRepository.countByCategory_Id(id);
    }
        public void clearCategoryCache() {
        var cache = cacheManager.getCache(CachingConfig.CATEGORY_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }
}