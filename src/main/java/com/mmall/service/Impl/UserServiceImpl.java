package com.mmall.service.Impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by lcy on 2017/12/20.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int num = userMapper.checkUsername(username);
        if (num == 0) {
            return ServerResponse.createByError("用户名不存在");
        }
        //MD5加密
        String md5Password = MD5Util.MD5EncodeUtf8(password);


        User user = userMapper.selectLogin(username, md5Password.toUpperCase());
        if (user == null) {
            return ServerResponse.createByError("密码输入错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功", user);
    }

    @Override
    public ServerResponse<String> register(User user) {
        ServerResponse response = this.checkValid(user.getUsername(), Const.USERNAME);
        if (!response.isSuccess()) {
            return response;
        }

        response = this.checkValid(user.getEmail(), Const.EMAIL);
        if (!response.isSuccess()) {
            return response;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int count = userMapper.insert(user);
        if (count == 0) {
            return ServerResponse.createByError("注册失败");
        }
        return ServerResponse.createBySuccess("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if (StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                int count = userMapper.checkUsername(str);
                if (count > 0) {
                    return ServerResponse.createByError("用户名已存在");
                }
            }

            if (Const.EMAIL.equals(type)) {
                int count = userMapper.checkEmail(str);
                if (count > 0) {
                    return ServerResponse.createByError("邮箱已被注册");
                }
            }

        } else {
            return ServerResponse.createByError("参数错误");
        }
        return ServerResponse.createBySuccess("校验正确");
    }

    @Override
    public ServerResponse<String> selectQuestion(String username) {
        int num = userMapper.checkUsername(username);
        if (num == 0) {
            return ServerResponse.createByError("该用户名不存在");
        }
        String question = userMapper.selectQuestion(username);
        if (!StringUtils.isNotBlank(question)) {
            return ServerResponse.createByError("该用户未设置找回密码问题");
        }
        return ServerResponse.createBySuccess(question);
    }

    @Override
    public ServerResponse<String> getAnswer(String username, String question, String answer) {
        int num = userMapper.selectAnswer(username, question, answer);
        if (num == 0) {
            return ServerResponse.createByError("问题答案错误");
        }
        String uuid = UUID.randomUUID().toString();
        TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, uuid);
        return ServerResponse.createBySuccess(uuid);
    }

    @Override
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByError("token不可以为空");
        }
        int num = userMapper.checkUsername(username);
        if (num == 0) {
            return ServerResponse.createByError("该用户名不存在,无法修改密码");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByError("token无效或者过期");
        }
        if (StringUtils.equals(token, forgetToken)) {
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if (rowCount > 0) {
                return ServerResponse.createBySuccess("修改成功");
            }
        } else {
            return ServerResponse.createByError("token密码错误，请重新获取token");
        }


        return ServerResponse.createByError("修改密码错误");
    }

    @Override
    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
        int rowCount = userMapper.selectPasswordAndId(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if (rowCount == 0) {
            return ServerResponse.createByError("原密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        rowCount = userMapper.updateByPrimaryKeySelective(user);
        if (rowCount > 0) {
            return ServerResponse.createBySuccess("密码修改成功");
        }
        return ServerResponse.createByError("修改密码错误");
    }

    @Override
    public ServerResponse<User> updateInformation(User user) {
        //用户名不能更新
        //email不能与其他用户的email重复

        int rowCount = userMapper.checkEmailAndId(user.getEmail(), user.getId());
        if (rowCount > 0) {
            return ServerResponse.createByError("该email已被注册，请重新输入email");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
        rowCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (rowCount > 0) {
            User newUser = userMapper.selectByPrimaryKey(user.getId());
            newUser.setPassword(StringUtils.EMPTY);
            return ServerResponse.createBySuccess("用户个人信息更新成功", newUser);
        }
        return ServerResponse.createByError("用户个人信息更新失败");
    }

    @Override
    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            user.setPassword(StringUtils.EMPTY);
            return ServerResponse.createBySuccess("获得用户信息成功", user);
        }

        return ServerResponse.createByError("获取用户信息失败");
    }
@Override
    public ServerResponse checkAdmin(User user) {

        if (user != null && user.getRole() == Const.Role.ROLE_ADMIN) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

}
