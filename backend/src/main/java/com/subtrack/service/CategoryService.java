package com.subtrack.service;

import com.subtrack.dto.request.CategoryRequest;
import com.subtrack.dto.response.CategoryOptionResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryOptionResponse> getCategories(Long userId);

    CategoryOptionResponse createCategory(Long userId, CategoryRequest request);

    CategoryOptionResponse updateCategory(Long userId, Long categoryId, CategoryRequest request);

    void deleteCategory(Long userId, Long categoryId);
}
