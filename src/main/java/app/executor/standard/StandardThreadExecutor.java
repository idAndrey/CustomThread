package app.executor.standard;

import app.Application;
import app.executor.custom.CustomThreadExecutor;
import app.executor.factory.CustomExecutor;
import app.executor.factory.CustomThreadFactory;
import app.taskjob.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class StandardThreadExecutor implements CustomExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StandardThreadExecutor.class);

    private final Properties config;

    ThreadPoolExecutor executor;
    BlockingQueue<Runnable> workQueue;

    ThreadFactory threadFactory = new CustomThreadFactory();

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
    public void execute(Runnable command) {
//        executor.execute(command);
        logger.info("Executor state: ActiveThreads={}, QueueSize={}, PoolSize={}",
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getPoolSize());
        logger.info("Queue: {}", executor.getQueue().stream()
                .map(r -> (Task)r)
//                .sorted((t1, t2) -> t2.getId() - t1.getId())
                .sorted(Comparator.comparingInt(Task::getId).reversed())
//                .map(task -> String.format("{id=%d, name='%s'}", task.getId(), task.getName()))
//                .collect(Collectors.joining(", ", "[", "]")));
                .map(task -> "{" + task.getId() + "}")
                .collect(Collectors.joining(", ", "[", "]")));
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
