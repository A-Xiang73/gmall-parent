package com.atguigu.gmall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 24657
 * @apiNote 线程池
 * @date 2023/8/4 18:03
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        /*
        * 核心线程数量
        * 拥有最多的线程数
        * 表示空闲线程的存活时间
        * 存活时间的单位
        * 用于缓存队伍的阻塞队列
        * htreadFactory指定创建线程的工厂
        * handler表示当workQueue已满，且线程池中的线程以达到上线时，线程池拒绝添加新的任务采取的拒绝策略
        *
        * */

        return new ThreadPoolExecutor(50,500,30, TimeUnit.SECONDS,new ArrayBlockingQueue<>(10000));
    }
}
