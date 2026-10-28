package com.example.shoptech.controller;
import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.*;
import com.example.shoptech.service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.*;
@Controller @RequestMapping("/seller/products") @PreAuthorize("hasRole('SELLER')")
public class SellerProductController {
    private final ProductService products;private final CategoryService categories;private final ShopService shops;
    public SellerProductController(ProductService products,CategoryService categories,ShopService shops){this.products=products;this.categories=categories;this.shops=shops;}
    @ModelAttribute public void context(Principal user,Model model){model.addAttribute("sellerArea",true);model.addAttribute("shop",shops.requiredOwn(user.getName()));model.addAttribute("productBase","/seller/products");}
    @GetMapping public String list(Principal user,@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="false") boolean trash,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,Model model){
        model.addAttribute("products",products.searchSeller(user.getName(),keyword,categoryId,trash,PageRequest.of(Math.max(0,page),size==20||size==50?size:10,Sort.by("id").descending())));model.addAttribute("categories",categories.getAvailableCategories());model.addAttribute("keyword",keyword);model.addAttribute("categoryId",categoryId);model.addAttribute("trash",trash);return "admin/product/list";
    }
    @GetMapping("/new") public String createForm(Model model){model.addAttribute("productForm",new ProductForm());prepare(model,null);return "admin/product/form";}
    @GetMapping("/{id}/edit") public String editForm(Principal user,@PathVariable Long id,Model model){Product p=products.getSellerProduct(user.getName(),id);model.addAttribute("productForm",products.getSellerForm(user.getName(),id));prepare(model,p);return "admin/product/form";}
    @PostMapping("/new") public String create(Principal user,@Valid @ModelAttribute("productForm") ProductForm form,BindingResult errors,Model model,RedirectAttributes flash){
        if(!errors.hasErrors())try{products.createSeller(user.getName(),form);flash.addFlashAttribute("successMessage","Đã thêm sản phẩm vào shop.");return "redirect:/seller/products";}catch(IllegalArgumentException|IllegalStateException|DataIntegrityViolationException e){errors.reject("product",message(e));}
        prepare(model,null);return "admin/product/form";
    }
    @PostMapping("/{id}/edit") public String update(Principal user,@PathVariable Long id,@Valid @ModelAttribute("productForm") ProductForm form,BindingResult errors,Model model,RedirectAttributes flash){
        Product p=products.getSellerProduct(user.getName(),id);if(Boolean.TRUE.equals(p.getIsDeleted()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(!errors.hasErrors())try{products.updateSeller(user.getName(),id,form);flash.addFlashAttribute("successMessage","Đã cập nhật sản phẩm.");return "redirect:/seller/products";}catch(IllegalArgumentException|IllegalStateException|DataIntegrityViolationException e){errors.reject("product",message(e));}
        prepare(model,p);return "admin/product/form";
    }
    @PostMapping("/{id}/delete") public String trash(Principal user,@PathVariable Long id,RedirectAttributes flash){products.trashSeller(user.getName(),id);flash.addFlashAttribute("successMessage","Đã chuyển sản phẩm vào thùng rác.");return "redirect:/seller/products";}
    @PostMapping("/{id}/restore") public String restore(Principal user,@PathVariable Long id,RedirectAttributes flash){products.restoreSeller(user.getName(),id);flash.addFlashAttribute("successMessage","Đã khôi phục sản phẩm.");return "redirect:/seller/products?trash=true";}
    private String message(RuntimeException e){return e instanceof DataIntegrityViolationException?"Mã sản phẩm đã có trong shop hoặc dữ liệu vừa thay đổi. Hãy tải lại trang.":e.getMessage();}
    private void prepare(Model model,Product p){
        List<Category> available=new ArrayList<>(categories.getAvailableCategories());
        if(p!=null&&!Boolean.TRUE.equals(p.getCategory().getIsDeleted())&&available.stream().noneMatch(c->c.getId().equals(p.getCategory().getId())))available.add(p.getCategory());
        model.addAttribute("editing",p!=null);model.addAttribute("formTitle",p==null?"Thêm sản phẩm":"Cập nhật sản phẩm");model.addAttribute("formAction",p==null?"/seller/products/new":"/seller/products/"+p.getId()+"/edit");model.addAttribute("productId",p==null?null:p.getId());model.addAttribute("categories",available);model.addAttribute("currentImage",p==null?null:p.getMainImage());model.addAttribute("moderationReason",p!=null&&Boolean.TRUE.equals(p.getIsBlocked())?p.getModerationReason():null);
    }
}
