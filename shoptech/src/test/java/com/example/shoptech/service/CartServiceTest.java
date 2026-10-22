package com.example.shoptech.service;

import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock UserRepository users; @Mock ProductRepository products;
    @Mock CartRepository carts; @Mock CartItemRepository items;
    @InjectMocks CartService service;
    User user; Cart cart; Product product; CartItem item;
    @BeforeEach void setup() {
        user = User.builder().id(1L).username("alice").build();
        cart = new Cart(); cart.setId(2L); cart.setUserId(1L);
        Category category = new Category(); category.setIsActive(true); category.setIsDeleted(false);
        product = new Product(); product.setName("Test"); product.setCategory(category); product.setPrice(new BigDecimal("100")); product.setStockQuantity(5);
        item = new CartItem(); item.setId(3L); item.setCartId(2L); item.setProduct(product); item.setQuantity(1);
    }
    void owner() { when(users.lockActiveByUsername("alice")).thenReturn(Optional.of(user)); when(carts.findByUserId(1L)).thenReturn(Optional.of(cart)); }
    void addProduct() { owner(); when(products.findByIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(product)); }
    @Test void addsWithServerDiscountAndNoStockMutation() {
        addProduct(); product.setDiscountPrice(new BigDecimal("75"));
        when(items.findByCartIdOrderByProductIdAsc(2L)).thenReturn(List.of(item));
        assertEquals(new BigDecimal("75"), service.add("alice", 4L, 1).total());
        assertEquals(5, product.getStockQuantity()); verify(items).saveAndFlush(any());
    }
    @Test void mergesExistingQuantity() { addProduct(); when(items.findByCartIdAndProductId(2L,4L)).thenReturn(Optional.of(item)); service.add("alice",4L,2); assertEquals(3,item.getQuantity()); }
    @Test void createsCartUnderOwnerLock() {
        when(users.lockActiveByUsername("alice")).thenReturn(Optional.of(user));
        when(carts.saveAndFlush(any())).thenReturn(cart); when(products.findByIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(product));
        service.add("alice",4L,1); InOrder order = inOrder(users,carts); order.verify(users).lockActiveByUsername("alice"); order.verify(carts).findByUserId(1L); order.verify(carts).saveAndFlush(any());
    }
    @Test void updatesOwnedItem() { owner(); when(items.findByIdAndCartId(3L,2L)).thenReturn(Optional.of(item)); service.update("alice",3L,4); assertEquals(4,item.getQuantity()); }
    @Test void removesOwnedItem() { owner(); when(items.findByIdAndCartId(3L,2L)).thenReturn(Optional.of(item)); service.remove("alice",3L); verify(items).delete(item); }
    @Test void rejectsInvalidQuantity() { assertThrows(ResponseStatusException.class, () -> service.add("alice",4L,0)); verifyNoInteractions(users); }
    @Test void rejectsStockOverflow() { addProduct(); assertThrows(ResponseStatusException.class, () -> service.add("alice",4L,6)); verify(items,never()).saveAndFlush(any()); }
    @Test void rejectsDeleted() { addProduct(); product.setIsDeleted(true); assertThrows(ResponseStatusException.class, () -> service.add("alice",4L,1)); }
    @Test void rejectsInactive() { addProduct(); product.setIsActive(false); assertThrows(ResponseStatusException.class, () -> service.add("alice",4L,1)); }
    @Test void rejectsMissingProduct() { owner(); assertThrows(ResponseStatusException.class, () -> service.add("alice",4L,1)); }
    @Test void preventsIdor() { owner(); assertThrows(ResponseStatusException.class, () -> service.remove("alice",99L)); verify(items,never()).delete(any()); }
    @Test void emptyCart() { when(users.findByUsernameAndIsDeletedFalse("alice")).thenReturn(Optional.of(user)); assertTrue(service.get("alice").items().isEmpty()); }
    @Test void rejectsUnknownOwner() { assertThrows(ResponseStatusException.class, () -> service.get("unknown")); }
}
