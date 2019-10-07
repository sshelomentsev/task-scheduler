package com.sshelomentsev.scheduler;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ExecutionOrderTest extends SchedulerTestCase {

    private static final int TASKS_COUNT = 10;

    private List<Integer> executionOrder = new ArrayList<>();

    /**
     * Schedule several task for the same time
     * and check that execution order is equal to scheduling order
     *
     * @throws InterruptedException
     */
    @Test
    public void test() throws InterruptedException {
        LocalDateTime d = LocalDateTime.now(ZoneId.of("UTC")).plusNanos(100_000_000);

        for (int i = 0; i < TASKS_COUNT; i++) {
            int finalI = i;
            scheduler.schedule(d, () -> {
                executionOrder.add(finalI);
                return null;
            });
        }
        Thread.sleep(1000);

        Assert.assertEquals("List of execution order must be equal to task count", TASKS_COUNT, executionOrder.size());
        Assert.assertTrue("Execution order is not equal to scheduling order", isNaturalOrder(executionOrder));
    }

    private static boolean isNaturalOrder(List<Integer> list) {
        Comparator<Integer> c = Comparator.comparingInt(o -> o);

        Iterator<Integer> it = list.iterator();
        Integer prev = it.next();
        Integer curr = prev;
        while (it.hasNext()) {
            curr = it.next();
            if (c.compare(prev, curr) >= 0) {
                return false;
            }
            prev = curr;
        }
        return true;
    }

}
