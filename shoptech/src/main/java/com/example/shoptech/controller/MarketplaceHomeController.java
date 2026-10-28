package com.example.shoptech.controller;

import com.example.shoptech.entity.Product;
import com.example.shoptech.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class MarketplaceHomeController {
    private final ProductCatalogService catalog;
    private final ProductSalesService sales;
    private final PublicShopService shops;
    private final CategoryService categories;
    public MarketplaceHomeController(ProductCatalogService catalog,ProductSalesService sales,PublicShopService shops,CategoryService categories){this.catalog=catalog;this.sales=sales;this.shops=shops;this.categories=categories;}

    @GetMapping("/")
    public String home(@RequestParam(defaultValue="newest")String feed,@RequestParam(defaultValue="0")int page,Model model){
        String selected=Set.of("newest","bestSelling").contains(feed)?feed:"newest";
        var products=catalog.search("",null,null,null,false,false,selected,null,selected.equals("bestSelling")?1:0,PageRequest.of(Math.max(0,page),20));
        var popular=catalog.search("",null,null,null,false,false,"bestSelling",null,11,PageRequest.of(0,4));
        var recent=catalog.search("",null,PageRequest.of(0,20));
        LinkedHashMap<Long,Product> promotions=new LinkedHashMap<>();
        recent.getContent().forEach(p->promotions.putIfAbsent(p.getCategory().getId(),p));
        var shopPage=shops.search("",0,6);
        Set<Long> ids=new HashSet<>();products.forEach(p->ids.add(p.getId()));popular.forEach(p->ids.add(p.getId()));
        model.addAttribute("categories",categories.getAvailableCategories());model.addAttribute("feed",selected);
        model.addAttribute("products",products);model.addAttribute("popularProducts",popular);model.addAttribute("promotions",promotions.values().stream().limit(3).toList());
        model.addAttribute("featuredShops",shopPage);model.addAttribute("shopCounts",catalog.visibleCountsForShops(shopPage.stream().map(s->s.id()).toList()));
        model.addAttribute("soldCounts",sales.counts(ids));return "user/index";
    }

    @GetMapping("/shops")
    public String directory(@RequestParam(defaultValue="")String keyword,@RequestParam(defaultValue="0")int page,Model model){
        var result=shops.search(keyword,page,12);
        model.addAttribute("categories",categories.getAvailableCategories());model.addAttribute("shops",result);model.addAttribute("shopKeyword",keyword.strip());
        model.addAttribute("shopCounts",catalog.visibleCountsForShops(result.stream().map(s->s.id()).toList()));return "user/shops";
    }
}
