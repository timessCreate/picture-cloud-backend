package com.timess.picturecloud;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 测试redis的基本操作
 */
@SpringBootTest
public class RedisStringTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisTemplateOperations(){
        //获取字符串类型的操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        //key-value
        String key = "testKey";
        String value = "testValue";

        //1.测试新增
        valueOps.set(key, value);
        //获取当前key
        String s = valueOps.get(key);
        assert  value.equals(s);
        System.out.println("新增操作测试成功");

        //2. 测试修改
        String updateValue = "updateValue";
        valueOps.set(key, updateValue);
        //获取更新后的value
        String updateResult = valueOps.get(key);
        assert updateValue.equals(updateResult);
        System.out.println("修改操作成功");

        stringRedisTemplate.delete(key);
        String result = valueOps.get(key);
        Assert.isNull(result);
        System.out.println("删除成功");
    }
}
