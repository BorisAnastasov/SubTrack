package com.subtrack.service.impl;

import com.subtrack.constant.MessageConstants;
import com.subtrack.dto.request.CategoryRequest;
import com.subtrack.dto.response.CategoryOptionResponse;
import com.subtrack.model.Category;
import com.subtrack.model.Subscription;
import com.subtrack.model.User;
import com.subtrack.repository.CategoryRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.repository.UserRepository;
import com.subtrack.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryOptionResponse> getCategories(Long userId) {
        return categoryRepository.findByUserIdOrUserIsNull(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryOptionResponse createCategory(Long userId, CategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MessageConstants.ERROR_USER_NOT_FOUND + userId));
        Category category = new Category();
        category.setUser(user);
        category.setName(request.name().trim());
        category.setColor(request.color());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryOptionResponse updateCategory(Long userId, Long categoryId, CategoryRequest request) {
        Category category = getOwnedCategory(userId, categoryId);
        category.setName(request.name().trim());
        category.setColor(request.color());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = getOwnedCategory(userId, categoryId);
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        for (Subscription subscription : subscriptions) {
            if (subscription.getCategory() != null && subscription.getCategory().getId().equals(categoryId)) {
                subscription.setCategory(null);
            }
        }
        subscriptionRepository.saveAll(subscriptions);
        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        MessageConstants.ERROR_CATEGORY_NOT_FOUND + categoryId));
        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    MessageConstants.ERROR_CATEGORY_NOT_FOUND + categoryId);
        }
        return category;
    }

    private CategoryOptionResponse toResponse(Category category) {
        return new CategoryOptionResponse(category.getId(), category.getName(), category.getColor());
    }
}
