package com.example.msaapporder.controller;

import com.example.msaapporder.dto.OrderDto;
import com.example.msaapporder.jpa.OrderEntity;
import com.example.msaapporder.messagequeue.KafkaProducer;
import com.example.msaapporder.messagequeue.OrderProducer;
import com.example.msaapporder.service.OrderService;
import com.example.msaapporder.vo.RequestOrder;
import com.example.msaapporder.vo.ResponseOrder;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    Environment env;
    OrderService orderService;
    KafkaProducer kafkaProducer;
    OrderProducer orderProducer;

    public OrderController(Environment env, OrderService orderService, KafkaProducer kafkaProducer, OrderProducer orderProducer) {
        this.env = env;
        this.orderService = orderService;
        this.kafkaProducer = kafkaProducer;
        this.orderProducer = orderProducer;
    }

    @GetMapping("/status")
    public String status() {
        return String.format("msa-app-catalog > port %s", env.getProperty("local.server.port"));
    }

    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                                     @RequestBody RequestOrder requestOrder) {
        log.info("Before add orders data");

        ModelMapper mm = new ModelMapper();
        mm.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mm.map(requestOrder, OrderDto.class);
        orderDto.setUserId(userId);

        // jpa -> 다중인스턴스로 운용 시 인베이디드 h2 내 처리 분기로 일관성 떨어짐
//        OrderDto createdOrder = orderService.createOrder(orderDto);
//        ResponseOrder responseOrder = mm.map(createdOrder, ResponseOrder.class);

        // kafka 처리 변경
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(requestOrder.getQty() * requestOrder.getUnitPrice());

        // send this order to the kafka
        kafkaProducer.send("example-catalog-topic", orderDto);
        orderProducer.send("my-msa-order-1", orderDto);

        ResponseOrder responseOrder = mm.map(orderDto, ResponseOrder.class);

        log.info("After added orders data");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) {
        log.info("Before retrieve orders data");
        Iterable<OrderEntity> orderList = orderService.getOrdersByUserId(userId);

        List<ResponseOrder> result = new ArrayList<>();
        orderList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseOrder.class));
        });

        log.info("After retrieve orders data");
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
