package com.sshelomentsev.scheduler;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ShutdownTest extends SchedulerTestCase {

    private boolean task1Marker = false;
    private boolean task2Marker = false;

    @Test
    public void scheduleAndCallShutdown() throws InterruptedException {
        LocalDateTime d1 = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(1);
        LocalDateTime d2 = d1.plusSeconds(5);

        scheduler.schedule(d1, () -> {
            task1Marker = true;
            return null;
        });

        scheduler.schedule(d2, () -> {
            task2Marker = true;
            return null;
        });

        Thread.sleep(3000);
        scheduler.shutdown();
        Thread.sleep(4000);

        Assert.assertTrue("First task must be executed", task1Marker);
        Assert.assertFalse("Second task must not be execution", task2Marker);
    }

}
