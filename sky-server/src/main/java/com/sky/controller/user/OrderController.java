package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("订单提交接口")
    public Result<OrderSubmitVO> submitOrderHandle(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单数据：{}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO= orderService.ordersSubmit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单接口")
    public Result orderAgain(@PathVariable Long id){
        orderService.orderAgain(id);
        return Result.success();
    }

    @GetMapping("/history/Orders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> historyPayment(int page, int pageSize, Integer status){
        PageResult pageResult=orderService.pageQuery(page,pageSize,status);

        return Result.success(pageResult);

    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> paymentDetail(@PathVariable Long id){
        OrderVO orderVO=orderService.detail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancelOrder(@PathVariable Long id){
        //模拟取消订单
        log.info("模拟取消订单：{}",id);
        orderService.cancel(id);
        log.info("模拟取消订单完成");
        return Result.success();
    }

    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);


        //对交易成功进行模拟,修改数据库订单状态
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("模拟交易成功：{}",ordersPaymentDTO.getOrderNumber());

        return Result.success(orderPaymentVO);
    }



}
