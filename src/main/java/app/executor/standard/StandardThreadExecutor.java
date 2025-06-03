package app.executor.standard;

import app.Application;

import app.executor.factory.CustomExecutorStatistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class StandardThreadExecutor implements CustomExecutorStatistic {

    private static final Logger logger = LoggerFactory.getLogger(StandardThreadExecutor.class);

    private final Properties config;

    ThreadPoolExecutor executor;
    BlockingQueue<Runnable> workQueue;

    private final AtomicInteger maxQueueSize;

    ThreadFactory threadFactory = new StandardThreadFactory("Worker-");

    public StandardThreadExecutor(){

        this.config = getConfig("executor.properties");

        workQueue = new ArrayBlockingQueue<>(Integer.parseInt(config.getProperty("queueSize")));
        executor = new ThreadPoolExecutor(
                Integer.parseInt(config.getProperty("corePoolSize")),
                Integer.parseInt(config.getProperty("maxPoolSize")),
                Long.parseLong(config.getProperty("keepAliveTime")),
                TimeUnit.valueOf(config.getProperty("timeUnit")),
                workQueue,
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );

        this.maxQueueSize = new AtomicInteger(0);
    }

    private Properties getConfig(String fileName){
        Properties config = new Properties();
        try (InputStream is = Application.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (is != null) {
                config.load(is);
            }

        } catch (IOException e) {

            logger.error("Failed to get executor settings: {}", e.getMessage());
            e.printStackTrace();
//            System.exit(1);
        }
        return config;
    }

    @Override
    public int getTotalQueueSize(){
        return maxQueueSize.get();
    }

    @Override
    public void execute(Runnable command) {

        int currentQueueSize = executor.getQueue().size();

        if(maxQueueSize.get() < currentQueueSize){
            maxQueueSize.set(currentQueueSize);
        };

//        logger.info("Executor state: ActiveThreads={}, QueueSize={}, PoolSize={}",
//                executor.getActiveCount(),
//                executor.getQueue().size(),
//                executor.getPoolSize());
//        logger.info("Queue: {}", executor.getQueue().stream()
//                .map(r -> (Task)r)
//                .sorted(Comparator.comparingInt(Task::getId).reversed())
//                .map(task -> "{" + task.getId() + "}")
//                .collect(Collectors.joining(", ", "[", "]")));

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
//        logger.info("[Standard Executor] Shutdown initiated.");
        logger.info("[Standard Executor] Shutdown initiated. MaxQueueSize = {}", maxQueueSize.get());
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
        logger.info("[Standard Executor] Immediate shutdown initiated.");
    }


}
