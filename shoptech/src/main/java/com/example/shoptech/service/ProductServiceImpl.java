package com.example.shoptech.service;

import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.Category;
import com.example.shoptech.entity.Product;
import com.example.shoptech.repository.CategoryRepository;
import com.example.shoptech.repository.ProductRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final Validator validator;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CloudinaryService cloudinaryService,
            Validator validator
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cloudinaryService = cloudinaryService;
        this.validator = validator;
    }

    @Override
    public Page<Product> searchAdmin(
            String keyword,
            Long categoryId,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.strip();

        return productRepository.searchAdmin(
                normalizedKeyword,
                categoryId,
                pageable
        );
    }

    @Override
    public Product getById(Long id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy sản phẩm."
                ));
    }

    @Override
    public ProductForm getFormById(Long id) {
        Product product = getById(id);

        ProductForm form = new ProductForm();
        form.setCategoryId(product.getCategory().getId());
        form.setName(product.getName());
        form.setSku(product.getSku());
        form.setDescription(product.getDescription());
        form.setSpecifications(product.getSpecifications());
        form.setPrice(product.getPrice());
        form.setDiscountPrice(product.getDiscountPrice());
        form.setStockQuantity(product.getStockQuantity());
        form.setActive(Boolean.TRUE.equals(product.getIsActive()));

        return form;
    }

    @Override
    @Transactional
    public void create(ProductForm form) {
        validateForm(form);
        validateSku(form.getSku(), null);

        Category category = getUsableCategory(
                form.getCategoryId(),
                null
        );

        MultipartFile imageFile = form.getImageFile();

        if (!hasNewImage(imageFile)) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ảnh chính cho sản phẩm."
            );
        }

        cloudinaryService.validateImage(imageFile);

        Product product = new Product();
        applyForm(product, form, category);
        product.setSlug(generateUniqueSlug(form.getName()));

        CloudinaryService.UploadedImage uploadedImage =
                cloudinaryService.uploadImage(imageFile);

        product.setMainImage(uploadedImage.secureUrl());

        productRepository.saveAndFlush(product);
    }

    @Override
    @Transactional
    public void update(Long id, ProductForm form) {
        validateForm(form);

        Product product = getById(id);
        validateSku(form.getSku(), id);

        Category category = getUsableCategory(
                form.getCategoryId(),
                product.getCategory().getId()
        );

        MultipartFile imageFile = form.getImageFile();

        if (hasNewImage(imageFile)) {
            cloudinaryService.validateImage(imageFile);
        }

        applyForm(product, form, category);

        if (hasNewImage(imageFile)) {
            CloudinaryService.UploadedImage uploadedImage =
                    cloudinaryService.uploadImage(imageFile);

            product.setMainImage(uploadedImage.secureUrl());
        }

        // Giữ nguyên slug và ảnh cũ nếu không chọn ảnh mới.
        productRepository.saveAndFlush(product);
    }

    @Override
    @Transactional
    public void moveToTrash(Long id) {
        Product product = getById(id);

        product.setIsDeleted(true);
        product.setDeletedAt(OffsetDateTime.now());

        productRepository.saveAndFlush(product);
    }

    private void applyForm(
            Product product,
            ProductForm form,
            Category category
    ) {
        product.setCategory(category);
        product.setName(form.getName());
        product.setSku(form.getSku());
        product.setDescription(form.getDescription());
        product.setSpecifications(form.getSpecifications());
        product.setPrice(form.getPrice());
        product.setDiscountPrice(form.getDiscountPrice());
        product.setStockQuantity(form.getStockQuantity());
        product.setIsActive(form.isActive());
    }

    private void validateForm(ProductForm form) {
        if (form == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu sản phẩm không hợp lệ."
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

    private void validateSku(String sku, Long productId) {
        if (sku == null || sku.isBlank()) {
            return;
        }

        boolean duplicated = productId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(
                        sku,
                        productId
                );

        if (duplicated) {
            throw new IllegalArgumentException(
                    "Mã sản phẩm đã tồn tại."
            );
        }
    }

    private Category getUsableCategory(
            Long categoryId,
            Long currentCategoryId
    ) {
        Category category = categoryRepository
                .findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Danh mục không tồn tại hoặc đã bị xóa."
                ));

        boolean keepingCurrentCategory =
                Objects.equals(categoryId, currentCategoryId);

        if (!Boolean.TRUE.equals(category.getIsActive())
                && !keepingCurrentCategory) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn danh mục đang bật."
            );
        }

        return category;
    }

    private boolean hasNewImage(MultipartFile file) {
        return file != null && !file.isEmpty();
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
            baseSlug = "san-pham";
        }

        String slug = baseSlug;
        int suffix = 2;

        while (productRepository.existsBySlug(slug)) {
            String suffixText = "-" + suffix;
            int maxBaseLength = 255 - suffixText.length();

            String shortenedBase = baseSlug.substring(
                    0,
                    Math.min(baseSlug.length(), maxBaseLength)
            );

            slug = shortenedBase + suffixText;
            suffix++;
        }

        return slug;
    }
}