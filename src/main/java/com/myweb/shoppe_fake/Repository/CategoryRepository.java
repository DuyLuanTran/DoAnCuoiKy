package com.myweb.shoppe_fake.Repository;




import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.myweb.shoppe_fake.Model.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    // Spring Data MongoDB đã tự động tạo sẵn cho bạn các hàm cơ bản: 
    // save(), findAll(), findById(), deleteById()
    
    // Bạn có thể tự định nghĩa thêm các hàm tìm kiếm tùy chỉnh nếu cần:
    boolean existsByName(String name);
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}