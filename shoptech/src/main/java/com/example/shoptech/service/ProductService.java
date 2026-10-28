package com.example.shoptech.service;
import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.*;
import org.springframework.data.domain.*;
import java.util.List;
public interface ProductService {
    Page<Product> searchAdmin(String keyword,Long categoryId,Long shopId,Pageable pageable);
    Product getById(Long id);
    List<ProductModerationHistory> moderationHistory(Long id);
    void moderate(String username,Long id,boolean blocked,boolean expectedBlocked,String reason);
    Page<Product> searchSeller(String username,String keyword,Long categoryId,boolean trash,Pageable pageable);
    Product getSellerProduct(String username,Long id);
    ProductForm getSellerForm(String username,Long id);
    void createSeller(String username,ProductForm form);
    void updateSeller(String username,Long id,ProductForm form);
    void trashSeller(String username,Long id);
    void restoreSeller(String username,Long id);
}
