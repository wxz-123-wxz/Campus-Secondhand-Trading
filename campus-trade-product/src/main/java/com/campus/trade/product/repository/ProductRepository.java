package com.campus.trade.product.repository;

import com.campus.trade.common.enums.ProductStatus;
import com.campus.trade.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :kw, '%')))")
    Page<Product> searchByKeyword(@Param("kw") String keyword,
                                  @Param("status") ProductStatus status,
                                  Pageable pageable);

    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Product> findAllByOrderByCreatedAtDesc();
}
