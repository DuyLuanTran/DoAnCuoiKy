package com.myweb.shoppe_fake.Service;

import com.myweb.shoppe_fake.Model.Cart;
import com.myweb.shoppe_fake.Model.CartItem;
import com.myweb.shoppe_fake.Model.User;
import com.myweb.shoppe_fake.Repository.CartRepository;
import com.myweb.shoppe_fake.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserRepository userRepository;

    public Cart getCartByUserId(String userId) {
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        // Nếu user chưa có giỏ hàng thì tạo mới
        if (cart.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            newCart.setItems(new ArrayList<>()); // Khởi tạo danh sách items
            return cartRepository.save(newCart);
        }
        
        // Đảm bảo items không null khi lấy từ database
        Cart existingCart = cart.get();
        if (existingCart.getItems() == null) {
            existingCart.setItems(new ArrayList<>());
            return cartRepository.save(existingCart);
        }
        return existingCart;
    }

    public Cart saveCart(Cart cart) { return cartRepository.save(cart); }
    
    public void addToCart(String userId, String productId, int quantity) {
        Cart cart = getCartByUserId(userId);
        
        boolean isProductExist = false;
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                isProductExist = true;
                break;
            }
        }

        if (!isProductExist) {
            CartItem newItem = new CartItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
    }

    // 3. Xóa khỏi giỏ hàng
    public void removeFromCart(String userId, String productId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cartRepository.save(cart);
    }
    
    }
    
