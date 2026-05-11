package com.smarthome.repository;

import com.smarthome.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
    List<Category> findByParentIdAndIsActiveTrue(Long parentId);
    List<Category> findAllByIsActiveTrueOrderByNameAsc();
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
