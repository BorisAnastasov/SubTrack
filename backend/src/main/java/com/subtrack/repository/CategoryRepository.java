package com.subtrack.repository;

import com.subtrack.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Връща както личните категории на потребителя, така и системните (където user е null)
    List<Category> findByUserIdOrUserIsNull(Long userId);
}