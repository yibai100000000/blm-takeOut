package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;


    /**
     * 提取时间段并转化为列表
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getTimeList(LocalDate begin, LocalDate end){
        List<LocalDate> timeList=new ArrayList<>();
        while(!begin.equals(end)){
            timeList.add(begin);
            begin=begin.plusDays(1);
        }
        timeList.add(end);
        return timeList;
    }


    @Override
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        List<LocalDate> timeList=getTimeList(begin,end);

        List<Double> turnoverList=new ArrayList<>();
        Map map=new HashMap();
        for (LocalDate localDate:timeList){
            LocalDateTime b = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(localDate, LocalTime.MAX);
            map.put("begin",b);
            map.put("end",e);
            map.put("status",Orders.COMPLETED);
            Double turnover=orderMapper.getTurnoverSum(map);
            turnover=turnover==null?0.0:turnover;
            turnoverList.add(turnover);
        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(timeList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }


    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> timeList=getTimeList(begin,end);
        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();
        Map map=new HashMap();

        //计算第一天的用户总量
        map.put("end",begin);
        Integer totalUser=userMapper.getUserNum(map);
        totalUser=totalUser==null?0:totalUser;

        Integer addUser=0;

        for (LocalDate localDate:timeList){
            LocalDateTime b = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(localDate, LocalTime.MAX);

            totalUserList.add(totalUser);

            map.put("begin",b);
            map.put("end",e);

            addUser+=userMapper.getUserNum(map);
            addUser=addUser==null?0:addUser;

            newUserList.add(addUser);

            totalUser+=addUser;
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(timeList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();

    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> timeList=getTimeList(begin,end);

        List<Integer> orderCountList=new ArrayList<>();
        List<Integer> validOrderCountList=new ArrayList<>();

        Map map=new HashMap();
        for (LocalDate localDate:timeList){
            LocalDateTime b = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(localDate, LocalTime.MAX);
            map.put("begin",b);
            map.put("end",e);
            if(map.containsKey("status"))map.remove("status");
            //查询每天总订单数
            Integer orderCount= orderMapper.countByMap(map);
            orderCount=orderCount==null?0:orderCount;
            //查询每天有效订单数
            map.put("status",Orders.COMPLETED);
            Integer validOrderCount=orderMapper.countByMap(map);
            validOrderCount=validOrderCount==null?0:validOrderCount;

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }
        //订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //有效订单总数
        Integer totalValidOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单成功率
        Double orderCompleteRate=0D;
        if (totalValidOrderCount!=0){
            orderCompleteRate=totalOrderCount.doubleValue()/totalValidOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(timeList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(orderCompleteRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10Statistics(LocalDate begin, LocalDate end) {
        LocalDateTime b=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime e=LocalDateTime.of(end,LocalTime.MAX);

        List<GoodsSalesDTO> list=orderMapper.getSalesTop(b,e);

        List<Integer> numberList=list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        List<String> nameList=list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());

        log.info("number:{}",numberList);

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }
}
