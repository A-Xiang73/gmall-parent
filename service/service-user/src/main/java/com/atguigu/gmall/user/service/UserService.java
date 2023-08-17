package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/9 18:16
 */
public interface UserService {
    UserInfo login(UserInfo userInfo);
}
