package com.chh.dc.calc.task;

import com.chh.dc.calc.trigger.Event;

import java.util.concurrent.Callable;

/**
 * Created by niow on 16/10/7.
 */
public abstract class Task implements Callable<TaskFuture>{

    private long id;

    private String name;

    private Event event;

    private boolean singleton;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
