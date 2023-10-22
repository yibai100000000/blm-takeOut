package com.sky.service.impl;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void addSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.add(setmeal);

        Long id=setmeal.getId();

        List<SetmealDish> setmealDish=setmealDTO.getSetmealDishes();

        if (setmealDish!=null && setmealDish.size()>0) {
            setmealDish.forEach( sd -> {
                sd.setSetmealId(id);
                log.info(sd+"："+sd.getSetmealId());
            });

            setmealMapper.insertBatch(setmealDish);
        }
    }
}
