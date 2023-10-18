package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorsMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorsMapper dishFlavorsMapper;


    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //添加一个菜品
        dishMapper.insert(dish);

        //获取DishId，因为DishId是数据表自动生成的
        Long Id=dish.getId();


        //添加n个口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors.size()>0 && flavors!=null){

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(Id);
            });

            dishFlavorsMapper.insertBatch(flavors);

        }


    }
}
