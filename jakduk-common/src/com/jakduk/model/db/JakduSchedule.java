package com.jakduk.model.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by pyohwan on 15. 12. 23.
 */

@Document
public class JakduSchedule {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private String id;

    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp date;

    private FootballClubOrigin home;

    private FootballClubOrigin away;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public FootballClubOrigin getHome() {
        return home;
    }

    public void setHome(FootballClubOrigin home) {
        this.home = home;
    }

    public FootballClubOrigin getAway() {
        return away;
    }

    public void setAway(FootballClubOrigin away) {
        this.away = away;
    }

    @Override
    public String toString() {
        return "JakduSchedule{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", home=" + home +
                ", away=" + away +
                '}';
    }
}