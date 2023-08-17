package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/11 21:26
 */


public interface UserAddressService {
    List<UserAddress> findUserAddressListByUserId(String userId);
}
