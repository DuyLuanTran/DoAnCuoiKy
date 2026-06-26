package com.myweb.shoppe_fake.Component; 

import com.myweb.shoppe_fake.Model.Category;
import com.myweb.shoppe_fake.Model.Order;
import com.myweb.shoppe_fake.Model.OrderDetail;
import com.myweb.shoppe_fake.Model.Product;
import com.myweb.shoppe_fake.Model.User;
import com.myweb.shoppe_fake.Repository.CategoryRepository;
import com.myweb.shoppe_fake.Repository.OrderRepository;
import com.myweb.shoppe_fake.Repository.ProductRepository;
import com.myweb.shoppe_fake.Repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public DatabaseSeeder(CategoryRepository categoryRepository, ProductRepository productRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }
    // Hàm phụ trợ tạo Category chuẩn với Model của bạn
    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        // field 'active' đã mặc định là true theo model của bạn
        return categoryRepository.save(category);
    }

    // Hàm phụ trợ tạo Product chuẩn với Model của bạn
    private Product createProduct(String name, double price, int quantity, String image, String description, String categoryId) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setQuantity(quantity); // Đã thêm quantity
        product.setImage(image);
        product.setDescription(description);
        product.setCategoryId(categoryId);
        // field 'active' đã mặc định là true
        return product;
    }
    private User createUser(String username, String password, String fullName, String email, String phone, String address, Set<String> roles, boolean active) {
        User user = new User();
        user.setUsername(username);
        // Nếu dùng Spring Security, nhớ bọc password bằng PasswordEncoder: passwordEncoder.encode(password)
        user.setPassword(password); 
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address); // Đã thêm trường address
        user.setRoles(roles);     // Dùng Set<String>
        user.setActive(active);
        return user;
    }
    private OrderDetail createOrderDetail(Product product, int quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProductId(product.getId());
        detail.setProductName(product.getName());
        detail.setQuantity(quantity);
        detail.setPrice(product.getPrice());
        return detail;
    }
    private Order createOrder(User user, List<OrderDetail> details, String status, int daysAgo) {
        Order order = new Order();
        order.setUserId(user.getId());
        order.setPhone(user.getPhone());
        order.setShippingAddress(user.getAddress() != null ? user.getAddress() : "123 Đường Mặc Định, TP.HCM");
        order.setStatus(status);

        // Lùi ngày đặt hàng để data trông thực tế hơn (từ quá khứ đến hiện tại)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        order.setOrderDate(cal.getTime());

        order.setDetails(details);

        // Tính tổng tiền tự động dựa trên chi tiết
        double total = 0;
        for (OrderDetail d : details) {
            total += (d.getPrice() * d.getQuantity());
        }
        order.setTotalAmount(total);

        return order;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            
            // 1. Tạo Danh mục
            Category ao = createCategory("Áo Thời Trang", "Các mẫu áo nam nữ hot trend");
            Category quan = createCategory("Quần Nam/Nữ", "Quần jean, quần tây, short");
            Category vay = createCategory("Đầm & Váy", "Váy thiết kế, đầm dự tiệc");
            Category phuKien = createCategory("Phụ Kiện", "Kính râm, túi xách, giày dép");

            // 2. Tạo 20 Sản phẩm (Mặc định set số lượng tồn kho là 50)
            productRepository.saveAll(Arrays.asList(
    // ---- ÁO ----
    createProduct("Áo Thun Nam Cổ Tròn Basic", 150000.0, 50, "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=500&q=80", "Áo thun cotton 100% thấm hút mồ hôi tốt.", ao.getId()),
    createProduct("Áo Sơ Mi Trắng Nam Công Sở", 250000.0, 50, "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=500&q=80", "Sơ mi lụa mềm mịn, chống nhăn.", ao.getId()),
    createProduct("Áo Khoác Denim Phong Cách", 450000.0, 50, "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?w=500&q=80", "Áo khoác bò nam nữ đều mặc được.", ao.getId()),
    createProduct("Áo Hoodie Nỉ Bông Ấm Áp", 320000.0, 50, "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=500&q=80", "Hoodie form rộng (oversize), mũ to dày dặn.", ao.getId()),
    createProduct("Áo Polo Nam Cổ Bẻ", 220000.0, 50, "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=500&q=80", "Chất vải cá sấu co giãn 4 chiều.", ao.getId()),
    createProduct("Áo Len Cổ Lọ Nữ", 280000.0, 50, "https://images.unsplash.com/photo-1608667508764-33cf0726b13a?w=500&q=80", "Áo len tăm ôm body tôn dáng.", ao.getId()),
    createProduct("Áo Blazer Nữ Khoác Ngoài", 550000.0, 50, "https://images.unsplash.com/photo-1596783046920-9e11fe09e51c?w=500&q=80", "Blazer form suông đứng form.", ao.getId()),

    // ---- QUẦN ----
    createProduct("Quần Jean Nữ Ống Rộng", 350000.0, 50, "https://images.unsplash.com/photo-1541099649-1b5ea4f114f1?w=500&q=80", "Quần jean xanh nhạt phong cách vintage.", quan.getId()),
    createProduct("Quần Tây Nam Ống Đứng", 300000.0, 50, "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=500&q=80", "Chất vai âu không nhăn không xù.", quan.getId()),
    createProduct("Quần Short Kaki Đi Biển", 180000.0, 50, "https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=500&q=80", "Short kaki mỏng nhẹ, cạp chun thoải mái.", quan.getId()),
    createProduct("Quần Jogger Thể Thao", 240000.0, 50, "https://images.unsplash.com/photo-1515562141207-7a8efb4834ba?w=500&q=80", "Quần thể thao bo chun gấu.", quan.getId()),
    createProduct("Quần Legging Nữ Lưng Cao", 120000.0, 50, "https://images.unsplash.com/photo-1506629082955-511b1aa562c8?w=500&q=80", "Chất thun ôm sát siêu co giãn.", quan.getId()),

    // ---- VÁY & ĐẦM ----
    createProduct("Đầm Hoa Nhí Mùa Hè", 380000.0, 50, "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=500&q=80", "Váy voan mỏng nhẹ bay bổng.", vay.getId()),
    createProduct("Chân Váy Chữ A", 190000.0, 50, "https://images.unsplash.com/photo-1583391733958-d15f0d08a846?w=500&q=80", "Chân váy năng động, có lớp lót.", vay.getId()),
    createProduct("Đầm Dạ Hội Trễ Vai", 850000.0, 50, "https://images.unsplash.com/photo-1566160975264-396462845ed3?w=500&q=80", "Thiết kế cúp ngực sang chảnh.", vay.getId()),
    createProduct("Váy Yếm Xòe Dễ Thương", 270000.0, 50, "https://images.unsplash.com/photo-1515347619152-c07a30366eb7?w=500&q=80", "Yếm jean kết hợp áo thun dễ thương.", vay.getId()),

    // ---- PHỤ KIỆN ----
    createProduct("Giày Sneaker Trắng", 450000.0, 50, "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=500&q=80", "Đế độn 3cm êm ái, form chuẩn.", phuKien.getId()),
    createProduct("Túi Xách Kẹp Nách Da Mềm", 320000.0, 50, "https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=500&q=80", "Túi xách nữ chất da PU chống nước.", phuKien.getId()),
    createProduct("Mắt Kính Râm Gọng Vuông", 150000.0, 50, "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500&q=80", "Mắt kính chống nắng, chống tia cực tím.", phuKien.getId()),
    createProduct("Mũ Lưỡi Trai Thêu Chữ", 90000.0, 50, "https://images.unsplash.com/photo-1521369908738-10f5c7eb22f9?w=500&q=80", "Mũ kết cotton 100%, khóa kim loại.", phuKien.getId())
));
            System.out.println("Đã khởi tạo thành công 20 sản phẩm thời trang!");
        }

        if (userRepository.count() == 1) {
            userRepository.saveAll(Arrays.asList(
                // 1 Admin (có thể vừa có quyền ADMIN vừa có quyền USER)
                createUser("admin", "123456", "Admin", "admin@fake.com", "0987654321", "Hà Nội", Set.of("ROLE_ADMIN", "ROLE_USER"), true),
                
                // 5 User hoạt động bình thường
                createUser("nguyenvana", "123456", "Nguyễn Văn A", "nva@fake.com", "0912345678", "Hồ Chí Minh", Set.of("ROLE_USER"), true),
                createUser("tranthib", "123456", "Trần Thị B", "ttb@fake.com", "0923456789", "Đà Nẵng", Set.of("ROLE_USER"), true),
                createUser("leminhc", "123456", "Lê Minh C", "lmc@fake.com", "0934567890", "Cần Thơ", Set.of("ROLE_USER"), true),
                createUser("phamvand", "123456", "Phạm Văn D", "pvd@fake.com", "0945678901", "Hải Phòng", Set.of("ROLE_USER"), true),
                createUser("hoangthie", "123456", "Hoàng Thị E", "hte@fake.com", "0956789012", "Nha Trang", Set.of("ROLE_USER"), true),
                
                // 2 User bị khóa (active = false)
                createUser("baduser1", "123456", "Kẻ Gian Lận 1", "bad1@fake.com", "0967890123", "Bình Dương", Set.of("ROLE_USER"), false),
                createUser("baduser2", "123456", "Kẻ Gian Lận 2", "bad2@fake.com", "0978901234", "Đồng Nai", Set.of("ROLE_USER"), false)
            ));
            System.out.println("Đã khởi tạo thành công 8 User mẫu vào MongoDB!");
        }
        if (orderRepository.count() == 1) {
            // Lấy danh sách user và product từ Database lên để lấy ID thật
            List<User> users = userRepository.findAll();
            List<Product> products = productRepository.findAll();

            // Chỉ tạo đơn hàng khi có đủ dữ liệu user và product
            if (users.size() >= 2 && products.size() >= 4) {
                
                User user1 = users.get(1); // nguyenvana
                User user2 = users.get(2); // tranthib
                User user3 = users.get(3); // leminhc

                Product p1 = products.get(0); // Áo thun
                Product p2 = products.get(1); // Áo sơ mi
                Product p3 = products.get(4); // Quần Jean
                Product p4 = products.get(7); // Giày sneaker

                orderRepository.saveAll(Arrays.asList(
                    // Đơn hàng 1: Hoàn thành cách đây 5 ngày
                    createOrder(user1, Arrays.asList(createOrderDetail(p1, 2), createOrderDetail(p3, 1)), "COMPLETED", 5),
                    
                    // Đơn hàng 2: Đang giao hàng cách đây 2 ngày
                    createOrder(user2, Arrays.asList(createOrderDetail(p4, 1)), "SHIPPING", 2),
                    
                    // Đơn hàng 3: Đã hủy cách đây 10 ngày
                    createOrder(user3, Arrays.asList(createOrderDetail(p2, 3)), "CANCELLED", 10),
                    
                    // Đơn hàng 4: Chờ xử lý (mới đặt hôm nay)
                    createOrder(user1, Arrays.asList(createOrderDetail(p4, 2), createOrderDetail(p1, 1)), "PENDING", 0),
                    
                    // Đơn hàng 5: Hoàn thành cách đây 15 ngày
                    createOrder(user2, Arrays.asList(createOrderDetail(p3, 2), createOrderDetail(p2, 1)), "COMPLETED", 15),

                    // Đơn hàng 6: Chờ xử lý (mới đặt hôm qua)
                    createOrder(user3, Arrays.asList(createOrderDetail(p1, 5)), "PENDING", 1),

                    // Đơn hàng 7: Đang giao
                    createOrder(user1, Arrays.asList(createOrderDetail(p3, 1), createOrderDetail(p4, 1)), "SHIPPING", 1),

                    // Đơn hàng 8: Đã hủy
                    createOrder(user2, Arrays.asList(createOrderDetail(p2, 2)), "CANCELLED", 3)
                ));

                System.out.println("Đã khởi tạo thành công 8 Đơn hàng mẫu!");
            }
    }
    }
}