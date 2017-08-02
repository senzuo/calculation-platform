package com.chh.dc.calc.util.redis;

import com.chh.dc.calc.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

public class JedisFactory {

    private static final Logger log = LoggerFactory.getLogger(JedisFactory.class);

    private JedisPool pool;

    private String host;

    private int port;

    /** 链接到redis server超时 */
    private int timeout = 10;

    /** 获取instance最大等待时间，如果超过则创建新的instance或throw exception */
    private long maxWaitMills;

    private int maxTotal;

    private int maxIdle;

    private String password;

    private int  maxActive;

    private int leftMaxTime;

    private boolean enRedisable;

    public JedisFactory() {

    }

    public JedisFactory(String host, int port, int timeout, long maxWaitMills, int maxTotal, int maxIdle, String password, int maxActive, int leftMaxTime, boolean enRedisable) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.maxWaitMills = maxWaitMills;
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.password = password;
        this.maxActive=maxActive;
        this.leftMaxTime=leftMaxTime;
        this.enRedisable=enRedisable;
    }

    public Jedis getJedis() {
        if (pool == null) {
            initPool();
        }
        return pool.getResource();
    }

    public static Jedis createJedis( String host, int port, int timeout,String password) {
        JedisShardInfo info = new JedisShardInfo(host, port);
        info.setConnectionTimeout(timeout);
        if (StringUtil.isNotNull(password)) {
            info.setPassword(password);
        }
        Jedis jedis = new Jedis(info);
        return jedis;
    }

    public void returnBackJedis(Jedis jedis) {
        if (pool == null) {
            initPool();
        }
        jedis.close();
    }

    private void initPool() {
        if (pool == null) {
            log.info(
                    "Start initialize Jedis Pool with params(host:{},port:{},passwordEnable:{},timeout:{},maxTotal:{},maxIdle:{},maxWaitMills:{})",
                    host, port, password != null, timeout, maxTotal, maxIdle, maxWaitMills);
            JedisPoolConfig config = new JedisPoolConfig();
            // 控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
            // 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
            config.setMaxTotal(maxTotal);
            // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
            config.setMaxIdle(maxIdle);
            // 表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
            config.setMaxWaitMillis(maxWaitMills);
            // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
            config.setTestOnBorrow(true);
            if (password != null) {
                pool = new JedisPool(config, host, port, timeout, password);
            } else {
                pool = new JedisPool(config, host, port, timeout);
            }
            log.info("creating Jedis Pool completed");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getMaxWaitMills() {
        return maxWaitMills;
    }

    public void setMaxWaitMills(long maxWaitMills) {
        this.maxWaitMills = maxWaitMills;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getLeftMaxTime() {
        return leftMaxTime;
    }

    public void setLeftMaxTime(int leftMaxTime) {
        this.leftMaxTime = leftMaxTime;
    }

    public boolean isEnRedisable() {
        return enRedisable;
    }

    public void setEnRedisable(boolean enRedisable) {
        this.enRedisable = enRedisable;
    }
}
