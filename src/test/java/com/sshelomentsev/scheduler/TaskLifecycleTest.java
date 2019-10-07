package com.sshelomentsev.scheduler;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class TaskLifecycleTest {

    @Test
    public void runCorrectTask() {
        Task t = new Task(LocalDateTime.now(), () -> null);

        Assert.assertEquals("Task must have created state", TaskState.CREATED, t.getState());
        t.run();
        Assert.assertEquals("Task must have finished state", TaskState.FINISHED, t.getState());
    }

    @Test
    public void runTaskWithRuntimeException() {
        Task t = new Task(LocalDateTime.now(), () -> {
            long a = 1 / 0;
            return null;
        });

        Assert.assertEquals("Task must have created state", TaskState.CREATED, t.getState());
        t.run();
        Assert.assertEquals("Task must have failed state", TaskState.FAILED, t.getState());
    }

}
