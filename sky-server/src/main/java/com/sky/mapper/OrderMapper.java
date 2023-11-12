package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据并返回订单id
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders selectById(Long id);

    /**
     * 查询所有订单的状态
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer selectStatus(Integer status);

    /**
     * 查询超时订单
     * @param unPaid
     * @param outTime
     * @return
     */
    @Select("SELECT * from orders where status=#{unPaid} and order_time<#{outTime}")
    List<Orders> selectByTimeAndStatus(Integer unPaid, LocalDateTime outTime);

    /**
     * 条件查询每日营业额
     * @param map
     * @return
     */
    Double getTurnoverSum(Map map);

    /**
     * 条件查询订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计指定时间内的销量top10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop(LocalDateTime begin,LocalDateTime end);
}
