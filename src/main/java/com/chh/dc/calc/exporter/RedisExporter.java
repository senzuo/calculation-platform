package com.chh.dc.calc.exporter;

import com.chh.dc.calc.util.redis.JedisFactory;
import com.chh.dc.calc.util.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by niow on 16/10/24.
 */
public class RedisExporter {

    public static final Logger log = LoggerFactory.getLogger(RedisExporter.class);

    public static final String OP_ZADD = "zadd";

    public static final String OP_SADD = "sadd";

    public static final String OP_PUBLISH = "publish";

    public static final String OP_LPUSH = "lpush";

    public static final String OP_SET = "set";

    public static final String OP_HSET = "hset";

    private JedisFactory jedisFactory;


    protected List<String> getReplaceKey(String keyValue) {
        String[] split = keyValue.split("\\{");
        List<String> keys = new ArrayList<String>(split.length);
        for (String str : split) {
            int index = str.indexOf("}");
            if (index < 0) {
                continue;
            }
            String dynamicKey = str.substring(0, index);
            keys.add(dynamicKey);
        }
        return keys;
    }


    public void export(String type, byte[] key, Map<String, Object> record, String scoreField, String hashField) {
        Jedis jedis = jedisFactory.getJedis();
        byte[] data = SerializeUtil.serialize(record);
        try {
            switch (type) {
                case OP_SET: {
                    jedis.set(key, data);
                    break;
                }
                case OP_SADD: {
                    jedis.sadd(key, data);
                    break;
                }
                case OP_ZADD: {
                    if (scoreField == null) {
                        break;
                    }
                    Object score = record.get(scoreField);
                    if (score instanceof Date) {
                        score = ((Date) score).getTime();
                    } else if (score instanceof Double) {
                        jedis.zadd(key, (double) score, data);
                        break;
                    }
                    jedis.zadd(key, Double.parseDouble(score.toString()), data);
                    break;
                }
                case OP_LPUSH: {
                    jedis.lpush(key, data);
                    break;
                }
                case OP_PUBLISH: {
                    jedis.publish(key, data);
                    break;
                }
                case OP_HSET:{
                    jedis.hset(key,hashField.getBytes(),data);
                    break;
                }
            }
        } catch (Exception e){
            log.error("Redis输出数据出错"+new String(key),e);
        }
        finally {
            jedisFactory.returnBackJedis(jedis);
        }
    }

    public JedisFactory getJedisFactory() {
        return jedisFactory;
    }

    public void setJedisFactory(JedisFactory jedisFactory) {
        this.jedisFactory = jedisFactory;
    }
}
