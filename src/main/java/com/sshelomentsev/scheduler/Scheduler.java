package com.sshelomentsev.scheduler;

import com.sshelomentsev.scheduler.model.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Task scheduler
 */
public class Scheduler {

    private ConcurrentHashMap<Long, CopyOnWriteArrayList<Task>> map = new ConcurrentHashMap<>();
    private final SchedulerThread thread;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new scheduler
     */
    public Scheduler() {
        this.thread = new SchedulerThread();
        thread.start();
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
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            thread.cancel();
        } finally {
            lock.unlock();
        }
    }

    class SchedulerThread extends Thread {

        private static final int timeout = 500_000;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition available = lock.newCondition();
        private volatile boolean newTasksMayBeScheduled = true;
        private List<Task> currentQueue = new CopyOnWriteArrayList<>();

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
         * Discarding any currently scheduled tasks
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
                        currentQueue.clear();
                        return;
                    }
                    if (!currentQueue.isEmpty()) {
                        Task task = currentQueue.remove(0);
                        task.run();
                    }
                    if (currentQueue.isEmpty()) {
                        long currTimestamp = System.currentTimeMillis();
                        for (long ts = prevTimestamp + 1; ts <= currTimestamp; ts++) {
                            currentQueue.addAll(map.getOrDefault(ts, new CopyOnWriteArrayList<>()));
                            map.remove(ts);
                        }
                        prevTimestamp = currTimestamp;
                    }
                    available.awaitNanos(timeout);
                }
            } catch (Exception ignored) {
            } finally {
                lock.unlock();
            }
        }

    }

}

