package com.pda.trading_service.controller;

import com.pda.common_service.response.ApiResponse;
import com.pda.common_service.response.ResponseMessage;
import com.pda.trading_service.controller.dto.OrderReqDto.OrderCreateReqDto;
import com.pda.trading_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<String>> createOrder(@RequestBody OrderCreateReqDto orderCreateReqDto) {
        Long userId = 1L;
        orderService.createOrder(userId, orderCreateReqDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ResponseMessage.ORDER_CREATE_SUCCESS.getCode(),
                        ResponseMessage.ORDER_CREATE_SUCCESS.getMessage()));

    }

}
