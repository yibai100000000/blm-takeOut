package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrdersDetailMapper ordersDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;



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

        orderMapper.insert(orders);
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


    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//      生成空json，跳过微信支付流程

        JSONObject jsonObject=new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    @Override
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void cancel(Long id) {

        Orders orderC=orderMapper.selectById(id);

        if(orderC==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orderC.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }



        Orders orders = Orders.builder()
                .id(orderC.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();

        if (orderC.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            log.info("用户自己取消订单退款");
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
    }

    @Override
    public PageResult pageQuery(int page1, int pageSize, Integer status) {
        OrdersPageQueryDTO ordersPageQueryDTO=new OrdersPageQueryDTO();
        ordersPageQueryDTO.setPage(page1);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());


        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page=orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list=new ArrayList<>();

        for(Orders o:page){
            OrderVO orderVO=new OrderVO();
            List<OrderDetail> details=ordersDetailMapper.getByOrderId(o.getId());
            BeanUtils.copyProperties(o,orderVO);
            orderVO.setOrderDetailList(details);
            list.add(orderVO);
        }

        PageResult pageResult=new PageResult(page.getTotal(),list);

        return pageResult;
    }

    @Override
    public OrderVO detail(Long id) {
        Orders orders=orderMapper.selectById(id);
        List<OrderDetail> list=ordersDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO=new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    @Override
    public void orderAgain(Long id) {
        //查询当前用户id
        Long userId=BaseContext.getCurrentId();

        //获取订单详情
        List<OrderDetail> orderDetails=ordersDetailMapper.getByOrderId(id);

//        //创建购物车列表
//        List<ShoppingCart> shoppingCarts=new ArrayList<>();
//        for(OrderDetail od:orderDetails){
//            ShoppingCart sc=new ShoppingCart();
//            BeanUtils.copyProperties(od,sc);
//            sc.setCreateTime(LocalDateTime.now());
//            shoppingCarts.add(sc);
//        }
        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            //为了保证购物车的id正确，在赋值时忽略订单详情中的id
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        //批量插入新购物车数据
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    @Override
    public PageResult searchPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page=orderMapper.pageQuery(ordersPageQueryDTO);
        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page){
        List<OrderVO> res=new ArrayList<>();
        for(Orders o:page){
            OrderVO orderVO=new OrderVO();
            BeanUtils.copyProperties(o,orderVO);
            String dishesStr=getStr(o);
            orderVO.setOrderDishes(dishesStr);
            res.add(orderVO);
        }
        return res;
    }

    private String getStr(Orders o) {
        List<OrderDetail> list=ordersDetailMapper.getByOrderId(o.getId());

        //为了使泛型保持到String然后使用String对象的方法使用新list对象
        //流修改旧的列表本身就会发生变化
        List<String> res=list.stream().map(x -> {
            String dish=x.getName()+"*"+x.getNumber()+";";
            return dish;
        }).collect(Collectors.toList());

        //拼接字符列表
        return String.join("",res);
    }

    @Override
    public OrderStatisticsVO statistics() {

        //使用数据库聚合查询优化代码
        Integer c=orderMapper.selectStatus(Orders.CONFIRMED);
        Integer d=orderMapper.selectStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer t=orderMapper.selectStatus(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO orderStatisticsVO=new OrderStatisticsVO(t,c,d);
        return orderStatisticsVO;
    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders=orderMapper.selectById(ordersCancelDTO.getId());

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orderC=new Orders();
        orderC.setStatus(Orders.CANCELLED);
        orderC.setId(orderC.getId());
        orderC.setCancelReason(ordersCancelDTO.getCancelReason());
        orderC.setCancelTime(LocalDateTime.now());
        orderMapper.update(orderC);
    }

    @Override
    //TODO id丢失
    public void delivery(Long id) {


        //查询原订单
        Orders ordersDB = orderMapper.selectById(id);

        // 校验订单是否存在，并且状态为3
        //这里比较的是Integer，为了避免常量池影响结果，使用.equals()
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders=new Orders();
        orders.setId(orders.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        //这里更新并不会影响原数据，而且优化了sql语句
        orderMapper.update(orders);

    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.selectById(id);

        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {

        // 根据id查询订单
        Orders ordersDB = orderMapper.selectById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            log.info("商家拒单给用户退款");
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .build();

        orderMapper.update(orders);
    }
}
