package com.sample.cosmos.vo;

import java.util.HashSet;

public class UserAudienceInfo {
    private String id;
    private int userId;
    private HashSet<Integer> audiences = new HashSet<>();

    public void setId(String id) {
        this.id = id;
    }

    public void setAudiences(HashSet<Integer> audiences) {
        this.audiences = audiences;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public HashSet<Integer> getAudiences() {
        return audiences;
    }

    public String getId() {
        return id;
    }
}
