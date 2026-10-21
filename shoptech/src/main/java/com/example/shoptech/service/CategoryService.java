package com.example.shoptech.service;

import com.example.shoptech.dto.CategoryForm;
import com.example.shoptech.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    Page<Category> search(String keyword, Pageable pageable);

    Category getById(Long id);

    CategoryForm getFormById(Long id);

    void create(CategoryForm form);

    void update(Long id, CategoryForm form);

    void moveToTrash(Long id);

    List<Category> getAvailableCategories();
}