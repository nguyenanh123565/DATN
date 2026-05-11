package com.smarthome.service;

import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.CategoryDto;
import com.smarthome.entity.Category;
import com.smarthome.repository.CategoryRepository;
import com.smarthome.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    public CategoryService(CategoryRepository categoryRepo, ProductRepository productRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
    }

    public List<CategoryDto.Response> getAllCategories() {
        // Lấy các danh mục gốc, sau đó toResponse sẽ tự đệ quy lấy con
        return categoryRepo.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toResponse).toList();
    }

    public List<CategoryDto.Response> getChildren(Long parentId) {
        return categoryRepo.findByParentIdAndIsActiveTrue(parentId)
                .stream().map(this::toResponse).toList();
    }

    public CategoryDto.Response getById(Long id) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));
        return toResponse(cat);
    }

    public CategoryDto.Response getBySlug(String slug) {
        Category cat = categoryRepo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục với slug: " + slug));
        return toResponse(cat);
    }

    @Transactional
    public CategoryDto.Response create(CategoryDto.CreateRequest req) {
        Category cat = new Category();
        cat.setName(req.getName());
        cat.setSlug(generateSlug(req.getName()));
        cat.setDescription(req.getDescription());
        cat.setImageUrl(req.getImageUrl());
        cat.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        cat.setIsActive(true);

        if (req.getParentId() != null) {
            categoryRepo.findById(req.getParentId()).ifPresent(cat::setParent);
        }
        return toResponse(categoryRepo.save(cat));
    }

    @Transactional
    public CategoryDto.Response update(Long id, CategoryDto.CreateRequest req) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));
        if (req.getName() != null) cat.setName(req.getName());
        if (req.getDescription() != null) cat.setDescription(req.getDescription());
        if (req.getImageUrl() != null)  cat.setImageUrl(req.getImageUrl());
        if (req.getSortOrder() != null) cat.setSortOrder(req.getSortOrder());
        return toResponse(categoryRepo.save(cat));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepo.deleteById(id);
    }

    private CategoryDto.Response toResponse(Category c) {
        CategoryDto.Response res = new CategoryDto.Response();
        res.setId(c.getId());
        res.setName(c.getName());
        res.setSlug(c.getSlug());
        res.setDescription(c.getDescription());
        res.setImageUrl(c.getImageUrl());
        res.setSortOrder(c.getSortOrder());
        res.setIsActive(c.getIsActive());
        if (c.getParent() != null) {
            res.setParentId(c.getParent().getId());
            res.setParentName(c.getParent().getName());
        }
        // Đếm số sản phẩm (bao gồm cả danh mục con)
        long count = productRepo.countByCategoryIdAndIsActiveTrue(c.getId());
        List<Category> children = categoryRepo.findByParentIdAndIsActiveTrue(c.getId());
        if (children != null && !children.isEmpty()) {
            res.setChildren(children.stream().map(this::toResponse).toList());
            // Cộng dồn count từ các con
            for (CategoryDto.Response child : res.getChildren()) {
                count += child.getProductCount();
            }
        }
        res.setProductCount(count);
        return res;
    }

    private String generateSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD);
        slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(slug).replaceAll("")
                .toLowerCase().replaceAll("[đ]", "d").replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-").replaceAll("-+", "-");
        String base = slug; int counter = 1;
        while (categoryRepo.existsBySlug(slug)) slug = base + "-" + counter++;
        return slug;
    }
}
