package com.sample.cosmos.vo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class User {
    private String id;
    private int userId;

    private Map<String, String> details = new HashMap<>();

    //private Set<Audience> audiences = new HashSet<>();

    //private Set<String> audiencesString = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    /*
    public Set<Audience> getAudiences() {
        return audiences;
    }

    public void setAudiences(Set<Audience> audiences) {
        this.audiences = audiences;
    }

    public void setAudiencesString(Set<String> audiencesString) {
        this.audiencesString = audiencesString;
    }

    public Set<String> getAudiencesString() {
        return audiencesString;
    }*/
}
