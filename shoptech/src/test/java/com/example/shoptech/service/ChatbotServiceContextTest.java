package com.example.shoptech.service;

import com.example.shoptech.dto.ChatMessage;
import com.example.shoptech.entity.Category;
import com.example.shoptech.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotServiceContextTest {

    @Test
    void systemInstruction_shouldContainComprehensiveShopTechContextAndLinks() throws Exception {
        ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
        CategoryService categoryService = mock(CategoryService.class);

        Category cat = new Category();
        ReflectionTestUtils.setField(cat, "id", 10L);
        cat.setName("Laptop Gaming");
        when(categoryService.getAvailableCategories()).thenReturn(List.of(cat));

        Product product = new Product();
        product.setName("Laptop Asus ROG");
        product.setCategory(cat);
        product.setPrice(BigDecimal.valueOf(25000000));
        product.setStockQuantity(5);
        product.setSlug("laptop-asus-rog");
        when(productCatalogService.search(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 15), 1));

        ChatbotService service = new ChatbotService(
                productCatalogService,
                categoryService,
                "test-api-key",
                "gemini-3.5-flash-lite"
        );

        Method method = ChatbotService.class.getDeclaredMethod("systemInstruction");
        method.setAccessible(true);
        String instruction = (String) method.invoke(service);

        assertNotNull(instruction);
        // Verify key ShopTech contexts
        assertTrue(instruction.contains("ShopTech"));
        assertTrue(instruction.contains("/register"), "Must guide user to registration link");
        assertTrue(instruction.contains("/login"), "Must guide user to login link");
        assertTrue(instruction.contains("/shop-registration"), "Must guide user to seller shop registration");
        assertTrue(instruction.contains("/orders"), "Must guide user to order history");
        assertTrue(instruction.contains("/cart"), "Must guide user to cart");
        assertTrue(instruction.contains("Laptop Gaming"), "Must include available categories");
        assertTrue(instruction.contains("Laptop Asus ROG"), "Must include available products");
        assertTrue(instruction.contains("/products/laptop-asus-rog"), "Must include product slug URL");
        assertTrue(instruction.contains("1900-8888"), "Must include support hotline");
    }
}
