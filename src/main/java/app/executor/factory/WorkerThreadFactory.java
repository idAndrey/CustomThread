package app.executor.factory;

import app.executor.custom.CustomThreadExecutor;
import app.executor.custom.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WorkerThreadFactory implements WorkerFactory{


    private static final Logger logger = LoggerFactory.getLogger(WorkerThreadFactory.class);

    private final AtomicLong internalCounter;
    private String prefix = "";

    private CustomThreadExecutor pool;
    private int queueSize;
    private
    int workerId;
    private long keepAliveTime;
    private TimeUnit timeUnit;

    public WorkerThreadFactory(String prefix) {
        this.prefix = prefix;
        this.internalCounter = new AtomicLong(0);
    }

    public void setupFactory(    CustomThreadExecutor pool,
                                 int queueSize,
                                 long keepAliveTime,
                                 TimeUnit timeUnit){
        this.pool = pool;
        this.queueSize = queueSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;

    }

    @Override
    public Worker newWorker() {

        BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(queueSize);
        Worker worker = new Worker(pool, taskQueue, pool.workerCountIncrementAndGet(), keepAliveTime,timeUnit, prefix);
        logger.info("New worker thread {} is created.", worker.getWorkerName());
        return worker;
    }
}
