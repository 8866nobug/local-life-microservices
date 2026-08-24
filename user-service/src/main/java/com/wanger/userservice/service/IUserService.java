package com.wanger.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wanger.common.dto.Result;
import com.wanger.userservice.dto.LoginFormDTO;
import com.wanger.userservice.entity.User;

public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);

    Result logout(String token);
}