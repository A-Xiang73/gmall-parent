package com.atguigu.gmall.common.cache;

import com.atguigu.gmall.common.constant.RedisConst;
import javassist.LoaderClassPath;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author 24657
 * @apiNote
 * @date 2023/8/2 21:23
 */
@Component
@Aspect
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    //切GmallCache注解
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")//切点表达式
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            GmallCache gmallCache = method.getAnnotation(GmallCache.class);
            String prefix = gmallCache.prefix();
            String suffix = gmallCache.suffix();
            //获取切入点参数
            Object[] args = joinPoint.getArgs();
            //存入缓存的key 前缀+参数+后缀
            String key = prefix + Arrays.asList(args) + suffix;
            //从缓存中查询
//            RLock hitlock = redissonClient.getLock(RedisConst.SKUKEY_PREFIX+"hitlock");
//            try {
//              hitlock.lock();
                Object queryObj = redisTemplate.opsForValue().get(key);
                //判断是否查询到
                if (queryObj == null) {

                    RLock lock = null;
                    //没有查询到，进入数据库操作 进行自定义业务操作
                    try {
                        String lockkey = prefix + Arrays.asList(args) + RedisConst.SKULOCK_SUFFIX;
                        //获取分布式锁
                        lock = redissonClient.getLock(lockkey);
                        //上锁 判断是否抢到锁
                        boolean isQuired = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                        if (isQuired) {
                            //二次检查
                            queryObj = redisTemplate.opsForValue().get(key);
                            if(queryObj!=null){
                                return queryObj;
                            }
                            //进行自定义数据库操作
                            queryObj = joinPoint.proceed(args);
                            //判断数据库是否操作到
                            if (queryObj != null) {
                                //查询到了，则返回，并且将其存到redis缓存
                                redisTemplate.opsForValue().set(key, queryObj, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                                return queryObj;
                            } else {
                                Object nullObj = new Object();
                                //若没有查询到则返回 一个空数据并存到缓存 防止缓存穿透
                                redisTemplate.opsForValue().set(key, nullObj, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                                return nullObj;
                            }
                        } else {
                            Thread.sleep(100);
                            return cacheAroundAdvice(joinPoint);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                else {
                    //从缓存中查到了直接返回
                    return queryObj;
                }
//            } finally {
//                hitlock.lock();
//            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        //保底操作 执行自定义操作
        return joinPoint.proceed();
    }

    private Object getObject(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        String suffix = gmallCache.suffix();
        //获取切入点参数
        Object[] args = joinPoint.getArgs();
        //存入缓存的key 前缀+参数+后缀
        String key = prefix + Arrays.asList(args) + suffix;
        //从缓存中查询
        Object queryObj = redisTemplate.opsForValue().get(key);
        //判断是否查询到
        if (queryObj == null) {
            RLock lock = null;
            //没有查询到，进入数据库操作 进行自定义业务操作
            try {
                String lockkey = prefix + Arrays.asList(args) + RedisConst.SKULOCK_SUFFIX;
                //获取分布式锁
                lock = redissonClient.getLock(lockkey);
                //上锁 判断是否抢到锁
                boolean isQuired = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (isQuired) {
                    //进行自定义数据库操作
                    queryObj = joinPoint.proceed(args);
                    //判断数据库是否操作到
                    if (queryObj != null) {
                        //查询到了，则返回，并且将其存到redis缓存
                        redisTemplate.opsForValue().set(key, queryObj, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return queryObj;
                    } else {
                        Object nullObj = new Object();
                        //若没有查询到则返回 一个空数据并存到缓存 防止缓存穿透
                        redisTemplate.opsForValue().set(key, nullObj, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return nullObj;
                    }
                } else {
                    Thread.sleep(100);
                    return cacheAroundAdvice(joinPoint);
                }
            } finally {
                lock.unlock();
            }
        } else {
            //从缓存中查到了直接返回
            return queryObj;
        }
    }
}
