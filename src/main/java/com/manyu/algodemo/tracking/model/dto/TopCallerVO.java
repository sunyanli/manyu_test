package com.manyu.algodemo.tracking.model.dto;

/**
 * 调用最多的人视图对象（姓名脱敏）。
 */
public class TopCallerVO {

    /** 姓名（脱敏）。 */
    private String name;
    /** 调用次数。 */
    private long calls;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }
}
