package com.sample.cosmos.vo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Audience {
    private int id;
    private boolean expired;

    public Audience() {
    }

    public Audience(int id, boolean expired) {
        this.id = id;
        this.expired = expired;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                //.append(expired)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Audience) {
            final Audience employee = (Audience) obj;

            return new EqualsBuilder()
                    .append(id, employee.id)
                  //  .append(expired, employee.expired)
                    .isEquals();
        } else {
            return false;
        }
    }
}
