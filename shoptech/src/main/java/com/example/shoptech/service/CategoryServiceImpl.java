package com.example.shoptech.service;

import com.example.shoptech.dto.CategoryForm;
import com.example.shoptech.entity.Category;
import com.example.shoptech.repository.CategoryRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final Validator validator;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            Validator validator
    ) {
        this.categoryRepository = categoryRepository;
        this.validator = validator;
    }

    @Override
    public Page<Category> search(
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.strip();

        return categoryRepository.search(
                normalizedKeyword,
                pageable
        );
    }

    @Override
    public Category getById(Long id) {
        return categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy danh mục."
                ));
    }

    @Override
    public CategoryForm getFormById(Long id) {
        Category category = getById(id);

        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setIconClass(category.getIconClass());
        form.setActive(Boolean.TRUE.equals(category.getIsActive()));

        return form;
    }

    @Override
    @Transactional
    public void create(CategoryForm form) {
        validateForm(form);

        Category category = new Category();
        applyForm(category, form);
        category.setSlug(generateUniqueSlug(form.getName()));

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void update(Long id, CategoryForm form) {
        validateForm(form);

        Category category = getById(id);
        applyForm(category, form);

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void moveToTrash(Long id) {
        Category category = getById(id);

        if (categoryRepository.hasNonDeletedProducts(id)) {
            throw new IllegalArgumentException(
                    "Danh mục còn sản phẩm. "
                    + "Hãy chuyển sản phẩm sang danh mục khác "
                    + "hoặc xóa mềm sản phẩm trước."
            );
        }

        category.setIsDeleted(true);
        category.setDeletedAt(OffsetDateTime.now());

        categoryRepository.save(category);
    }

    @Override
    public List<Category> getAvailableCategories() {
        return categoryRepository
                .findByIsDeletedFalseAndIsActiveTrueOrderByNameAsc();
    }

    private void applyForm(
            Category category,
            CategoryForm form
    ) {
        category.setName(form.getName());
        category.setDescription(form.getDescription());
        category.setIconClass(form.getIconClass());
        category.setIsActive(form.isActive());
    }

    private void validateForm(CategoryForm form) {
        if (form == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu danh mục không hợp lệ."
            );
        }

        String errorMessage = validator.validate(form)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .findFirst()
                .orElse(null);

        if (errorMessage != null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = Normalizer.normalize(
                        name,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "danh-muc";
        }

        String slug = baseSlug;
        int suffix = 2;

        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }
}