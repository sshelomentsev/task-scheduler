package com.sshelomentsev.scheduler;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Scheduler extends TimerTask {

    private ConcurrentHashMap<Long, CopyOnWriteArrayList<Task>> map = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;
    private AtomicLong timestamp = new AtomicLong(System.currentTimeMillis());
    private Timer[] timers;
    private List<Task> currentQueue;
    private final int threadsNumber;

    public Scheduler(final int threadsNumber) {
        this.threadsNumber = threadsNumber;
        this.timers = new Timer[threadsNumber];
        for (int i = 0; i < threadsNumber; i++) {
            this.timers[i] = new Timer();
        }
    }

    public Scheduler() {
        this(1);
    }

    public void addTask(Task task) {
        map.computeIfAbsent(task.getTimestamp(), s -> new CopyOnWriteArrayList<>()).add(task);
    }

    @Override
    public void run() {
        if (!isRunning) {
            isRunning = true;
            if (currentQueue.size() > 0) {
                Date date = new Date(timestamp.get());
                for (int i = 0; i < threadsNumber; i++) {
                    if (currentQueue.size() > 0) {
                        Task task = currentQueue.remove(0);
                        System.out.println("schedule " + task.toString() + "at " + System.currentTimeMillis());
                        timers[i].schedule(new TimerTask() {
                            @Override
                            public void run() {
                                task.run();
                            }
                        }, date);
                    }
                }
            }
            if (currentQueue.size() < threadsNumber) {
                long ts = timestamp.incrementAndGet();
                currentQueue.addAll(map.getOrDefault(ts, new CopyOnWriteArrayList<>()));
                map.remove(ts);
            }

            isRunning = false;
        }
    }
}
