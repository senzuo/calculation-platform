package com.chh.dc.calc.db.pojo;

import java.io.Serializable;

public class TDtc implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -7990531571386597679L;

    private Integer id;
    private String value;
    private Integer type;
    private String description;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
