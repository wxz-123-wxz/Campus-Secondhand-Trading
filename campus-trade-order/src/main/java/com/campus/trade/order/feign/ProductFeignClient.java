package com.campus.trade.order.feign;

import com.campus.trade.common.dto.ProductDTO;
import com.campus.trade.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "campus-trade-product", path = "/api/products")
public interface ProductFeignClient {

    @GetMapping("/{id}")
    Result<ProductDTO> getProduct(@PathVariable("id") Long id);
}
