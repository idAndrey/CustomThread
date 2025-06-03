package app.executor.standard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class StandardThreadFactory implements ThreadFactory {

    private static final Logger logger = LoggerFactory.getLogger(StandardThreadFactory.class);

    private final ThreadFactory defaultThreadFactory;// = Executors.defaultThreadFactory();

    //thread creation counter
    private final AtomicLong counter = new AtomicLong(0);

    private String prefix = "";

    public StandardThreadFactory(String prefix) {
        this.prefix = prefix;
        this.defaultThreadFactory = Executors.defaultThreadFactory();
    }

    @Override
    public Thread newThread(Runnable r) {

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
