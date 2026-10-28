package com.example.shoptech;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.ShopRepository;
public final class TestShops {
    private TestShops(){}
    public static Shop active(ShopRepository shops,User owner,String slug){Shop s=new Shop();s.setOwner(owner);s.setName("Test shop "+slug);s.setSlug(slug);s.setEmail(owner.getEmail());s.setPhone("0900000000");s.setAddress("Test address");s.setStatus(ShopStatus.ACTIVE);s.setCommissionRate(java.math.BigDecimal.ZERO);return shops.saveAndFlush(s);}
}
