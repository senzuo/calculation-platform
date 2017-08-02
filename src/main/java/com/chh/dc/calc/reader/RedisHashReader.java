package com.chh.dc.calc.reader;

import redis.clients.jedis.Jedis;

/**
 * @deprecated
 * RedisDataReader中已包含该功能
 * @author wangbin 
 * @date 2016年11月26日 下午3:21:29    
 * @Description: TODO
 */
public class RedisHashReader extends RedisStackReader {

    public static final int OPTION_HGET = 1;

    @Override
    public DataPackage getData(Object... objects) {
        Jedis jedis = null;
        DataPackage pack = null;
        try {
            jedis = getJedisFactory().getJedis();
            if (objects == null || objects.length == 0) {
                return null;
            }
            int op = (int) objects[0];
            String key = (String) objects[1];
            String field = (String) objects[2];
            switch (op) {
                case OPTION_HGET: {
                    byte[] bytes = jedis.hget(key.getBytes(), field.getBytes());
                    DataPackage dataPackage = new DataPackage();
                    dataPackage.setData(bytes);
                    return dataPackage;
                }
            }

        } catch (Exception e) {
            log.error("获取RedisHash进行" + objects[0] + "数据读取" + objects[1] + "出错", e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return pack;
    }
}