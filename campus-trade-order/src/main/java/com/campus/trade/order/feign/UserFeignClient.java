package com.campus.trade.order.feign;

import com.campus.trade.common.dto.UserDTO;
import com.campus.trade.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "campus-trade-user", path = "/api/users")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserDTO> getUser(@PathVariable("id") Long id);
}
