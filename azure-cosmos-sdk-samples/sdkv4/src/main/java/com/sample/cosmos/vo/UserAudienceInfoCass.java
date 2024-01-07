package com.sample.cosmos.vo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UserAudienceInfoCass {
    private long userId;
    private List<Integer> audiences = new ArrayList<>();

    public void setAudiences(List<Integer> audiences) {
        this.audiences = audiences;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public List<Integer> getAudiences() {
        return audiences;
    }

}
