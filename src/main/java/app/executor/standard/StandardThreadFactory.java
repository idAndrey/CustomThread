package app.executor.standard;

import app.executor.factory.CustomThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class StandardThreadFactory implements ThreadFactory {
    private static final Logger logger = LoggerFactory.getLogger(StandardThreadFactory.class);

    //    private final int workerQueueSize;
    private final ThreadFactory defaultThreadFactory;// = Executors.defaultThreadFactory();

    //thread creation counter
    private final AtomicLong counter = new AtomicLong(0);

    private String prefix = "";


    public StandardThreadFactory(int workerQueueSize, ThreadFactory defaultThreadFactory) {
//        this.workerQueueSize = workerQueueSize;
//        this.defaultThreadFactory = defaultThreadFactory;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    public StandardThreadFactory(int workerQueueSize) {
//        this.workerQueueSize = workerQueueSize;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    public StandardThreadFactory() {
//        this.workerQueueSize = workerQueueSize;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
//        this.counter = new AtomicLong(0);
    }

    public StandardThreadFactory(String prefix) {
//        this.workerQueueSize = workerQueueSize;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
//        this.counter = new AtomicLong(0);
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {

//        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(workerQueueSize);

        // Добавляем логирование при завершении потока
        Runnable wrappedRunnable = () -> {
            Thread currentThread = Thread.currentThread();
            if(!prefix.isEmpty()) {
                currentThread.setName(this.prefix + counter.incrementAndGet());
            }
            try {
                logger.debug("Thread [{}] started", currentThread.getName());
                r.run();
                logger.debug("Thread [{}] completed successfully", currentThread.getName());
            } catch (Throwable t) {
                logger.error("Thread [{}] failed with exception", currentThread.getName(), t);
            } finally {
                logger.info("Thread [{}] is shutting down", currentThread.getName());
            }
        };

        Thread thread = defaultThreadFactory.newThread(wrappedRunnable);

        thread.setUncaughtExceptionHandler((t, e) ->
                logger.error("Thread [{}] terminated due to uncaught exception", t.getName(), e)
        );

        return thread;
    }
}
