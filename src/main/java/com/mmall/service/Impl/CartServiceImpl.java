package com.mmall.service.Impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by lcy on 2017/12/25.
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
        if (count == null || productId == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdAndProductId(userId, productId);
        if (cart == null) {
            //没有拥有该商品的购物车，需要新建一个购物车
            Cart cartNew = new Cart();
            cartNew.setChecked(Const.Cart.CHECKED);
            cartNew.setProductId(productId);
            cartNew.setQuantity(count);
            cartNew.setUserId(userId);
            cartMapper.insert(cartNew);
        } else {
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return ServerResponse.createBySuccess(getCartVo(userId));
    }

    @Override
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        if (count == null || productId == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdAndProductId(userId, productId);
        if (cart == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cart.setQuantity(count);
        cartMapper.updateByPrimaryKey(cart);
        return ServerResponse.createBySuccess(getCartVo(userId));
    }

    public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds) {
        if (StringUtils.isBlank(productIds)) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        int rowCount = cartMapper.deleteCartProductByUserIdAndProductIds(userId, productIdList);
        if (rowCount == 0) {
            return ServerResponse.createByError("删除购物车失败");
        }
        return ServerResponse.createBySuccess(getCartVo(userId));
    }

    @Override
    public ServerResponse<Integer> getCartProductCount(Integer userId) {
        if (userId == null) {
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductNumByUserId(userId));
    }

    @Override
    public ServerResponse<CartVo> checkedOrUncheckedSelect(Integer userId, Integer productId, Integer checked) {
        cartMapper.checkedOrUncheckedProduct(userId, productId, checked);
        return this.list(userId);
    }


    public ServerResponse<CartVo> list(Integer userId) {
        return ServerResponse.createBySuccess(getCartVo(userId));
    }

    private CartVo getCartVo(Integer userId) {
        //返回的list不会为null？
        List<Cart> cartList = cartMapper.selectCartListByUserId(userId);
        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        CartVo cartVo = new CartVo();
        BigDecimal totalPrice = new BigDecimal("0.0");
        if (CollectionUtils.isNotEmpty(cartList)) {
            for (Cart cartItem : cartList) {
                CartProductVo cpv = new CartProductVo();
                cpv.setId(cartItem.getId());
                cpv.setUserId(userId);
                cpv.setProductId(cartItem.getProductId());
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if (product != null) {
                    cpv.setProductName(product.getName());
                    cpv.setProductChecked(cartItem.getChecked());
                    cpv.setProductMainImage(product.getMainImage());
                    cpv.setProductPrice(product.getPrice());
                    cpv.setProductStatus(product.getStatus());
                    cpv.setProductStock(product.getStock());
                    cpv.setProductSubtitle(product.getSubtitle());
                    //判断库存
                    int buyLimitNuber = 0;
                    if (product.getStock() >= cartItem.getQuantity()) {
                        buyLimitNuber = cartItem.getQuantity();
                        cpv.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                        cpv.setQuantity(buyLimitNuber);
                    } else {
                        buyLimitNuber = product.getStock();
                        cpv.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        cpv.setQuantity(buyLimitNuber);
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitNuber);
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }
                    cpv.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), buyLimitNuber));
                }
                cartProductVoList.add(cpv);

                if (cartItem.getChecked() == Const.Cart.CHECKED) {
                    totalPrice = BigDecimalUtil.add(totalPrice.doubleValue(), cpv.getProductTotalPrice().doubleValue());
                }
            }

        }
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setCartTotalPrice(totalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        return cartVo;
    }

    private boolean getAllCheckedStatus(Integer userId) {
        if (userId == null) {
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;

    }
}
