package app.executor.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class CustomThreadFactory implements ThreadFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomThreadFactory.class);

//    private final int workerQueueSize;
    private final ThreadFactory defaultThreadFactory;// = Executors.defaultThreadFactory();

    public CustomThreadFactory(int workerQueueSize, ThreadFactory defaultThreadFactory) {
//        this.workerQueueSize = workerQueueSize;
//        this.defaultThreadFactory = defaultThreadFactory;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    public CustomThreadFactory(int workerQueueSize) {
//        this.workerQueueSize = workerQueueSize;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    public CustomThreadFactory() {
//        this.workerQueueSize = workerQueueSize;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    @Override
    public Thread newThread(Runnable r) {

//        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(workerQueueSize);

        // Добавляем логирование при завершении потока
        Runnable wrappedRunnable = () -> {
            Thread currentThread = Thread.currentThread();
            try {
                logger.debug("Thread {} started", currentThread.getName());
                r.run();
                logger.debug("Thread {} completed successfully", currentThread.getName());
            } catch (Throwable t) {
                logger.error("Thread {} failed with exception", currentThread.getName(), t);
            } finally {
                logger.info("Thread {} is shutting down", currentThread.getName());
            }
        };

        Thread thread = defaultThreadFactory.newThread(wrappedRunnable);

        thread.setUncaughtExceptionHandler((t, e) ->
                logger.error("Thread {} terminated due to uncaught exception", t.getName(), e)
        );

        return thread;
    }
}
