package com.chh.dc.calc.reader;

import com.chh.dc.calc.util.redis.JedisFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

/**
 * Created by fulr on 2016/11/28.
 */
public class RedisDataReader implements DataReader {

    public static final Logger log = LoggerFactory.getLogger(RedisDataReader.class);

    private JedisFactory jedisFactory = null;

    public static final int OPTION_RPOP = 1;

    public static final int OPTION_LPOP = 2;

    public static final int OPTION_BLPOP = 3;

    public static final int OPTION_BRPOP = 4;
    
    public static final int OPTION_LRANGE = 5;

    public static final int OPTION_ZRANGE_BY_SCORES = 6;

    public static final int OPTION_ZREVRANGE_BY_SCORES = 7;

    public static final int OPTION_HMGET = 8;

    public static final int OPTION_HGET = 9;

//    private int option = 1;

//    private String keyName;


    /**
     * seconds,0==disable
     */
    private int timeout = 0;

    public RedisDataReader() {

    }


    /**
     * 参数objects[0]: 请求类型
     * 参数objects[0]: redis key
     * RedisHashReader.OPTION_HGET, LAST_GPS_KEY
     * @param objects
     *
     * @return
     */
    @Override
    public DataPackage getData(Object... objects)  throws Exception {
        if (objects == null||objects.length<2) {
            return null;
        }
        int op = (int) objects[0];
        byte[] keyNameBytes = ((String) objects[1]).getBytes();
        Jedis jedis = null;
        DataPackage pack = null;
        try {
            jedis = jedisFactory.getJedis();
            Object data = null;
            switch (op) {
                case OPTION_RPOP: {
                    data = jedis.rpop(keyNameBytes);
                    break;
                }
                case OPTION_LPOP: {
                    data = jedis.lpop(keyNameBytes);
                    break;
                }
                case OPTION_BLPOP: {
                    List<byte[]> blpop = jedis.blpop(timeout, keyNameBytes);
                    if (blpop != null && blpop.size() > 0) {
                        data = blpop.get(0);
                    }
                    break;
                }
                case OPTION_BRPOP: {
                    List<byte[]> blpop = jedis.brpop(timeout, keyNameBytes);
                    if (blpop != null && blpop.size() > 0) {
                        data = blpop.get(0);
                    }
                    break;
                }
                case OPTION_LRANGE: {
                	String min = objects[2].toString();
                    String max = objects[3].toString();
                    data = jedis.lrange(keyNameBytes, Long.parseLong(min), Long.parseLong(max));
                    break;
                }
                case OPTION_ZRANGE_BY_SCORES: {
                    String min = objects[2].toString();
                    String max = objects[3].toString();
                    Set<byte[]> bytes = jedis.zrangeByScore(keyNameBytes, Double.parseDouble(min), Double.parseDouble(max));
                    if (bytes!=null&&bytes.size()>0) {
                        data = bytes;
                    }
                    break;
                }
                case OPTION_ZREVRANGE_BY_SCORES: {
                    String min = objects[2].toString();
                    String max = objects[3].toString();
                    Set<byte[]> bytes = null;
                    if (objects.length >= 6 && objects[4] != null && objects[5] != null) {
                        int offset = (int) objects[4];
                        int count = (int) objects[5];
                        bytes = jedis.zrevrangeByScore(keyNameBytes, Double.parseDouble(max), Double.parseDouble(min), offset, count);
                    } else {
                        bytes = jedis.zrevrangeByScore(keyNameBytes, Double.parseDouble(max), Double.parseDouble(min));
                    }
                    if (bytes!=null&&bytes.size()>0) {
                        data = bytes;
                    }
                    break;
                }
                case OPTION_HMGET: {
//                    List<byte[]> bytes = null;
                    if(objects.length>2){
                        byte[][] fields = new byte[objects.length-2][];
                        for (int i=2;i<objects.length;i++){
                            fields[i-2] = objects[i].toString().getBytes();
                        }
                        data = jedis.hmget(keyNameBytes,fields);
                    }
                    break;
                }
                case OPTION_HGET:{
                    data = jedis.hget(keyNameBytes,objects[2].toString().getBytes());
                    break;
                }
            }
            if (data == null) {
                return null;
            }
            pack = new DataPackage();
            pack.setData(data);
        } catch (Exception e) {
            log.error("获取Redis缓存数据" + objects[1].toString() + "出错", e);
            throw e;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return pack;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public JedisFactory getJedisFactory() {
        return jedisFactory;
    }

    public void setJedisFactory(JedisFactory jedisFactory) {
        this.jedisFactory = jedisFactory;
    }
}
