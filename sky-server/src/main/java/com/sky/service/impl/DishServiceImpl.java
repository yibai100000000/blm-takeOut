package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorsMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorsMapper dishFlavorsMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品
     */
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


    /**
     * 分页查询菜品
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);

        PageResult pageResult=new PageResult(page.getTotal(), page.getResult());

        return pageResult;
    }

    /**
     * 批量删除菜品
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能删除--是否存在起售中的菜品
        for (Long id:ids) {
            Dish dish=dishMapper.selectById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                //菜品起售中,不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能删除--是否关联套餐
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        log.info(setmealIds.toString());
        if(setmealIds!=null && setmealIds.size()>0){
            throw  new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
//        //删除菜品表中的菜品
//        //删除菜品关联的口味数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            dishFlavorsMapper.deleteByDishId(id);
//        }

        //批量删除优化
        dishMapper.deleteByIds(ids);
        dishFlavorsMapper.deleteByDishIds(ids);

    }


    /**
     * 根据id查询菜品与口味
     */
    @Override
    public DishVO findDishById(Integer id) {
        DishVO dishVO=dishMapper.selectDishAndFlavorById(id);
        if(dishVO==null){
            throw new BaseException("id不存在");
        }
        List<DishFlavor> flavors=dishFlavorsMapper.selectByDishId(dishVO.getId());
        log.info(flavors.toString());
        dishVO.setFlavors(flavors);
        log.info(dishVO.toString());
        return dishVO;
    }


    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    public void updateDish(DishDTO dishDTO) {
        //可以使用dish对象来修改基本信息
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //修改dish表的基本信息
        dishMapper.update(dish);
        //删除菜品的口味信息
        dishFlavorsMapper.deleteByDishId(dish.getId());
        //添加菜品的口味信息
        List<DishFlavor> flavors=dishDTO.getFlavors();
        if(flavors.size()>0 && flavors!=null){

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });

            dishFlavorsMapper.insertBatch(flavors);

        }

    }


    /**
     * 修改菜品起售状态
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        Dish dish=new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }


    /**
     * 根据分类ID查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public List<Dish> findDishByCategoryID(DishPageQueryDTO dishPageQueryDTO) {
        List<Dish>list=dishMapper.selectByCategoryID(dishPageQueryDTO.getCategoryId());
        return list;
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        long id=dish.getCategoryId();
        Integer cid=(int) id;

        List<Dish> dishList = dishMapper.selectByCategoryID(cid);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorsMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
