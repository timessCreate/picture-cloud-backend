package com.timess.picturecloud.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Properties;

/**
 * @author 33363
 * 自动分库分表算法
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        //分表指定字段值获取
        Long spaceId = preciseShardingValue.getValue();
        //获取到逻辑表名
        String logicTableName = preciseShardingValue.getLogicTableName();
        //spaceId 为null 所有图片
        if(spaceId == null){
            return logicTableName;
        }
        if(spaceId == 0L){
            return "picture";
        }
        //根据id动态生成分表名
        String realTableName = "picture_" + spaceId;

        if(collection.contains(realTableName)){
            return realTableName;
        }else{
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayDeque<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
