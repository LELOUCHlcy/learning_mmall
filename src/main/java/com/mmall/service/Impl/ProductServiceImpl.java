package com.mmall.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.ICategoryService;
import com.mmall.service.IProductService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVO;
import com.mmall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lcy on 2017/12/23.
 */
@Service("iProductService")
public class ProductServiceImpl implements IProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ICategoryService iCategoryService;

    @Override
    public ServerResponse saveOrUpdateProduct(Product product) {
        if (product != null) {
            if (StringUtils.isNotBlank(product.getSubImages())) {
                String[] subImages = product.getSubImages().split(",");
                if (subImages.length > 0) {
                    product.setMainImage(subImages[0]);
                }
            }
            if (product.getId() != null) {
                int rowCount = productMapper.updateByPrimaryKey(product);
                if (rowCount > 0) {
                    return ServerResponse.createBySuccess("更新产品信息成功");
                } else {
                    return ServerResponse.createByError("更新产品信息失败");
                }
            } else {
                int rowCount = productMapper.insert(product);
                if (rowCount > 0) {
                    return ServerResponse.createBySuccess("新增产品成功");
                } else {
                    return ServerResponse.createByError("新增产品失败");
                }
            }
        }
        return ServerResponse.createByError("新增或更新产品参数错误");

    }

    @Override
    public ServerResponse setSaleStatus(Integer productId, Integer status) {

        if (productId == null && status == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if (rowCount > 0) {
            return ServerResponse.createBySuccess("更新商品状态成功");
        }
        return ServerResponse.createByError("更新商品状态失败");
    }

    @Override
    public ServerResponse<ProductDetailVO> getDetail(Integer productId) {
        if (productId == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null) {
            return ServerResponse.createByError("该商品已被下架或者删除");
        }
        ProductDetailVO pvo = getPVO(product);


        return ServerResponse.createBySuccess(pvo);
    }

    @Override
    public ServerResponse<PageInfo> getProductList(Integer pageNum, Integer pageSize) {
        //pagehelper page start
        //自定义sql
        //返回list
        PageHelper.startPage(pageNum, pageSize);
        List<Product> productList = productMapper.selectLists();
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for (Product productItem : productList) {
            productListVoList.add(getProductListVo(productItem));
        }

        PageInfo result = new PageInfo(productListVoList);
        return ServerResponse.createBySuccess(result);
    }

    public ServerResponse<PageInfo> searchProductList(String productName, Integer productId, Integer pageNum, Integer pageSize) {
        //pagehelper page start
        //自定义sql
        //返回list
        PageHelper.startPage(pageNum, pageSize);
        String productNameLike = new StringBuilder().append("%").append(productName).append("%").toString();
        List<Product> productList = productMapper.selectByProductNameAndId(productNameLike, productId);
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for (Product productItem : productList) {
            productListVoList.add(getProductListVo(productItem));
        }

        PageInfo result = new PageInfo(productListVoList);
        return ServerResponse.createBySuccess(result);
    }


    //前台用户查询产品详情
    @Override
    public ServerResponse<ProductDetailVO> getProductDetail(Integer productId) {
        if (productId == null) {
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null) {
            return ServerResponse.createByError("该商品已被下架或者删除");
        }
        if (product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()) {
            return ServerResponse.createByError("该商品已被下架或者删除");
        }
        ProductDetailVO pvo = getPVO(product);
        return ServerResponse.createBySuccess(pvo);
    }

    //前台列表搜索动态排序
    public ServerResponse<PageInfo> getCustomList(Integer categoryId, String keyword, Integer pageNum, Integer pageSize, String orderBy) {
        if(StringUtils.isBlank(keyword) && categoryId == null){
            return ServerResponse.createByErrorMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        List<Integer> categoryIdList = new ArrayList<Integer>();

        if(categoryId != null){
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if(category == null && StringUtils.isBlank(keyword)){
                //没有该分类,并且还没有关键字,这个时候返回一个空的结果集,不报错
                PageHelper.startPage(pageNum,pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productListVoList);
                return ServerResponse.createBySuccess(pageInfo);
            }
            categoryIdList = iCategoryService.getDeepCategory(category.getId()).getData();
        }
        if(StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }

        PageHelper.startPage(pageNum,pageSize);
        //排序处理
        if(StringUtils.isNotBlank(orderBy)){
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] orderByArray = orderBy.split("_");
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]);
            }
        }
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword,categoryIdList.size()==0?null:categoryIdList);

        List<ProductListVo> productListVoList = Lists.newArrayList();
        for(Product product : productList){
            ProductListVo productListVo = getProductListVo(product);
            productListVoList.add(productListVo);
        }

        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }


    private ProductDetailVO getPVO(Product product) {
        ProductDetailVO pvo = new ProductDetailVO();
        pvo.setId(product.getId());
        pvo.setStatus(product.getStatus());
        pvo.setCategoryId(product.getCategoryId());
        pvo.setDetail(product.getDetail());
        pvo.setMainImage(product.getMainImage());
        pvo.setName(product.getName());
        pvo.setPrice(product.getPrice());
        pvo.setStock(product.getStock());
        pvo.setSubImages(product.getSubImages());
        pvo.setSubtitle(product.getSubtitle());
        pvo.setImageHost((PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.happymmall.com/")));

        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if (category == null) {
            pvo.setParentCategoryId(0);
        } else {
            pvo.setParentCategoryId(category.getParentId());
        }
        pvo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        pvo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        return pvo;
    }

    private ProductListVo getProductListVo(Product product) {
        ProductListVo plv = new ProductListVo();
        plv.setPrice(product.getPrice());
        plv.setId(product.getId());
        plv.setName(product.getName());
        plv.setSubtitle(product.getSubtitle());
        plv.setMainImage(product.getMainImage());
        plv.setStatus(product.getStatus());
        plv.setCategoryId(product.getCategoryId());

        plv.setImageHost((PropertiesUtil.getProperty("ftp.server.http.prefix", "http://img.happymmall.com/")));
        return plv;
    }
}
