package com.campus.trade.payment.feign;

import com.campus.trade.common.dto.OrderDTO;
import com.campus.trade.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "campus-trade-order", path = "/api/orders")
public interface OrderFeignClient {

    @GetMapping("/by-no")
    Result<OrderDTO> getByOrderNo(@RequestParam("orderNo") String orderNo);
}
