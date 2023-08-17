package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/11 21:24
 */
@RestController
@RequestMapping("/api/user")
@SuppressWarnings("all")
public class UserApiController {
    @Autowired
    private UserAddressService userAddressService;

    /**
     * 获取用户地址
     * @param userId
     * @return
     */
    @GetMapping("/inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable("userId") String userId){
        return  userAddressService.findUserAddressListByUserId(userId);
    }
}
