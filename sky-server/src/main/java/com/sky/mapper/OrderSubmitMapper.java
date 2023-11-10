package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderSubmitMapper {

    /**
     * 插入订单数据并返回订单id
     * @param orders
     */
    void insert(Orders orders);
}
