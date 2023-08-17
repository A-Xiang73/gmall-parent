package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/12 9:02
 */
@Component
public class UserDegradeFeignClient implements UserFeignClient{
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        return null;
    }
}
