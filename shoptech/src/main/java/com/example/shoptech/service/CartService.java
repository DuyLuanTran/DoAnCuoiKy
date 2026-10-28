package com.example.shoptech.service;

import com.example.shoptech.dto.CartDto;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartService {
    private final UserRepository users;
    private final ProductRepository products;
    private final CartRepository carts;
    private final CartItemRepository items;
    public CartService(UserRepository users, ProductRepository products, CartRepository carts, CartItemRepository items) {
        this.users = users; this.products = products; this.carts = carts; this.items = items;
    }
    public CartDto get(String username) {
        User user = users.findByUsernameAndIsDeletedFalse(username)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại."));
        return carts.findByUserId(user.getId()).map(c -> view(c.getId())).orElse(new CartDto(List.of(), BigDecimal.ZERO, 0));
    }
    @Transactional
    public CartDto add(String username, Long productId, int quantity) {
        checkQuantity(quantity);
        User user = lockUser(username);
        Cart cart = carts.findByUserId(user.getId()).orElseGet(() -> {
            Cart created = new Cart(); created.setUserId(user.getId()); return carts.saveAndFlush(created);
        });
        Product product = products.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
        CartItem item = items.findByCartIdAndProductId(cart.getId(), productId).orElseGet(CartItem::new);
        long combined = (item.getQuantity() == null ? 0L : item.getQuantity().longValue()) + quantity;
        validateProduct(product, combined);
        item.setCartId(cart.getId()); item.setProduct(product); item.setQuantity((int) combined);
        items.saveAndFlush(item);
        return view(cart.getId());
    }
    @Transactional
    public CartDto update(String username, Long itemId, int quantity) {
        checkQuantity(quantity);
        Cart cart = existingCart(lockUser(username));
        CartItem item = ownedItem(cart, itemId);
        validateProduct(item.getProduct(), quantity);
        item.setQuantity(quantity); items.flush();
        return view(cart.getId());
    }
    @Transactional
    public CartDto remove(String username, Long itemId) {
        Cart cart = existingCart(lockUser(username));
        items.delete(ownedItem(cart, itemId)); items.flush();
        return view(cart.getId());
    }
    private CartItem ownedItem(Cart cart, Long itemId) {
        return items.findByIdAndCartId(itemId, cart.getId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ."));
    }
    private Cart existingCart(User user) {
        return carts.findByUserId(user.getId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Giỏ hàng trống."));
    }
    private User lockUser(String username) {
        // Lock the existing owner even when the cart has not been created yet.
        return users.lockActiveByUsername(username).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại."));
    }
    private CartDto view(Long cartId) {
        List<CartDto.Item> lines = items.findByCartIdOrderByProductIdAsc(cartId).stream().map(item -> {
            Product p = item.getProduct(); BigDecimal price = price(p);
            return new CartDto.Item(item.getId(), p.getId(), p.getName(), p.getMainImage(), p.getSlug(), price,
                    item.getQuantity(), p.getStockQuantity(), price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }).toList();
        return new CartDto(lines, lines.stream().map(CartDto.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                lines.stream().mapToLong(CartDto.Item::quantity).sum());
    }
    static BigDecimal price(Product product) {
        BigDecimal price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
        if (price == null || price.signum() < 0) throw error(HttpStatus.CONFLICT, "Giá sản phẩm không hợp lệ.");
        return price;
    }
    static void validateProduct(Product p, long quantity) {
        if (Boolean.TRUE.equals(p.getIsBlocked()) || p.getShop() == null
                || p.getShop().getStatus() != ShopStatus.ACTIVE || p.getShop().getOwner() == null
                || !Boolean.TRUE.equals(p.getShop().getOwner().getIsActive()) || Boolean.TRUE.equals(p.getShop().getOwner().getIsDeleted()))
            throw error(HttpStatus.CONFLICT, "Sản phẩm hoặc shop đã ngừng kinh doanh.");
        if (!Boolean.TRUE.equals(p.getIsActive()) || Boolean.TRUE.equals(p.getIsDeleted()) || p.getCategory() == null
                || !Boolean.TRUE.equals(p.getCategory().getIsActive()) || Boolean.TRUE.equals(p.getCategory().getIsDeleted()))
            throw error(HttpStatus.CONFLICT, "Sản phẩm đã ngừng kinh doanh.");
        if (quantity < 1 || p.getStockQuantity() == null || quantity > p.getStockQuantity())
            throw error(HttpStatus.CONFLICT, "Số lượng vượt tồn kho.");
        price(p);
    }
    private static void checkQuantity(int quantity) {
        if (quantity < 1 || quantity > 1000000) throw error(HttpStatus.BAD_REQUEST, "Số lượng không hợp lệ.");
    }
    static ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
