package com.sshelomentsev.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class Task extends TimerTask {

    private final LocalDateTime time;
    private final Callable callable;

    public Task(LocalDateTime time, Callable callable) {
        this.time = time;
        this.callable = callable;
    }

    public long getTimestamp() {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public void run() {
        try {
            callable.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
