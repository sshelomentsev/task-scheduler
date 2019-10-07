package com.sshelomentsev.scheduler;

import com.sshelomentsev.scheduler.model.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Task scheduler
 */
public class Scheduler {

    private ConcurrentSkipListMap<Long, CopyOnWriteArrayList<Task>> map = new ConcurrentSkipListMap<>();

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
        thread.newTaskAdded();
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
         * Wake up a thread that's waiting on the available condition
         */
        void newTaskAdded() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                available.signal();
            } catch (Exception ignored) {
            } finally {
                lock.unlock();
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
                        if (map.isEmpty()) {
                            available.await();
                        } else {
                            long currTimestamp = System.currentTimeMillis();

                            ConcurrentNavigableMap<Long, CopyOnWriteArrayList<Task>> newTasks
                                    = map.headMap(currTimestamp, true);
                            newTasks.values().forEach(v -> currentQueue.addAll(v));
                            newTasks.keySet().forEach(k -> map.remove(k));

                            if (currentQueue.isEmpty()) {
                                if (map.isEmpty()) {
                                    available.await();
                                } else {
                                    long awaitTimeout = map.firstKey() - currTimestamp;
                                    available.await(awaitTimeout, TimeUnit.MILLISECONDS);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                lock.unlock();
            }
        }

    }

}

