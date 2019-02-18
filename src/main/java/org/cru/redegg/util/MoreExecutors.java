package org.cru.redegg.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MoreExecutors
{
    private MoreExecutors() {}

    /**
     * Shuts down the given executor service,
     * and handles an InterruptedException if the thread is interrupted.
     * This is intended to be called in {@code @PreDestroy} methods.
     */
    public static void shutdownAndHandleInterruptions(
        ExecutorService executorService,
        int timeoutInSeconds,
        final String executorDescription,
        ErrorLog errorLog)
    {
        executorService.shutdown();
        try
        {
            TimeUnit timeUnit = TimeUnit.SECONDS;
            boolean completed = executorService.awaitTermination(timeoutInSeconds, timeUnit);
            if (!completed)
            {
                final String message =
                    "unable to shut down " + executorDescription + " executor within " +
                    timeoutInSeconds + " " + timeUnit.name().toLowerCase();
                errorLog.warn(message);
            }
        }
        catch (InterruptedException e)
        {
            errorLog.error(executorDescription + " executor shutdown interrupted", e);
            // Lifecycle interceptor methods may not throw checked exceptions.
            // Throwing an unchecked exception may prevent other cleanup methods from running entirely.
            // So to preserve the interruption, we have to set the interrupt flag.
            Thread.currentThread().interrupt();

            // attempt a rapid shutdown, which will probably propagate the interruption to each task
            final List<Runnable> preemptedTasks = executorService.shutdownNow();
            errorLog.warn(preemptedTasks.size() + " tasks were preempted");
        }

    }
}
