package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import jdk.nashorn.internal.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/9 19:59
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {
    //网关引入redisConfig的原因 保持序列化规则一样 service服务中使用的是jackson2   这里若不引入redisTemplate使用默认的序列化规则
    @Autowired
    private RedisTemplate redisTemplate;
    //匹配路径的工具类
    private AntPathMatcher antPathMatcher=new AntPathMatcher();
    //获取配置文件中的白名单
    @Value("${authUrls.url}")
    private String authUrls;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /*
        * 全局过滤器
        * 1.首先获取请求路径
        * 2.判断是否为内部路径，内部路径拒绝访问，
        * 3.不是内部路径，获取用户id，判断用户是否已经登录
        * 4.设置白名单，拦截需要过滤的页面
        * 5.放行
        * */
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        //获取url路径
        String path = request.getURI().getPath();
        //判断是否为内部路径 内部资源 /**/inner/**
        if (antPathMatcher.match("/**/inner/**",path)) {
            return this.out(response, ResultCodeEnum.PERMISSION);
        }
        //认证用户 token redis
        String userId=this.getUserId(request);
        //ip被盗用的判断
        if ("-1".equals(userId)) {
            return this.out(response,ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //判断认证路径   /**/auth/** ，判断用户id是否存在，
        if(antPathMatcher.match("/**/auth/**",path)&& StringUtils.isEmpty(userId)){
            return this.out(response,ResultCodeEnum.LOGIN_AUTH);
        }

        //白名单判断
        if (StringUtils.isEmpty(authUrls)) {
            String[] splits = authUrls.split(",");
            for (String split : splits) {
                if (path.indexOf(split)!=-1&& StringUtils.isEmpty(userId)) {
                    //设置状态码
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //进行重定向 设置location
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                    return response.setComplete();
                }
            }
        }

        //获取临时用户id
        String userTempId=this.getUserTempId(request);
        //携带userId的情况
        if (!StringUtils.isEmpty(userTempId)||!StringUtils.isEmpty(userId)) {
            //如果userid不为空就将userid存进去
            if (!StringUtils.isEmpty(userId)) {
                //mutate构建一个新的请求对象，将userid设置到请求头中去
                request.mutate().header("userId",userId).build();
            }
            //如果临时id部位空则将临时id存进去
            if (StringUtils.isEmpty(userTempId)) {
                request.mutate().header("userTempId",userTempId).build();
            }
            /*chain 请求过滤器链  exchange.mutate 用于创建修改过的ServerWevExchange对象的方法，ServerWebExchange封装来客户端的请求项客户度
             *发送响应的上下文信息，.request（request）通过请求对象来修改serverwebexchange对象的方法 ，request在上文增加了userid和usertempid
             * 整个表达式的作用是对传入的chain对象进行过滤处理，并使用request对象修改了ServerWebExchange对象，然后将修改过的ServerWebExchange对象传递给下一个过滤器或处理程序。
             */
            return chain.filter(exchange.mutate().request(request).build());
        }
        //放行
        return chain.filter(exchange);
    }


    /*
    * 获取临时id,
    * */
    private String getUserTempId(ServerHttpRequest request) {
        //先从head中获取
        String userTempId = request.getHeaders().getFirst("userTempId");
        //如果header中没有获取到再从cooike中获取
        if (StringUtils.isEmpty(userTempId)) {
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            if (cookies != null) {
                HttpCookie cookie = cookies.getFirst("userTempId");
                if (cookie != null) {
                    userTempId = cookie.getValue();
                }
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //先从head中获取token 根据token查询redis 找到对应的用户
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isEmpty(token)) {
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            if (cookies != null) {
                HttpCookie cookie = cookies.getFirst("token");
                if (cookie != null) {
                    token=cookie.getValue();
                }
            }
        }
        //判断token
        if (token != null) {
            String strJson = (String) redisTemplate.opsForValue().get("user:login:" + token);
            //转换成jsonObject对象 方便获取userid和ip
            if (strJson != null) {
                //转换成jsonobject
                JSONObject jsonObject = JSONObject.parseObject(strJson);
                String userId = jsonObject.getString("userId");
                String ip = jsonObject.getString("ip");
                //获取当前ip
                String ipAddress = IpUtil.getGatwayIpAddress(request);
                if (ip.equals(ipAddress)) {
                    return userId;
                }else {
                    return "-1";
                }
            }

        }
        return "";
    }

    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {
        //封装响应体
        Result<Object> result = Result.build(null, permission);
        //转换成json对象
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        //获取DataBuffer缓冲区对象
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //设置中文编码，防止乱码
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        //Mono.just（wrap）创建一个Mono<DataBuffer>对象用于异步的将字节数组写入响应体 返回一个Mono<void> 表示异步写入响应体完成
        return response.writeWith(Mono.just(wrap));
    }
}






