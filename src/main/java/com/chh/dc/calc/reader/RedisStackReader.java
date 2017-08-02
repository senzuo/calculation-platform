package com.chh.dc.calc.reader;

import com.chh.dc.calc.util.redis.JedisFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Created by Niow on 2016/7/12.
 */
public class RedisStackReader extends RedisDataReader {

    public static final Logger log = LoggerFactory.getLogger(RedisStackReader.class);

    private JedisFactory jedisFactory = null;

    public static final int OPTION_RPOP = 1;

    public static final int OPTION_LPOP = 2;

    public static final int OPTION_BLPOP = 3;

    public static final int OPTION_BRPOP = 4;


    private int option = 1;

    private String stackName;


    /**
     * seconds,0==disable
     */
    private int timeout = 0;

    public RedisStackReader() {

    }

    public RedisStackReader(int option) {
        this.option = option;
    }

    public RedisStackReader(int option, int timeout) {
        this.option = option;
    }


    @Override
    public DataPackage getData(Object... objects)  throws Exception {
        if (stackName == null && objects == null) {
            return null;
        }
        byte[] stackNameBytes = null;
        if (stackName != null) {
            stackNameBytes = stackName.getBytes();
        } else {
            stackNameBytes = (byte[]) objects[0];
        }
        Jedis jedis = null;
        DataPackage pack = null;
        try {
            jedis = jedisFactory.getJedis();
            byte[] data = null;
            switch (option) {
                case OPTION_RPOP: {
                    data = jedis.rpop(stackNameBytes);
                    break;
                }
                case OPTION_LPOP: {
                    data = jedis.lpop(stackNameBytes);
                    break;
                }
                case OPTION_BLPOP: {
                    List<byte[]> blpop = jedis.blpop(timeout, stackNameBytes);
                    if (blpop != null && blpop.size() > 0) {
                        data = blpop.get(0);
                    }
                    break;
                }
                case OPTION_BRPOP: {
                    List<byte[]> blpop = jedis.brpop(timeout, stackNameBytes);
                    if (blpop != null && blpop.size() > 0) {
                        data = blpop.get(0);
                    }
                    break;
                }
            }
            if (data == null) {
                return null;
            }
            pack = new DataPackage();
            pack.setData(data);
        } catch (Exception e) {
            log.error("获取RedisStack缓存数据" + stackName + "出错", e);
            throw e;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return pack;
    }


    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public JedisFactory getJedisFactory() {
        return jedisFactory;
    }

    public void setJedisFactory(JedisFactory jedisFactory) {
        this.jedisFactory = jedisFactory;
    }
}
