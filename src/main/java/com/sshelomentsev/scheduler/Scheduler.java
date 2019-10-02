package com.sshelomentsev.scheduler;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Scheduler {

    private ConcurrentHashMap<Long, CopyOnWriteArrayList<Task>> map = new ConcurrentHashMap<>();
    private final int executionThreadsNumber;
    private final SchedulerThread thread;

    /**
     * Creates a new scheduler
     * @param executionThreadsNumber Number of execution threads
     */
    public Scheduler(final int executionThreadsNumber) {
        this.executionThreadsNumber = executionThreadsNumber;
        this.thread = new SchedulerThread();
        thread.start();
    }

    /**
     * Creates a new single thread scheduler
     */
    public Scheduler() {
        this(1);
    }

    /**
     * Schedule a new task
     * @param dateTime time in UTC at which task is to be executed
     * @param callable callable to be executed
     */
    public void schedule(LocalDateTime dateTime, Callable callable) {
        Task task = new Task(dateTime, callable);
        map.computeIfAbsent(task.getTimestamp(), s -> new CopyOnWriteArrayList<>()).add(task);
    }

    /**
     * Discarding any currently scheduled tasks,
     * terminates this scheduler and all it's execution threads.
     */
    public void shutdown() {
        thread.cancel();
    }

    class SchedulerThread extends Thread {

        private static final int TIMEOUT = 500000;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition available = lock.newCondition();
        private volatile boolean newTasksMayBeScheduled = true;
        private Timer[] timers;
        private List<Task> currentQueue;

        SchedulerThread() {
            this.currentQueue = new CopyOnWriteArrayList<>();
            this.timers = new Timer[executionThreadsNumber];
            for (int i = 0; i < executionThreadsNumber; i++) {
                this.timers[i] = new Timer();
            }
        }

        public void run() {
            newTasksMayBeScheduled = true;
            try {
                mainLoop();
            } catch (InterruptedException ignored) {
            } finally {
                newTasksMayBeScheduled = false;
                currentQueue.clear();
            }
        }

        /**
         * Terminate timers, discarding any currently scheduled tasks
         */
        void cancel() {
            newTasksMayBeScheduled = false;
            currentQueue.clear();
        }

        /**
         * The main loop of scheduler thread
         * @throws InterruptedException
         */
        private void mainLoop() throws InterruptedException {
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            long prevTimestamp = System.currentTimeMillis();
            try {
                while (true) {
                    if (!newTasksMayBeScheduled) {
                        cancelTimers();
                        return;
                    }
                    if (!currentQueue.isEmpty()) {
                        for (int i = 0; i < executionThreadsNumber; i++) {
                            if (currentQueue.size() > 0) {
                                Task task = currentQueue.remove(0);
                                Date date = new Date(task.getTimestamp());
                                timers[i].schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        task.run();
                                    }
                                }, date);
                            }
                        }
                    }
                    if (currentQueue.size() < executionThreadsNumber) {
                        long currTimestamp = System.currentTimeMillis();
                        for (long ts = prevTimestamp + 1; ts <= currTimestamp; ts++) {
                            currentQueue.addAll(map.getOrDefault(ts, new CopyOnWriteArrayList<>()));
                            map.remove(ts);
                        }
                        prevTimestamp = currTimestamp;
                    }
                    available.awaitNanos(TIMEOUT);
                }
            } catch (Exception ignored) {
            } finally {
                lock.unlock();
            }
        }

        private void cancelTimers() {
            for (int i = 0; i < executionThreadsNumber; i++) {
                timers[i].cancel();
            }
        }

    }

}

