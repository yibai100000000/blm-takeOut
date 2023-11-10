package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderSubmitMapper;
import com.sky.mapper.OrdersDetailMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderSubmitService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderSubmitServiceImpl implements OrderSubmitService {

    @Autowired
    private OrderSubmitMapper orderSubmitMapper;

    @Autowired
    private OrdersDetailMapper ordersDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;


    @Override
    @Transactional
    public OrderSubmitVO ordersSubmit(OrdersSubmitDTO ordersSubmitDTO) {
        //进行异常处理
        //地址簿为空，购物车为空
        AddressBook addressBook=addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId=BaseContext.getCurrentId();
        ShoppingCart shoppingCart=new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list=shoppingCartMapper.list(shoppingCart);
        if(list==null || list.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //向订单表插入一条数据
        Orders orders=new Orders();

        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setPhone(addressBook.getPhone());
        orders.setStatus(Orders.UN_PAID);
        orders.setPayStatus(Orders.PENDING_PAYMENT);
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        orderSubmitMapper.insert(orders);
        //向订单明细表插入n条数据
        Long orderId=orders.getId();

        List<OrderDetail> orderList=new ArrayList<>();
        for (ShoppingCart sc:list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(sc,orderDetail);
            orderDetail.setOrderId(orderId);
            orderList.add(orderDetail);
        }

        ordersDetailMapper.insertBatch(orderList);
        //删除购物车表中的数据
        shoppingCartMapper.clean(userId);

        //返回VO对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orderId)
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }
}
