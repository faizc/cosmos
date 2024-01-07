package com.sample.cosmos.vo;

public class UserAudience {
    private String id;
    private long userId;
    private long audience;

    public UserAudience() {
    }

    public UserAudience(long userId, long audience) {
        this.userId = userId;
        this.audience = audience;
    }

    public void setAudience(long audience) {
        this.audience = audience;
    }

    public long getAudience() {
        return audience;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
