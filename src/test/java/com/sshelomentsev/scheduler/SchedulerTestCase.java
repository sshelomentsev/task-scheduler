package com.sshelomentsev.scheduler;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SchedulerTestCase {

    protected static Scheduler scheduler;

    @BeforeClass
    public static void setUp() {
        scheduler = new Scheduler();
    }

    @AfterClass
    public static void tearDown() {
        scheduler.shutdown();
    }

}
