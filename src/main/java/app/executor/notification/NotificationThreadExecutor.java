package app.executor.notification;

import app.executor.factory.CustomExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.*;

public class NotificationThreadExecutor implements CustomExecutor {
    private static final Logger logger = LoggerFactory.getLogger(NotificationThreadExecutor.class);

//    private final Properties config;

    ExecutorService executor;
    BlockingQueue<Runnable> workQueue;

    public NotificationThreadExecutor() {
        this.workQueue = new SynchronousQueue<>();
        this.executor = Executors.newCachedThreadPool();
    }

    public ExecutorService getExecutor() {
        return  this.executor;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        executor.execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        logger.info("[Standard Executor] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
        logger.info("[Standard Executor] Immediate shutdown initiated.");
    }
}
