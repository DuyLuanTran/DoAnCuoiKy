package com.example.shoptech.controller;

import com.example.shoptech.dto.CatalogFilters;
import com.example.shoptech.dto.PublicShop;
import com.example.shoptech.entity.Product;
import com.example.shoptech.service.CategoryService;
import com.example.shoptech.service.ProductCatalogService;
import com.example.shoptech.service.PublicShopService;
import com.example.shoptech.service.ProductSalesService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Controller
public class ProductCatalogController {
    private static final Set<String> SORT_OPTIONS=Set.of("newest","priceAsc","priceDesc","nameAsc","bestSelling");
    private final ProductCatalogService products;
    private final CategoryService categories;
    private final PublicShopService shops;
    private final ProductSalesService sales;
    public ProductCatalogController(ProductCatalogService products,CategoryService categories,PublicShopService shops,ProductSalesService sales){this.products=products;this.categories=categories;this.shops=shops;this.sales=sales;}

    @GetMapping("/products")
    public String list(@ModelAttribute CatalogFilters filters,Model model){return catalog(filters,null,model);}

    @GetMapping("/shops/{shopSlug}")
    public String shop(@PathVariable String shopSlug,@ModelAttribute CatalogFilters filters,Model model){
        // The route fixes the shop. A forged shopId query cannot broaden this storefront.
        return catalog(filters,shops.bySlug(shopSlug),model);
    }

    private String catalog(CatalogFilters f,PublicShop storefront,Model model){
        String keyword=f.getKeyword()==null?"":f.getKeyword().strip();
        if(keyword.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 100 ký tự.");
        String sort=f.getSort()!=null&&SORT_OPTIONS.contains(f.getSort())?f.getSort():"newest";
        int size=Set.of(12,20,24,48).contains(f.getSize())?f.getSize():12;
        Long shopId=storefront==null?f.getShopId():storefront.id();
        PublicShop selected=storefront!=null?storefront:shopId==null?null:shops.byId(shopId);
        Page<Product> result;
        try{result=products.search(keyword,f.getCategoryId(),f.getMinPrice(),f.getMaxPrice(),f.isInStock(),f.isOnSale(),sort,shopId,f.getMinSold(),PageRequest.of(Math.max(0,f.getPage()),size));}
        catch(IllegalArgumentException e){model.addAttribute("filterError",e.getMessage());result=Page.empty(PageRequest.of(0,size));}
        model.addAttribute("minSold",f.getMinSold());model.addAttribute("soldCounts",sales.counts(result.getContent().stream().map(Product::getId).toList()));
        model.addAttribute("products",result);model.addAttribute("categories",categories.getAvailableCategories());
        model.addAttribute("shopOptions",shops.options());model.addAttribute("storeShop",storefront);model.addAttribute("selectedShop",selected);
        model.addAttribute("shopId",shopId);model.addAttribute("catalogBase",storefront==null?"/products":"/shops/"+storefront.slug());
        model.addAttribute("keyword",keyword);model.addAttribute("categoryId",f.getCategoryId());model.addAttribute("minPrice",f.getMinPrice());model.addAttribute("maxPrice",f.getMaxPrice());
        model.addAttribute("inStock",f.isInStock());model.addAttribute("onSale",f.isOnSale());model.addAttribute("sort",sort);model.addAttribute("pageSize",size);
        if(storefront!=null)model.addAttribute("shopProductCount",products.countVisibleForShop(storefront.id()));
        return "user/store";
    }

    @GetMapping("/products/{slug}")
    public String detail(@PathVariable String slug,Model model){
        Product product=products.findBySlug(slug).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm."));
        model.addAttribute("product",product);model.addAttribute("categories",categories.getAvailableCategories());
        model.addAttribute("catalogBase","/products");model.addAttribute("keyword","");model.addAttribute("categoryId",product.getCategory().getId());
        model.addAttribute("minPrice",null);model.addAttribute("maxPrice",null);model.addAttribute("inStock",false);model.addAttribute("onSale",false);model.addAttribute("sort","newest");model.addAttribute("pageSize",12);
        return "user/product";
    }
}
