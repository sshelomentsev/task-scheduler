package com.sshelomentsev.scheduler;

import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;

/**
 * This class is an internal representation of <LocalDateTime, Callable> pair
 */
class Task {

    private static final Logger logger = Logger.getLogger(Task.class);

    private final LocalDateTime time;
    private final Callable callable;
    private volatile TaskState state;

    Task(LocalDateTime time, Callable callable) {
        this.time = time;
        this.callable = callable;
        this.state = TaskState.CREATED;
    }

    long getTimestamp() {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    TaskState getState() {
        return state;
    }

    void run() {
        try {
            state = TaskState.RUNNING;
            callable.call();
            state = TaskState.FINISHED;
        } catch (Exception e) {
            state = TaskState.FAILED;
            logger.error("Task failed during execution", e);
        }
    }

}
