package com.jakduk.model.db;

import com.jakduk.common.CommonConst;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

/**
 * Created by pyohwan on 16. 1. 10.
 */

@Document
public class JakduScheduleGroup {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private String id;

    private int seq;

    private CommonConst.JAKDU_GROUP_STATE state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public CommonConst.JAKDU_GROUP_STATE getState() {
        return state;
    }

    public void setState(CommonConst.JAKDU_GROUP_STATE state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "JakduScheduleGroup{" +
                "id='" + id + '\'' +
                ", seq=" + seq +
                ", state=" + state +
                '}';
    }
}
