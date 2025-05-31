package app.executor.custom;

import app.Application;


import app.executor.factory.CustomExecutor;
import app.taskjob.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class CustomThreadExecutor implements CustomExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CustomThreadExecutor.class);

    private final Properties config;

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

//    BalanceStrategy balanceStrategy = BalanceStrategy.ROUND_ROBIN;
//    RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;
    BalanceStrategy balanceStrategy;
    RejectionPolicy rejectionPolicy;

    private final List<Worker> workers = new CopyOnWriteArrayList<>();

    private final AtomicInteger workerCount = new AtomicInteger(0);
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

//    private final ExecutorStrategy executorStrategy;

    private volatile boolean isShutdown = false;

    public CustomThreadExecutor() {
        this.config = getConfig("executor.properties");

        this.corePoolSize = Integer.parseInt(config.getProperty("corePoolSize"));
        this.maxPoolSize = Integer.parseInt(config.getProperty("maxPoolSize"));
        this.queueSize = Integer.parseInt(config.getProperty("queueSize"));
        this.keepAliveTime = Long.parseLong(config.getProperty("keepAliveTime"));
        this.timeUnit = TimeUnit.valueOf(config.getProperty("timeUnit"));
        this.minSpareThreads = Integer.parseInt(config.getProperty("minSpareThreads"));
        this.balanceStrategy = BalanceStrategy.valueOf(config.getProperty("balanceStrategy"));
        this.rejectionPolicy = RejectionPolicy.valueOf(config.getProperty("rejectionPolicy"));

        for (int i = 0; i < corePoolSize; i++) {
            workers.add(newWorker());
        }
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

    public CustomThreadExecutor(int corePoolSize, int maxPoolSize, int queueSize, long keepAliveTime, TimeUnit timeUnit,
                             int minSpareThreads, BalanceStrategy balanceStrategy, RejectionPolicy rejectionPolicy) {

        this.config = getConfig("executor.properties");

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.balanceStrategy = balanceStrategy;
        this.rejectionPolicy = rejectionPolicy;
//        this.executorStrategy = new ExecutorStrategy();

        for (int i = 0; i < corePoolSize; i++) {
            workers.add(newWorker());
        }
    }

    private Worker newWorker() {
        if (workers.size() >= maxPoolSize) return null;
//        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize);
//        String workerName = "MyPool-worker-" + workerCount.incrementAndGet();
        int workerId = workerCount.incrementAndGet();
        Worker worker = new Worker(this, queue, workerId, keepAliveTime, timeUnit);
//        BlockingQueue<Runnable> taskQueue, int workerId, long keepAliveTime, TimeUnit timeUnit
//        System.out.println("[ThreadFactory] Creating new thread: " + worker.getName());
        worker.start();
        logger.info("[ThreadFactory] New worker thread {} is started.", worker.getWorkerName());
        return worker;
    }

    public int getWorkerCount() {
//        return workers.size();
        return workerCount.get();

    }


    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPool is shutdown.");
        }

        int idleCount = 0;
        for (Worker w : workers) {
            if (w.isRunning() && w.isIdle()) idleCount++;
            if(!w.isRunning()) {
                workerCount.decrementAndGet();
                workers.remove(w);
            }
        }
        if (idleCount < minSpareThreads && workers.size() < maxPoolSize) {
            workers.add(newWorker());
        }

        if(workers.isEmpty()) workers.add(newWorker());

        if (!workers.isEmpty()) {
            int index = roundRobinCounter.getAndIncrement() % workers.size();
            Worker worker = workers.get(index);

            System.out.println("\nRound Robin index = " + index +
                                " worker = " + (index + 1) +
                                " isRunning = " + worker.isRunning() +
                                "\n");
//            executorStrategy.execute();0




            if(worker.isRunning()) {

                boolean offered = worker.offerTaskWrapped(command);
                if (offered) {
                    logger.info("[Pool] Task accepted into queue #{}: {}", index, command.toString());

//                    System.out.println("[Pool] Queue #" + index + ": " + worker.getTaskQueue().toString());
                    System.out.println("[Pool] Queue #" + index + ": " + worker.getTaskQueue().stream()
                            .map(r -> (Task)r)
                            .sorted(Comparator.comparingInt(Task::getId).reversed())
                            .map(task -> "{" + task.getId() + "}")
                            .collect(Collectors.joining(", ", "[", "]")));
                } else {
                    switch (rejectionPolicy) {
                        case CALLER_RUNS_POLICY -> {
                            logger.info("[Rejected] Task {} was rejected due to overload! Executing in caller thread.", command.toString());
                            command.run();
//                            throw new RejectedExecutionException("Queue #" + index + " is overloaded. Task was rejected.");
                        }
                        case DISCARD_POLICY -> {
                            logger.info("[Discarded] Task {} was discarded due to overload!", command.toString());
                            throw new DiscardedExecutionException("Queue #" + index + " is overloaded. Task was discarded.");
                        }
                        case ABORT_POLICY -> {
                            logger.info("[Aborted] Task {} was aborted due to overload!", command.toString());
//                            throw new DiscardedExecutionException("Queue #" + index + " is overloaded. Task was aborted.");
                            throw new RejectedExecutionException("Queue #" + index + " is overloaded. Task was rejected.");
                        }

                    }


                    //                System.out.println("[Rejected] Task " + command.toString() +
                    //                        " was rejected due to overload! Executing in caller thread.");

                }
            } else {
                logger.error("Thread {} is not running! RoundRobin Index = {}.", worker.getWorkerName(), index);

            }
        }
//        else {
//            workers.add(newWorker());
//            Worker worker = workers.getFirst();
//            boolean offered = worker.offerTask(command);
//            if (offered) {
//                System.out.println("[Pool] Task accepted into queue #0: " + command.toString());
//            } else {
//                System.out.println("[Rejected] Task " + command.toString() +
//                        " was rejected due to overload! Executing in caller thread.");
//                command.run();
//            }
//        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {

        // пройти список WORKERS и начать SHUTDOWN

        isShutdown = true;

        logger.info("[Pool] Shutdown initiated.");

        for (Worker worker : workers) {
            worker.interrupt();
        }

        // Сделать JOIN
    }

    @Override
    public void shutdownNow() {

        // пройти список WORKERS и принудительно ЗАВЕРШИТЬ SHUTDOWN

        isShutdown = true;

        logger.warn("[Pool] Immediate shutdown initiated.");

        for (Worker worker : workers) {
            worker.interrupt();
            worker.clearQueue();
        }

        // Сделать JOIN

    }

    public static CustomThreadExecutor getCustomThreadExecutor(){
        Properties config = new Properties();
        try (InputStream is = Application.class.getClassLoader()
                .getResourceAsStream("executor.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {

            logger.error("Failed to get executor settings: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        CustomThreadExecutor executor = new CustomThreadExecutor(
                Integer.parseInt(config.getProperty("corePoolSize")),
                Integer.parseInt(config.getProperty("maxPoolSize")),
                Integer.parseInt(config.getProperty("queueSize")),
                Long.parseLong(config.getProperty("keepAliveTime")),
                TimeUnit.valueOf(config.getProperty("timeUnit")),
                Integer.parseInt(config.getProperty("minSpareThreads")),
                BalanceStrategy.valueOf(config.getProperty("balanceStrategy")),
                RejectionPolicy.valueOf(config.getProperty("rejectionPolicy"))
        );

        return executor;
    }
}
