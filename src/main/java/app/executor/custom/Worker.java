package app.executor.custom;

import app.executor.custom.strategy.WorkerStrategies;
import app.taskjob.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
//    private final RunnableExecutor strategyExecutor;
    private final WorkerStrategies workerStrategies;

    public Worker(CustomThreadExecutor pool, BlockingQueue<Runnable> taskQueue, int workerId, long keepAliveTime, TimeUnit timeUnit) {
        super("Worker-" + String.valueOf(workerId));
        this.workerId = workerId;
        this.workerName = workerNamePrefix + String.valueOf(workerId);
        this.taskQueue = taskQueue;
        this.pool = pool;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
//        this.strategyExecutor = new RunnableExecutor();
        this.workerStrategies = new WorkerStrategies();

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
        try {
            while (isRunning) {
                isIdle = true;

                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                isIdle = false;

                if (task != null) {

                    if (!isRunning) {
                        break;
                    }

                    logger.info("{} executes {}", workerName, task.toString());

                    try {
                        workerStrategies.run(task);
                    } catch (Exception e) {
                        logger.debug("{} raise exception on task {}. Message: {}", workerName, task.toString(), e.getMessage());

                    }
                } else if (pool.getWorkerCount() > pool.getCorePoolSize())
                {
                    logger.info("{} idle timeout, stopping.", workerName);
                    isRunning = false;
                } else {
                    logger.info("{} idle timeout. Keep working by reason of corePoolSize.", workerName);
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
//    public BlockingQueue getTaskQueue(){
//
//        return taskQueue;
//    }
}

