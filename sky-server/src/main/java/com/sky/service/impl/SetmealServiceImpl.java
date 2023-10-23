package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
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

    /**
     * 添加套餐
     * @param setmealDTO
     */
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

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page=setmealMapper.pageQuery(setmealPageQueryDTO);
        PageResult pageResult=new PageResult(page.getTotal(),page.getResult());
        return pageResult;
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
//        setmealMapper.deleteBatch(ids);
        for (Long id:ids){
            Setmeal setmeal=setmealMapper.selectSetmealById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // TODO 删除功能
//        //判断当前套餐是否存在起售菜品
//        for (Long id:ids){
//            //查找当前套餐包含的菜品集合
//            List<Integer> setmealDishes=setmealMapper.selectSetmealOfDish(id);
//            //查找菜品集合的起售情况
//            for (Integer i:setmealDishes){
//                if (i==StatusConstant.ENABLE){
//                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
//                }
//            }
//        }


    }
}
