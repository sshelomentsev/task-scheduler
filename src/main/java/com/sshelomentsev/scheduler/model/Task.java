package com.sshelomentsev.scheduler.model;

import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;

/**
 * This class is an internal representation of <LocalDateTime, Callable> pair
 */
public class Task {

    private static final Logger logger = Logger.getLogger(Task.class);

    private final LocalDateTime time;
    private final Callable callable;

    public Task(LocalDateTime time, Callable callable) {
        this.time = time;
        this.callable = callable;
    }

    public long getTimestamp() {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public void run(){
        try {
            callable.call();
        } catch (Exception e) {
            logger.error("Task failed during execution", e);
        }
    }

}
