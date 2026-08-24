package com.wanger.userservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wanger.userservice.entity.UserInfo;
import com.wanger.userservice.mapper.UserInfoMapper;
import com.wanger.userservice.service.IUserInfoService;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}