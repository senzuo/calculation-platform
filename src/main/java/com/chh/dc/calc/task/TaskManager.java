package com.chh.dc.calc.task;

import com.chh.dc.calc.Runner;
import com.chh.dc.calc.trigger.TriggerManager;
import com.chh.dc.calc.util.redis.RedisCache;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.trigger.Event;
import com.chh.dc.calc.util.SerializeUtil;
import com.chh.dc.calc.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by niow on 16/10/7.
 */
public class TaskManager extends Thread {

    public static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private TriggerManager triggerManager;

    private RedisCacheMgr redisCacheMgr;

    private static boolean keepRunning = true;

    private static int taskMaxNum = 50;

    /**
     * 任务线程池
     */
    private ExecutorService threadPool;

    /**
     * 任务线程池包装类
     */
    private CompletionService<TaskFuture> service;


    /**
     * <eventType,taskBeanId>
     */
    private Map<String, List<String>> taskMap;

    private List<Task> taskQueue = new ArrayList<>();

//    /**
//     * 正在运行的任务的Map<br>
//     * 同时使用workingTasks来进行并发控制,而不是使用线程池的线程并发控制,目的在于将运行队列提供给控制台用于显示<br>
//     */
//    private Map<String, Task> workingTasks = new HashMap<String, Task>();

    public void init() {
        threadPool = Executors.newFixedThreadPool(taskMaxNum);
        service = new ExecutorCompletionService<TaskFuture>(threadPool);
        Listener listener = new Listener();
        listener.start();
        redisCacheMgr.start();
    }

    @Override
    public void run() {
        while (keepRunning) {
            try {

                Event event = triggerManager.takeEvent();
                if (event == null) {
                    ThreadUtil.sleep(10);
                    continue;
                }
                List<String> taskList = taskMap.get(event.getType());
//                System.out.println(taskList);
                for (int i = 0; taskList != null && i < taskList.size(); i++) {
                    String taskId = taskList.get(i);
                    Task task = Runner.getBean(taskId,Task.class);

//                    if (task.isSingleton() && workingTasks.containsKey(task.getName())) {
//                        log.info("单例任务[{}]正在运行中", task.getName());
//                        continue;
//                    }
//                    System.out.println(event.getParams());
                    task.setEvent(event);
                    service.submit(task);
                }
            } catch (Exception e) {
                log.error("任务调度线程异常。",e);
            }
        }
    }


    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public void setTriggerManager(TriggerManager triggerManager) {
        this.triggerManager = triggerManager;
    }

    public RedisCacheMgr getRedisCacheMgr() {
        return redisCacheMgr;
    }

    public void setRedisCacheMgr(RedisCacheMgr redisCacheMgr) {
        this.redisCacheMgr = redisCacheMgr;
    }
//    /**
//     * 将任务从运行队列中移除 同时唤醒trigger线程
//     *
//     * @param task
//     */
//    public void removeTask(Task task) {
//        synchronized (workingTasks) {
//            if (!workingTasks.containsKey(task.getId())) {
//                log.error("任务已从运行队列移除失败,taskName={}在运行任务队列中不存在", task.getName());
//                return;
//            }
//            workingTasks.remove(task.getId());
//            workingTasks.notifyAll();
//            log.debug("任务已从运行队列移除：{}", task.getName());
//        }
//    }

    /**
     * 任务执行结果处理监听器<br>
     * 监听任务的执行结果
     */
    class Listener extends Thread {

        Listener() {
            super("任务结果处理器");
        }

        @Override
        public void run() {
            TaskFuture taskFuture = null;
            log.debug("任务运行结果提取线程启动。");
            while (keepRunning) {
                try {
                    // 取出任务运行结果 如果没有返回 则线程会挂起
                    Future<TaskFuture> future = service.take();
                    if (future == null) {
                        log.error("提取线程返回结果异常.Future==null");
                        continue;
                    }
                    taskFuture = future.get();
                    if (taskFuture == null) {
                        log.error("提取线程返回结果异常.TaskFuture==null");
                        continue;
                    }
                    Event event = taskFuture.getEvent();
                    if (event == null) {
                        continue;
                    }
                    log.debug("[event={},{}]", new Object[]{event.getType(), "cause=" + taskFuture.getCause()});
                } catch (InterruptedException e) {
                    log.error("提取任务线程运行中断", e);
                    continue;
                } catch (ExecutionException e) {
                    log.error("提取任务线程运行结果失败", e);
                    continue;
                } finally {
//                    // 无论返回成功与否都必须从 当前运行任务表 中清除掉
//                    if (taskFuture != null && taskFuture.getTask() != null) {
//                        removeTask(taskFuture.getTask());
//                    }
                }
            }
        }

    }

    public static class RedisCacheMgr extends Thread {

        /**
         * 轮询间隔时间：单位秒
         */
        private long intervalsSec = 4*60*60l;

//        private Long maxLifeSec = 10*3600*1000l;

        /**
         * 非盒子最后一次行程缓存数据最大缓存时长，单位秒
         */
        private long maxLifeSec = 10*60*60L;

        private RedisCache redisCache;

        public RedisCacheMgr(){
            super("缓存管理处理器");
        }

        public long getIntervalsSec() {
            return intervalsSec;
        }

        public void setIntervalsSec(long intervalsSec) {
            this.intervalsSec = intervalsSec;
        }

        public long getMaxLifeSec() {
            return maxLifeSec;
        }

        public void setMaxLifeSec(long maxLifeSec) {
            this.maxLifeSec = maxLifeSec;
        }

        @Override
        public void run(){
            while(keepRunning) {
                try {
                    //1.读取所有 htwx_processed_trip:开头的缓存key
                    //2.循环遍历key
                    //3.查看key包含的元素的个数
                    //4.如果个数大于1
                    //5.取出当前key分数从小到大，0-（N-1）的元素
                    //遍历元素
                    //如果元素update_time大于当前时间+10小时，则删除该元素对应行程的所有行程数据
                    //            //删除当前行程缓存
//            redisCache.del("htwx_gps:"+deviceUId+"|"+acconTimeSec);
//            redisCache.del("htwx_fault_code:"+deviceUId+"|"+acconTimeSec);
//            redisCache.del("htwx_alarm:"+deviceUId+"|"+acconTimeSec);
//            redisCache.del("htwx_snap:"+deviceUId+"|"+acconTimeSec);

                    Date curDate = new Date();
                    //1.读取所有 htwx_processed_trip:开头的缓存key
                    DataPackage keysData = redisCache.getData(RedisCache.OPTION_KEYS,"htwx_processed_trip:*");
                    if (keysData!=null) {
                        Set<String> keys = (Set<String>) keysData.getData();
                        String key = null;
                        DataPackage tempData = null;
                        Set<byte[]> tempBytes = null;
                        Map<String,Object> trip = null;
                        Date updateTime = null;
                        String deviceUId = null;
                        Long acconTimeSec = null;
                        //2.循环遍历key
                        for (Iterator<String> it = keys.iterator();it.hasNext();) {
                            key = it.next();
                            //3.查看key包含的元素的个数
                            tempData = redisCache.getData(RedisCache.OPTION_ZCARD, key);
                            //4.如果个数大于1
                            if (tempData!=null&&(Long)tempData.getData()>1){
                                //5.取出当前key分数从小到大，0-（N-1）的元素
                                tempData = redisCache.getData(RedisCache.OPTION_ZRANGE,key,0,-2);
                                //遍历元素
                                if (tempData!=null) {
                                    tempBytes = (Set<byte[]>) tempData.getData();
                                    for (Iterator<byte[]> it2 = tempBytes.iterator();it2.hasNext();) {
                                        trip = SerializeUtil.unserialize(it2.next(),Map.class);
                                        //如果元素update_time小于当前时间+10小时，则删除该元素对应行程的所有行程数据
                                        updateTime = (Date) trip.get("update_time");
                                        if (updateTime.getTime()+ maxLifeSec*1000 <curDate.getTime()) {
                                            deviceUId = (String) trip.get("device_uid");
                                            acconTimeSec = ((Date)trip.get("start_time")).getTime()/1000;
                                            redisCache.del("htwx_gps:"+deviceUId+"|"+acconTimeSec);
                                            redisCache.del("htwx_fault_code:"+deviceUId+"|"+acconTimeSec);
                                            redisCache.del("htwx_alarm:"+deviceUId+"|"+acconTimeSec);
                                            redisCache.del("htwx_snap:"+deviceUId+"|"+acconTimeSec);
                                            //从htwx_processed_trip:{device_uid}删除
//                                            redisCache.zrem(key,trip);
                                            redisCache.zremrangeByScore(key,acconTimeSec,acconTimeSec);
                                            log.debug("缓存管理移出行程缓存，tripId:{}",trip.get("id"));
                                        }
                                    }
                                }

                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("缓存管理处理失败！",e);
                }
                try {
                    Thread.sleep(intervalsSec*1000);
                } catch (Exception e) {
                    log.info("缓存管理处理器休眠终端",e);
                }
            }


        }

        public RedisCache getRedisCache() {
            return redisCache;
        }

        public void setRedisCache(RedisCache redisCache) {
            this.redisCache = redisCache;
        }



    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public Map<String, List<String>> getTaskMap() {
        return taskMap;
    }

    public void setTaskMap(Map<String, List<String>> taskMap) {
        this.taskMap = taskMap;
    }

    public int getTaskMaxNum() {
        return taskMaxNum;
    }

    public void setTaskMaxNum(int taskMaxNum) {
        this.taskMaxNum = taskMaxNum;
    }
}
