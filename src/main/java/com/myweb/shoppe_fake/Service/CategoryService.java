package com.myweb.shoppe_fake.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.myweb.shoppe_fake.Model.Category;
import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Repository.CategoryRepository;
import com.myweb.shoppe_fake.Repository.ProductRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    // 1. Lấy danh sách tất cả danh mục
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 2. Thêm mới một danh mục
    public Category addCategory(Category category) {
        // Kiểm tra xem tên danh mục đã tồn tại chưa
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Tên danh mục đã tồn tại!");
        }
        return categoryRepository.save(category);
    }
     
    public Page<Category> getCategoriesWithPaginationAndSearch(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }
        return categoryRepository.findAll(pageable); // Lấy tất cả nhưng có phân trang
    }
    // 3. Lấy chi tiết 1 danh mục theo ID
    public Optional<Category> getCategoryById(String id) {
        return categoryRepository.findById(id);
    }

    public Category updateCategory(String id, Category categoryDetails) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        
        // Kiểm tra trùng tên nếu đổi sang một tên khác với tên hiện tại
        if (!existingCategory.getName().equals(categoryDetails.getName()) && 
            categoryRepository.existsByName(categoryDetails.getName())) {
            throw new RuntimeException("Tên danh mục đã tồn tại!");
        }

        // Cập nhật dữ liệu
        existingCategory.setName(categoryDetails.getName());
        existingCategory.setDescription(categoryDetails.getDescription());
        existingCategory.setActive(categoryDetails.isActive());

        return categoryRepository.save(existingCategory);
    }

    // 4. Xóa danh mục
    public void deleteCategory(String id) {
        List<Product> products = productRepository.findByCategoryId(id);
    
    // 2. Nếu danh sách không rỗng -> Có sản phẩm -> Chặn xóa
    if (products != null && !products.isEmpty()) {
        throw new RuntimeException("LỖI: Không thể xóa danh mục này vì đang chứa " + products.size() + " sản phẩm!");
    }
        categoryRepository.deleteById(id);
    }
}