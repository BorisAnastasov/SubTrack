package com.subtrack.controller;

import com.subtrack.dto.request.CategoryRequest;
import com.subtrack.dto.response.CategoryOptionResponse;
import com.subtrack.security.CustomUserDetails;
import com.subtrack.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryOptionResponse>> getCategories(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(categoryService.getCategories(user.getId()));
    }

    @PostMapping
    public ResponseEntity<CategoryOptionResponse> createCategory(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(user.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryOptionResponse> updateCategory(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        categoryService.deleteCategory(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
