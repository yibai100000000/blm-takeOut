package com.sky.controller.user;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.Result;
import com.sky.service.OrderSubmitService;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderSubmitService orderSubmitService;

    @PostMapping("/submit")
    @ApiOperation("订单提交接口")
    public Result<OrderSubmitVO> submitOrderHandle(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单数据：{}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO=orderSubmitService.ordersSubmit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

}
