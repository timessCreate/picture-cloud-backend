package com.timess.picturecloud.manager;

import com.timess.picturecloud.exception.ErrorCode;
import com.timess.picturecloud.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author xing10
 * 提供RedisLimiter限流服务
 */
@Service
public class RedissonLimitManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * 通过key区分不同的限流器
     */
    public void doRateLimit(String key){
        //创建一个限流器，每秒最多访问2次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        //限流器的统计规则(限制所有请求速率，每秒2个请求，最多允许通过一个)
        //RateType.OVERALL 表示速率限制作用于整个令牌桶，即限制所有请求的速率
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        //每来一个操作，就请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,"请求过于频繁");
        }
    }

}
