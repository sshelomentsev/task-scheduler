package com.sshelomentsev.app;

import com.sshelomentsev.scheduler.Scheduler;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class App {

    private static final Logger logger = Logger.getLogger(App.class);

    private static final Scheduler scheduler = new Scheduler();

    public static void main(String... args) {
        LocalDateTime curr = LocalDateTime.now(ZoneId.of("UTC"));

        scheduler.schedule(curr.plusNanos(100_000_000L), () -> {
            logger.info("FIRST TASK");
            return null;
        });

        Thread t0 = new Thread(() -> {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
            }
            scheduler.schedule(curr.plusSeconds(3), () -> {
                logger.info("TASKX");
                return null;
            });
        });
        t0.start();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                String name = "Task(1) " + i;
                scheduler.schedule(curr.plusSeconds(3), () -> {
                    logger.info("Start " + name);
                    logger.info("Finish " + name);
                    return null;
                });
            }
        });
        t1.start();

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                String name = "Task(2) " + i;
                scheduler.schedule(curr.plusSeconds(5), () -> {
                    logger.info("Start " + name);
                    Thread.sleep(300);
                    logger.info("Finish " + name);
                    return null;
                });
            }
        });
        t2.start();

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(40000);
                logger.info("Call scheduler shutdown");
                scheduler.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

}
