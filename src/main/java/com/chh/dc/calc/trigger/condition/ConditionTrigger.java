package com.chh.dc.calc.trigger.condition;

import com.chh.dc.calc.reader.DataReader;
import com.chh.dc.calc.trigger.Trigger;
import com.chh.dc.calc.trigger.TriggerManager;
import com.chh.dc.calc.reader.DataPackage;
import com.chh.dc.calc.trigger.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by niow on 16/10/7.
 */
public class ConditionTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(ConditionTrigger.class);

    protected TriggerManager triggerManager;

    private DataReader dataReader;

    private String name;

    /**
     * <eventType,condition>
     */
    private Map<String, Condition> conditionMap;

    private boolean keepRunning = true;

    /*
     从redis读取数据
     数据丢到condition中
     condition判断结果
     触发event
     condition对应type

     */

    public ConditionTrigger() {

    }

    public ConditionTrigger(TriggerManager triggerManager) {
        this.triggerManager = triggerManager;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public void setTriggerManager(TriggerManager triggerManager) {
        this.triggerManager = triggerManager;
    }

    @Override
    public void run() {
        while (keepRunning) {

            try {
                DataPackage dataPackage = dataReader.getData();
                if (dataPackage == null) {
                    continue;
                }
                trigger(dataPackage);
            } catch (Exception e ) {
                log.error("读取待汇总告警缓存出错",e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
                }
            }
        }
    }


    @Override
    public void trigger(DataPackage dataPackage) {

        for (Map.Entry<String, Condition> entry : conditionMap.entrySet()) {
            Condition condition = entry.getValue();
            Map<String, Object> params = condition.check(dataPackage);
            if (params == null) {
                continue;
            }
            String eventType = entry.getKey();
            Event event = new Event(eventType, params);
            try {
                triggerManager.addEvent(event);
            } catch (Exception e) {
                log.error("触发事件加入队列失败！",e);
            }
        }
    }

    public DataReader getDataReader() {
        return dataReader;
    }

    public void setDataReader(DataReader dataReader) {
        this.dataReader = dataReader;
    }

    public Map<String, Condition> getConditionMap() {
        return conditionMap;
    }

    public void setConditionMap(Map<String, Condition> conditionMap) {
        this.conditionMap = conditionMap;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
