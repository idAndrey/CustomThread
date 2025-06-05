package app.executor.custom;

import app.executor.custom.strategy.WorkerStrategies;
import app.executor.factory.CustomThreadFactory;
import app.taskjob.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final int workerId;
    private final String workerNamePrefix = "Worker-";
    private final String workerName;

    private final long keepAliveTime;
    private final TimeUnit timeUnit;

    private volatile boolean isRunning = true;
    private volatile boolean isIdle = true;

    private final CustomThreadExecutor pool;
    private final BlockingQueue<Runnable> taskQueue;
//    private final AtomicInteger maxWorkerQueueSize;
    private int maxWorkerQueueSize;

//    private final RunnableExecutor strategyExecutor;
    private final WorkerStrategies workerStrategies;

    public Worker(      CustomThreadExecutor pool,
                        BlockingQueue<Runnable> taskQueue,
                        int workerId,
                        long keepAliveTime,
                        TimeUnit timeUnit,
                        String workerNamePrefix) {
        super(workerNamePrefix + String.valueOf(workerId));
//        super(threadFactory);
        this.workerId = workerId;
        this.workerName = workerNamePrefix + String.valueOf(workerId);
        this.taskQueue = taskQueue;
        this.pool = pool;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
//        this.strategyExecutor = new RunnableExecutor();
        this.workerStrategies = new WorkerStrategies();
//        this.maxQueueSize = new AtomicInteger(0);
        this.maxWorkerQueueSize = 0;

    }

    public String getWorkerName() {
        return workerName;
    }
    public int getWorkerId() {
        return workerId;
    }

    public BlockingQueue<Runnable> getTaskQueue() {
         return this.taskQueue;
    }

    public boolean isIdle() {
        return isIdle && taskQueue.isEmpty();
    }

    public boolean isRunning() {
        return isRunning;
    }
    public void setRunning(boolean isRunning){
        this.isRunning = isRunning;
    }

    public boolean offerTaskWrapped(Runnable runnable) {
        Task task = (Task) runnable;
//        System.out.println("\n FROM offerTaskWrapped\ntaskId = " + task.getName() );

        boolean isOffered = workerStrategies.offerTask(this, task);

//        System.out.println("\nFROM offerTaskWRAPPED\n[" + this.workerName + "]" +
//                " Queue #" + (workerId - 1) + ": " + getTaskQueue().toString() + "\n");

        return isOffered;
    }

    public boolean offerTask(Runnable runnable) {

        return taskQueue.offer(runnable);
    }

    public void clearQueue() {
        taskQueue.clear();
    }

    @Override
    public void run() {
        isRunning = true;
        logger.info("{} is started", workerName);

//        int currentQueueSize = 0;

        try {
            while (isRunning) {
                isIdle = true;

                pool.checkMaxPoolQueueSize(workerId, checkMaxWorkerQueueSize(taskQueue.size()));

                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);

                int currentWorkerCount;

                if (task != null) {

                    if (!isRunning) {
                        break;
                    }

                    isIdle = false;

                    logger.info("{} executes {}", workerName, task.toString());

                    try {
                        workerStrategies.run(task);
                    } catch (Exception e) {
                        logger.debug("{} raise exception on task {}. Message: {}", workerName, task.toString(), e.getMessage());

                    }
                } else if ( (currentWorkerCount = pool.getWorkerCount()) > pool.getCorePoolSize())
                {
                    isRunning = false;
                    isIdle = true;
                    pool.workerShutdown(this);
                    logger.info("{} idle timeout, stopping. workerCount = {}, corePoolSize = {}.",
                            workerName, pool.getWorkerCount(), pool.getCorePoolSize());



                } else {
                    isIdle = true;
                    logger.info("{} idle timeout. Keep working by reason of corePoolSize. workerCount = {}, corePoolSize = {}.", workerName, currentWorkerCount, pool.getCorePoolSize());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("{} is interrupted.", workerName);
        } finally {
            logger.warn("{} is terminated.", workerName);
//            workers.remove(this);
        }
    }

    public int getMaxWorkerQueueSize(){
        return maxWorkerQueueSize;
    }
    public int checkMaxWorkerQueueSize(int currentQueueSize){

        if(maxWorkerQueueSize < currentQueueSize){
            maxWorkerQueueSize = currentQueueSize;
        }
        return maxWorkerQueueSize;
    }


}

