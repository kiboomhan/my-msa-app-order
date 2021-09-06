package com.example.msaapporder.service;

import com.example.msaapporder.dto.OrderDto;
import com.example.msaapporder.jpa.OrderEntity;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDetails);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
}
