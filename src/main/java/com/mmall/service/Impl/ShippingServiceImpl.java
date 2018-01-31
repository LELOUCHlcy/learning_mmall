package com.mmall.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by lcy on 2017/12/26.
 */
@Service("IShippingService")
public class ShippingServiceImpl implements IShippingService {
    @Autowired
    private ShippingMapper shippingMapper;

    @Override
    public ServerResponse<Map> add(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if (rowCount == 0) {
            return ServerResponse.createByError("添加地址错误");
        }

        Map result = Maps.newHashMap();
        result.put("id",shipping.getId());
        return ServerResponse.createBySuccess("添加地址成功", result);
    }

    @Override
    public ServerResponse del(Integer userId, Integer shippingId) {
        if (userId == null || shippingId == null) {
            return ServerResponse.createByError("删除地址参数错误");
        }
        int rowCount = shippingMapper.deleteByUserIdAndShippingId(userId, shippingId);
        if (rowCount == 0) {
            return ServerResponse.createByError("删除地址失败");
        }
        return ServerResponse.createBySuccess("删除地址成功");
    }
    @Override
    public ServerResponse update(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByUserIdAndShippingId(shipping);
        if (rowCount == 0) {
            return ServerResponse.createByError("更新地址失败");
        }
        return ServerResponse.createBySuccess("更新地址成功");
    }
    @Override
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {
        if (userId == null || shippingId == null) {
            return ServerResponse.createByError("获取地址详情参数错误");
        }
        Shipping shipping = shippingMapper.selectByUserIdAndShippingId(userId, shippingId);
        if (shipping == null) {
            return ServerResponse.createByError("获取地址详情出错");
        }
        return ServerResponse.createBySuccess("获取地址详请成功", shipping);
    }
    @Override
    public ServerResponse<PageInfo> list(Integer userId, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);
        List<Shipping> shippings = shippingMapper.selectByUserId(userId);
        PageInfo<Shipping> pageInfo = new PageInfo<Shipping>(shippings);

        return ServerResponse.createBySuccess(pageInfo);
    }

}
