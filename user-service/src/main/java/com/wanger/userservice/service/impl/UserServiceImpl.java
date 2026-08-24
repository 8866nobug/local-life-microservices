package com.wanger.userservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import com.wanger.common.dto.UserDTO;
import com.wanger.common.utils.RegexUtils;
import com.wanger.userservice.dto.LoginFormDTO;
import com.wanger.userservice.entity.User;
import com.wanger.userservice.mapper.UserMapper;
import com.wanger.userservice.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        //校验手机号格式
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //符合，生成校验码
        String code = RandomUtil.randomNumbers(6);
        //将校验码保存到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //模拟发送校验码
        log.info("发送验证码，{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        //校验手机号格式
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        //从redis中取验证码，并校验
        String code =stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+loginForm.getPhone());
        if(code==null||!code.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        //校验通过，删除验证码，防止被重复使用
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());
        //查询用户是否存在
        User user = lambdaQuery()
                .eq(User::getPhone, loginForm.getPhone())
                .one();
        //不存在，创建新用户
        if(user==null) {
            user = insertNewUserWithPhone(loginForm.getPhone());
        }
        //将用户存入redis
        //生成UUID作为唯一标识token
        String token = UUID.randomUUID().toString();
        //处理Long
        UserDTO userDTO= BeanUtil.copyProperties(user,UserDTO.class);
        Map map = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String key=RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll( key,map);
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        //返回token
        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        if (token == null || token.isBlank()) {
            return Result.fail("未登录");
        }
        // 删除 Redis 中的登录会话，token 随即失效
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        return Result.ok();
    }

    private User insertNewUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+ RandomUtil.randomString(6));
        save(user);
        return user;
    }
}

