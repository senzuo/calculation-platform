package com.chh.dc.calc.trigger;

import com.chh.dc.calc.reader.DataPackage;

/**
 * Created by niow on 16/10/7.
 */
public interface Trigger extends Runnable{

    /**
     * 触发事件,触发以后调用trggerManager.addEvent向队列中注入
     */
    public void trigger(DataPackage dataPackage);

    public String getName();

//    public void setTriggerManager(TriggerManager triggerManager);
}
