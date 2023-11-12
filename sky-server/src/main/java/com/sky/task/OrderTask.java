package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron="0 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("正在处理超时订单");
        LocalDateTime outTime=LocalDateTime.now().plusMinutes(-15);

        List<Orders> list=orderMapper.selectByTimeAndStatus(Orders.PENDING_PAYMENT,outTime);


        if (list!=null && list.size()>0) {
            for(Orders orders:list){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason("超时未下单");

                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理长时间派送中订单
     */
    @Scheduled(cron="0 0 1 * * ? ")
    public void processDeliveryOrder(){
        log.info("正在处理长时间派送中订单");

        LocalDateTime outTime=LocalDateTime.now().plusMinutes(-60);

        List<Orders> list=orderMapper.selectByTimeAndStatus(Orders.DELIVERY_IN_PROGRESS,outTime);


        if (list!=null && list.size()>0) {
            for(Orders orders:list){
                orders.setStatus(Orders.COMPLETED);

                orderMapper.update(orders);
            }
        }
    }
}
