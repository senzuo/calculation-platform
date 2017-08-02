package com.chh.dc.calc.task;

import com.chh.dc.calc.trigger.Event;

/**
 * Created by niow on 16/10/7.
 */
public class TaskFuture {

    /** 执行成功 */
    public static final int TASK_CODE_SUCCESS = 0;

    /** 执行失败，需要重新执行 */
    public static final int TASK_CODE_FAILED = -1;

    /** 执行结果不完整 */
    public static final int TASK_CODE_INCOMPLETE = 1;

    private String cause; // 失败原因

    private Event event;

    private Task task;

    private int code;

    public TaskFuture(int code){
        this.code = code;
    }

    /**
     * 构建任务执行的结果对象
     *
     * @param code 操作结果码
     * @param cause 如果失败，可以填写一下原因
     * @param event 如果失败，可以填写一下参考对象
     */
    public TaskFuture(int code, String cause, Event event){
        super();
        this.cause = cause;
        this.event = event;
        this.code = code;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }


    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
