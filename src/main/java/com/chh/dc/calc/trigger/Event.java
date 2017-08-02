package com.chh.dc.calc.trigger;

import java.util.Map;
import java.util.UUID;

/**
 * Created by niow on 16/10/7.
 */
public class Event {

    private int id;

    private String type;

    private Map<String, Object> params;

    public Event() {
        this.id = UUID.randomUUID().hashCode();
    }

    public Event(String type, Map<String, Object> params) {
        this();
        this.type = type;
        this.params = params;

    }

    public int getId() {
        return id;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
