package com.example.shoptech.controller;

import com.example.shoptech.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.*;

@Controller
public class ProductCommunityController {
    private final ProductCommunityService community;private final CategoryService categories;
    public ProductCommunityController(ProductCommunityService community,CategoryService categories){this.community=community;this.categories=categories;}
    @GetMapping("/favorites") public String favorites(Principal user,@RequestParam(defaultValue="0")int page,Model model){
        model.addAttribute("products",community.favorites(user.getName(),page));model.addAttribute("categories",categories.getAvailableCategories());return "user/favorites";
    }
    @GetMapping("/api/favorites/selected") @ResponseBody public Set<Long> selected(Principal user,@RequestParam List<Long> ids){return community.selected(user.getName(),ids);}
    @PutMapping("/api/favorites/{id}") @ResponseBody public Map<String,Boolean> add(Principal user,@PathVariable Long id){return Map.of("selected",community.favorite(user.getName(),id,true));}
    @DeleteMapping("/api/favorites/{id}") @ResponseBody public Map<String,Boolean> remove(Principal user,@PathVariable Long id){return Map.of("selected",community.favorite(user.getName(),id,false));}
    @GetMapping("/api/community/summaries") @ResponseBody public Map<Long,ProductCommunityService.Summary> summaries(@RequestParam List<Long> ids){return community.summaries(ids);}
    @GetMapping("/api/community/products/{id}/reviews") @ResponseBody public Map<String,Object> reviews(@PathVariable Long id,@RequestParam(defaultValue="0")int page){
        var result=community.publicReviews(id,page);return Map.of("items",result.getContent(),"page",result.getNumber(),"pages",result.getTotalPages(),"total",result.getTotalElements(),"summary",community.summaries(List.of(id)).getOrDefault(id,new ProductCommunityService.Summary(0,0)));
    }
    @GetMapping("/api/reviews/eligibility/{id}") @ResponseBody public ProductCommunityService.Eligibility eligibility(Principal user,@PathVariable Long id){return community.eligibility(user.getName(),id);}
    @PostMapping(value="/api/reviews/products/{id}",consumes="multipart/form-data") @ResponseBody
    public ProductCommunityService.ReviewView review(Principal user,@PathVariable Long id,@RequestParam int rating,@RequestParam String content,@RequestParam(required=false)List<MultipartFile> files){return community.create(user.getName(),id,rating,content,files);}
}
