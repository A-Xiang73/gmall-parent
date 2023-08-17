package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.PrimitiveIterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote 用户认证接口
 * @date 2023/8/9 18:17
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;
    /*
    * 1、用接收的用户名密码核对后台数据库
    * 2、核对通过，用uuid生成token
    * 3、将用户id加载到写入redis，redis的key为token，value为用户id。
    * 4、登录成功返回token与用户信息，将token与用户信息记录到cookie里面
    * 5、重定向用户到之前的来源地址。
    * */

    //登录接口的实现
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request
    , HttpServletResponse response){
        //进行登录 登录成功返回userinfo对象
        UserInfo user=userService.login(userInfo);
        //如果user查询到， 则生成对应的token 将token和相关的数据存储到redis
        if (user != null) {
            //生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            //获取自己的ip
            String ipAddress = IpUtil.getIpAddress(request);
            String userId = user.getId().toString();
            //创建一个json对象作为redis中的value
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ip",ipAddress);
            jsonObject.put("userId",userId);
            //存储  key user:login:token:userJson
            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX+token,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //前端需要的数据有nickname和userId
            HashMap<Object, Object> resultMap = new HashMap<>();
            resultMap.put("nickName",user.getNickName());
            resultMap.put("token",token);
            return Result.ok(resultMap);
        }else {
            return Result.fail().message("用户名或密码错误");
        }

    }
    /**
     * 退出登录
     * @param request
     * @return
     */
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        //登出的token是存放在请求头里面
        redisTemplate.delete(RedisConst.USER_KEY_PREFIX+request.getHeader("token"));
        return Result.ok();
    }

}
