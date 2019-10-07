package com.sshelomentsev.scheduler;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

public class SchedulingOrderTest extends SchedulerTestCase {

    private AtomicLong firstTaskTimestamp = new AtomicLong(0);
    private AtomicLong secondTaskTimestamp = new AtomicLong(0);

    @Test
    public void scheduleTwoTask() throws InterruptedException {
        LocalDateTime d1 = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(5);
        LocalDateTime d2 = d1.minusSeconds(2);
        Task t1 = new Task(d1, () -> {
            firstTaskTimestamp.set(System.currentTimeMillis());
            return null;
        });
        Task t2 = new Task(d2, () -> {
            secondTaskTimestamp.set(System.currentTimeMillis());
            return null;
        });

        scheduler.schedule(t1);
        scheduler.schedule(t2);

        Thread.sleep(5500);

        Assert.assertEquals("First task was not finished", TaskState.FINISHED, t1.getState());
        Assert.assertEquals("Second task was not finished", TaskState.FINISHED, t2.getState());
        Assert.assertTrue("Second task must be executed first", secondTaskTimestamp.get() < firstTaskTimestamp.get());
    }

}
