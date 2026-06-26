package com.myweb.shoppe_fake.Controller;

import com.myweb.shoppe_fake.Model.Cart;
import com.myweb.shoppe_fake.Model.Order;
import com.myweb.shoppe_fake.Model.OrderDetail;
import com.myweb.shoppe_fake.Model.CartItem;
import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Service.CartService;
import com.myweb.shoppe_fake.Service.OrderService;
import com.myweb.shoppe_fake.Service.ProductService;
import com.myweb.shoppe_fake.Repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartRepository cartRepository;

    private String getLoggedInUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || 
        auth.getName().equals("anonymousUser")) {
        return null;
    }
    return auth.getName();
}

    // === CHECKOUT - Đặt hàng từ Giỏ ===
    @PostMapping("/checkout")
    public String checkout(
            @RequestParam(value = "shippingAddress") String shippingAddress,
            @RequestParam(value = "phone") String phone,
            HttpSession session,
            Model model) {

        String userId = getLoggedInUsername();
        if (userId == null) return "redirect:/login";

        Cart cart = cartService.getCartByUserId(userId);

        // Kiểm tra giỏ hàng rỗng
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi đặt hàng.");
            return "user/cart";
        }

        // Kiểm tra address và phone
        if (shippingAddress == null || shippingAddress.trim().isEmpty() || 
            phone == null || phone.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng điền đầy đủ địa chỉ và số điện thoại!");
            return "user/cart";
        }

        // Tạo Order từ Cart
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(new Date());
        order.setShippingAddress(shippingAddress);
        order.setPhone(phone);

        List<OrderDetail> orderDetails = new ArrayList<>();
        double totalAmount = 0;

        // Convert CartItems → OrderDetails & update product stock
        for (CartItem cartItem : cart.getItems()) {
            Optional<Product> productOpt = productService.getProductById(cartItem.getProductId());

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Kiểm tra stock
                if (product.getQuantity() < cartItem.getQuantity()) {
                    model.addAttribute("error", 
                        "Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho!");
                    return "user/cart";
                }

                // Tạo OrderDetail (lưu giá tại thời điểm đặt hàng)
                OrderDetail detail = new OrderDetail();
                detail.setProductId(cartItem.getProductId());
                detail.setProductName(product.getName()); // Thêm tên để hiển thị
                detail.setQuantity(cartItem.getQuantity());
                detail.setPrice(product.getPrice());

                orderDetails.add(detail);

                // Tính tổng tiền
                totalAmount += product.getPrice() * cartItem.getQuantity();

                // Giảm stock sản phẩm
                product.setQuantity(product.getQuantity() - cartItem.getQuantity());
                productService.saveProduct(product);
            }
        }

        order.setDetails(orderDetails);
        order.setTotalAmount(totalAmount);

        // Lưu Order vào database
        Order savedOrder = orderService.createOrder(order);

        // Xóa giỏ hàng sau khi đặt hàng thành công
        cartRepository.delete(cart);

        // Redirect tới trang order success
        return "redirect:/order/success/" + savedOrder.getId();
    }

    // === ORDER SUCCESS - Thành công đặt hàng ===
    @GetMapping("/success/{orderId}")
    public String orderSuccess(@PathVariable String orderId, Model model) {
        Optional<Order> orderOpt = orderService.getOrderById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            model.addAttribute("order", order);
            return "user/order-success";
        }

        model.addAttribute("error", "Không tìm thấy đơn hàng!");
        return "error";
    }

    // === ORDER HISTORY - Xem lịch sử đơn hàng ===
    @GetMapping("/history")
    public String orderHistory(HttpSession session, Model model) {
       String userId = getLoggedInUsername();
        if (userId == null) return "redirect:/login";
        List<Order> orders = orderService.getOrdersByUserId(userId);

        model.addAttribute("orders", orders);
        return "user/order-history";
    }

    // === ORDER DETAIL - Xem chi tiết đơn hàng ===
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable String orderId, Model model) {
        Optional<Order> orderOpt = orderService.getOrderById(orderId);

        if (orderOpt.isPresent()) {
            model.addAttribute("order", orderOpt.get());
            return "user/order-detail";
        }

        model.addAttribute("error", "Không tìm thấy đơn hàng!");
        return "error";
    }
}
