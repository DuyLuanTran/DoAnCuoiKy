package com.example.shoptech.service;

import com.example.shoptech.entity.*;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;

/** The same definition of a completed sale is used for ranking and displayed quantities. */
final class ProductSalesCriteria {
    private ProductSalesCriteria() {}

    static Predicate eligible(CommonAbstractCriteria query, CriteriaBuilder cb, Root<OrderDetail> line) {
        Path<ShopOrder> shipment=line.get("shopOrder");
        Subquery<BigDecimal> historicTotal=query.subquery(BigDecimal.class);
        Root<OrderDetail> historicLine=historicTotal.from(OrderDetail.class);
        historicTotal.select(cb.sum(historicLine.<BigDecimal>get("subtotal")))
                .where(cb.equal(historicLine.get("shopOrder").get("id"),shipment.get("id")));
        return cb.and(
                cb.equal(shipment.get("status"),OrderStatus.COMPLETED),
                cb.isFalse(line.get("order").get("isDeleted")),
                cb.or(cb.isFalse(shipment.get("historicalImport")),cb.equal(shipment.get("subtotal"),historicTotal)));
    }

    static Subquery<Long> sold(CommonAbstractCriteria query, CriteriaBuilder cb, Root<Product> product) {
        Subquery<Long> result=query.subquery(Long.class);
        Root<OrderDetail> line=result.from(OrderDetail.class);
        result.select(cb.coalesce(cb.sumAsLong(line.get("quantity")),0L)).where(
                cb.equal(line.get("productId"),product.get("id")),
                cb.equal(line.get("shopOrder").get("shop").get("id"),product.get("shop").get("id")),
                eligible(result,cb,line));
        return result;
    }
}
