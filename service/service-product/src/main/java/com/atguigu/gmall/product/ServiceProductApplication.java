package com.atguigu.gmall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 24657
 * @apiNote
 * @date 2023/7/25 14:49
 */
@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplication.class, args);
    }
}
