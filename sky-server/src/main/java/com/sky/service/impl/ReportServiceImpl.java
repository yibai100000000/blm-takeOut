package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService;


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


    @Override
    public void exportBusinessData(HttpServletResponse httpServletResponse){
        //查询数据库，获得营业数据--最近30天
        LocalDate begin=LocalDate.now().minusDays(30);
        LocalDate end=LocalDate.now().minusDays(1);

        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);

        //通过POI将数据导入到excel文件中
        //导入resource中的模板文件为输入流
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/excelTemplate.xlsx");

        try(XSSFWorkbook excel=new XSSFWorkbook(inputStream);
            ServletOutputStream out = httpServletResponse.getOutputStream()
        ){
            //基于模板文件创建一个新的excel
            //填充数据
            XSSFSheet sheet=excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间："+begin+"至："+end);

            XSSFRow row=sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row=sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());


            for(int i=0;i<30;i++){
                LocalDate date=LocalDate.now();

                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row=sheet.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //通过输出流将excel文件下载到客户端浏览器
            excel.write(out);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
