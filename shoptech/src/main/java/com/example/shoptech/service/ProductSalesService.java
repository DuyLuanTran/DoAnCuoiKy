package com.example.shoptech.service;

import com.example.shoptech.entity.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ProductSalesService {
    @PersistenceContext private EntityManager entityManager;

    public Map<Long,Long> counts(Collection<Long> ids) {
        if(ids.isEmpty())return Map.of();
        CriteriaBuilder cb=entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query=cb.createTupleQuery();
        Root<OrderDetail> line=query.from(OrderDetail.class);
        Root<Product> product=query.from(Product.class);
        query.multiselect(product.get("id"),cb.sumAsLong(line.get("quantity")))
                .where(product.get("id").in(ids),cb.equal(product.get("id"),line.get("productId")),
                        cb.equal(product.get("shop").get("id"),line.get("shopOrder").get("shop").get("id")),
                        ProductSalesCriteria.eligible(query,cb,line))
                .groupBy(product.get("id"));
        Map<Long,Long> result=new HashMap<>();
        ids.forEach(id->result.put(id,0L));
        entityManager.createQuery(query).getResultList().forEach(row->result.put(row.get(0,Long.class),row.get(1,Long.class)));
        return result;
    }
}
