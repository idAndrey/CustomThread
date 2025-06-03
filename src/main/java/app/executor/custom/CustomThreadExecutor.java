package app.executor.custom;

import app.Application;

import app.executor.factory.CustomExecutorStatistic;

import app.executor.factory.CustomThreadFactory;
import app.executor.factory.WorkerFactory;
import app.executor.factory.WorkerThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class CustomThreadExecutor implements CustomExecutorStatistic {

    private static final Logger logger = LoggerFactory.getLogger(CustomThreadExecutor.class);

    private final Properties config;

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    BalanceStrategy balanceStrategy;
    RejectionPolicy rejectionPolicy;

    private final String workerThreadNamePrefix;

    private final WorkerThreadFactory workerFactory;

    private final List<Worker> workers = new CopyOnWriteArrayList<>();

    private final AtomicInteger workerCount = new AtomicInteger(0);
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    private final AtomicInteger maxPoolQueueSize;

    private final ConcurrentHashMap<Integer, Integer> mapWorkerQueue;

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

        this.workerThreadNamePrefix = config.getProperty("workerThreadNamePrefix", "CustomPool-worker-");
        this.workerFactory = new WorkerThreadFactory(workerThreadNamePrefix);

        workerFactory.setupFactory(this, queueSize, keepAliveTime, timeUnit);

        this.maxPoolQueueSize = new AtomicInteger(0);
        this.mapWorkerQueue = new ConcurrentHashMap<>();

        for (int i = 0; i < corePoolSize; i++) {
            workers.add(addWorker());
        }
    }

    public int getTotalQueueSize(){
        return mapWorkerQueue.values().stream().mapToInt(Integer::intValue).sum();
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

    private Worker addWorker() {

        if (workers.size() >= maxPoolSize) return null;

//        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize);
//        int workerId = workerCount.incrementAndGet();
//        Worker worker = new Worker(this, queue, workerId, keepAliveTime, timeUnit);

        Worker worker = workerFactory.newWorker();

        mapWorkerQueue.put(worker.getWorkerId(), worker.getMaxWorkerQueueSize());

        worker.start();
//        logger.info("[ThreadFactory] New worker thread {} is started.", worker.getWorkerName());
        return worker;
    }

    public synchronized void checkMaxPoolQueueSize(int workerId, int maxWorkerQueueSize){
        if(maxPoolQueueSize.get() < maxWorkerQueueSize){
            maxPoolQueueSize.set(maxWorkerQueueSize);
        }
        mapWorkerQueue.put(workerId, maxWorkerQueueSize);
    }

    public synchronized int getWorkerCount() {
//        return workers.size();
        return workerCount.get();

    }
    public synchronized int workerCountIncrementAndGet(){
        return workerCount.incrementAndGet();
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
            workers.add(addWorker());
        }

        if(workers.isEmpty()) workers.add(addWorker());

        if (!workers.isEmpty()) {
            int index = roundRobinCounter.getAndIncrement() % workers.size();
            Worker worker = workers.get(index);

//            System.out.println("\nRound Robin index = " + index +
//                                " worker = " + (index + 1) +
//                                " isRunning = " + worker.isRunning() +
//                                "\n");

            if(worker.isRunning()) {

                boolean offered = worker.offerTaskWrapped(command);
                if (offered) {
                    logger.info("[Pool] Task accepted into queue #{}: {}", index, command.toString());

//                    System.out.println("[Pool] Queue #" + index + ": " + worker.getTaskQueue().stream()
//                            .map(r -> (Task)r)
//                            .sorted(Comparator.comparingInt(Task::getId).reversed())
//                            .map(task -> "{" + task.getId() + "}")
//                            .collect(Collectors.joining(", ", "[", "]")));
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
                }
            } else {
                logger.error("Thread {} is not running! RoundRobin Index = {}.", worker.getWorkerName(), index);

            }
        }
    }

    public synchronized void workerShutdown(Worker worker){
        int idleCount = 0;
        for (Worker w : workers) {
            if (w.isRunning() && w.isIdle()) idleCount++;
//            if(!w.isRunning()) {
//                workerCount.decrementAndGet();
//                workers.remove(w);
//            }
        }
        if(idleCount >= minSpareThreads){
            worker.clearQueue();
            worker.interrupt();
            workerCount.decrementAndGet();
            workers.remove(worker);

            StringBuilder stringOutput = new StringBuilder();
            stringOutput.append("Worker-" + worker.getWorkerId() + " is terminated.\n");
            stringOutput.append(workers.stream()
                    .map(w -> String.format("[%d - %s]",
                            w.getWorkerId(),
                            w.isRunning() ? "on" : "off"))
                    .collect(Collectors.joining(", ")));
            stringOutput.append("\nWorkerCount = " + getWorkerCount() + ", corePoolSize = " + corePoolSize + ".");
//            System.out.println(stringOutput.toString());
        }
        if (idleCount < minSpareThreads && workers.size() < maxPoolSize) {
            worker.setRunning(true);
            worker.start();
            StringBuilder stringOutput = new StringBuilder();
            stringOutput.append("Worker-" + worker.getWorkerId() + " is restarted due minSpareThreads.");
            stringOutput.append(workers.stream()
                    .map(w -> String.format("[%d - %s]",
                            w.getWorkerId(),
                            w.isRunning() ? "running" : "stopped"))
                    .collect(Collectors.joining(", ")));
//            System.out.println(stringOutput.toString());
        }
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

//        logger.info("[Pool] Shutdown initiated.");
        StringBuilder stringOutput = new StringBuilder();
        mapWorkerQueue.entrySet()
                .stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> {
                    stringOutput.append(
                            String.format("Worker-%d -> MaxWorkerQueueSize = %d%n",
                                    entry.getKey(), entry.getValue())
                    );
                });
        int totalQueueSize = mapWorkerQueue.values().stream().mapToInt(Integer::intValue).sum();
        stringOutput.append(String.format("Overall worker queues size = %d%n", totalQueueSize));

        logger.info("[Custom Executor] Shutdown initiated. MaxQueueSize = {}\n{}", maxPoolQueueSize.get(),stringOutput.toString());


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
}
