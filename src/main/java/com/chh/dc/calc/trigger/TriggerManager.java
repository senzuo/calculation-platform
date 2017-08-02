package com.chh.dc.calc.trigger;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by niow on 16/10/7.
 */
public class TriggerManager extends Thread {

    public static final Logger log = LoggerFactory.getLogger(TriggerManager.class);

//    private SynLinkedBuffer<Event> eventBuffer = new SynLinkedBuffer<>();

    private int eventQueueSize = 200; // 事件队列大小,初始化为200
    private BlockingQueue<Event> eventBuffer = new ArrayBlockingQueue<Event>(eventQueueSize);;

    private boolean keepRunning = false;

    private List<Trigger> triggerList;

    public TriggerManager() {

    }

    public void init() {
        loadTrigger();
    }

    private void loadTrigger() {
        for (Trigger trigger : triggerList) {
            log.info("加载触发器:" + trigger.getName());
            new Thread(trigger).start();
        }
    }

    @Override
    public void run() {
        super.run();
    }

    public Event takeEvent() throws InterruptedException {
        return eventBuffer.take();
    }

    public void addEvent(Event event) throws InterruptedException {
        eventBuffer.put(event);
    }

    public int eventSize(){
        return eventBuffer.size();
    }

    public List<Trigger> getTriggerList() {
        return triggerList;
    }

    public void setTriggerList(List<Trigger> triggerList) {
        this.triggerList = triggerList;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }
}
