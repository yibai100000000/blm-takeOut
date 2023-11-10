package com.sky.service;


import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;

public interface OrderSubmitService {



    /**
     * 用户下单接口
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO ordersSubmit(OrdersSubmitDTO ordersSubmitDTO);
}
