package app.taskjob;

import app.executor.custom.AbortedExecutionException;
import app.executor.custom.CustomThreadExecutor;
import app.executor.custom.DiscardedExecutionException;
import app.executor.factory.*;
import app.executor.notification.NotificationThreadExecutor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Job implements Callable, PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private final String jobName;
    private final long jobId;
    private final int taskCount;
    private final TimeUnit timeUnit;
    private final long duration;
    private final long interval;
    private final ExecutorType executorType;

//    public volatile JobStatus jobStatus;
    public final AtomicReference<JobStatus> jobStatus;

    private final long durationMS;
    private final long intervalMS;

    private List<Task> tasks;

    private CustomExecutorFactory taskExecutorFactory;
    private CustomThreadExecutor customThreadExecutor;
    private ExecutorService standardThreadExecutor;
    private CustomExecutorService customExecutorService;
//    private ExecutorType executorType;
    private CustomExecutor taskExecutor;

//    private final NotificationThreadExecutor notificationExecutor;
    private ThreadFactory notificationThreadFactory;
    private ExecutorService notificationExecutor;

    private final AtomicInteger startedTaskCount;
    private final AtomicInteger offeredTaskCount;
//    int offeredTaskCount = 0;
    private final AtomicInteger rejectedTaskCountAtomic;
    private final AtomicInteger completedTaskCountAtomic;
    private final AtomicInteger currentTaskCountAtomic;

    int rejectedTaskCount = 0;
    int abortedTaskCount = 0;
    int discardedTaskCount = 0;

    long startTime;
    long endTime;
    long totalTime;

    @JsonCreator
    public Job(@JsonProperty("jobName") String jobName,
               @JsonProperty("jobId") long jobId,
               @JsonProperty("taskCount") int taskCount,
               @JsonProperty("duration") long duration,
               @JsonProperty("interval") long interval,
               @JsonProperty("timeUnit") TimeUnit timeUnit,
               @JsonProperty("executorType") ExecutorType executorType
               ) {
        this.jobName = jobName;
        this.jobId = jobId;
        this.taskCount = taskCount;
        this.duration = duration;
        this.interval = interval;
        this.timeUnit = timeUnit;
        this.executorType = executorType;

//        this.jobStatus = JobStatus.NEW;
        this.jobStatus = new AtomicReference<>(JobStatus.NEW);

        this.durationMS = timeUnit.toMillis(duration);
        this.intervalMS = timeUnit.toMillis(interval);

        this.offeredTaskCount = new AtomicInteger(0);
        this.startedTaskCount = new AtomicInteger(0);
        this.rejectedTaskCountAtomic = new AtomicInteger(0);
        this.completedTaskCountAtomic = new AtomicInteger(0);
        this.currentTaskCountAtomic = new AtomicInteger(0);

        this.taskExecutorFactory = new CustomExecutorFactory();
//        this.notificationThreadFactory = new CustomThreadFactory();
        this.notificationThreadFactory = new CustomThreadFactory("NotificationThread-");

//        this.notificationExecutor = taskExecutorFactory.getExecutor(ExecutorType.NOTIFICATION);
//        this.notificationExecutor = (NotificationThreadExecutor) taskExecutorFactory.getExecutor(ExecutorType.NOTIFICATION);
        this.notificationExecutor = Executors.newCachedThreadPool(notificationThreadFactory);

        this.startTime = 0;
        this.endTime = 0;
        this.totalTime = 0;

        this.tasks = new ArrayList<>();
    }

    public void setTaskExecutor(ExecutorType executorType) {
        this.taskExecutor = taskExecutorFactory.getExecutor(executorType);
    }

    @Override
    public JobResult call() {

        for (int i = 1; i <= taskCount; i++) {
            int taskId = i;
            String taskName = "task";
            String taskDescription = "task description";

            Task task = new Task(jobId, taskId,taskName,taskDescription,timeUnit,duration, notificationExecutor);
            task.addPropertyChangeListener(this);
//            task.setState(TaskState.NEW);
//            task.setStatus(TaskStatus.NEW);

            tasks.add(task);
        }

        startTime = System.currentTimeMillis();
        logger.info("Job #{} started", jobName);

        for(Task task:tasks){
            try{
                switch (executorType){
                    case CUSTOM -> {
//                        customThreadExecutor.execute(task);
                        taskExecutor.execute(task);
                    }
                    case STANDARD -> {
//                        standardThreadExecutor.execute(task);
                        taskExecutor.execute(task);
                    }
                }

            } catch (AbortedExecutionException e){
                task.setStatus(TaskStatus.ABORTED);
                logger.warn("❌ Task #{} is aborted.", task.getName());
                abortedTaskCount++;
            } catch (RejectedExecutionException e){
                task.setStatus(TaskStatus.REJECTED);
                logger.warn("❌ Task #{} is rejected.", task.getName());
//                rejectedTaskCount++;
            } catch (DiscardedExecutionException e){
                task.setStatus(TaskStatus.DISCARDED);
                logger.warn("❌ Task #{} is discarded.", task.getName());
                discardedTaskCount++;
            }

            try {
                Thread.sleep(intervalMS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        while(!jobStatus.compareAndSet(JobStatus.COMPLETED, JobStatus.COMPLETED)){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        JobResult result = new JobResult(
                completedTaskCountAtomic.get(),
                rejectedTaskCountAtomic.get(),
                abortedTaskCount,
                discardedTaskCount,
                startTime,
                endTime,
                executorType
        );

        return result;
    }

    public void printTaskInfo() {
        String[] unitNames = new String[] { "ns", "us", "ms", "s", "min", "h", "d" };

        long totalTime = endTime - startTime;
        int executedTasks = taskCount - rejectedTaskCount - discardedTaskCount - abortedTaskCount;
        double avgTime = executedTasks > 0 ? (double) totalTime / executedTasks : 0.0;
        DecimalFormat df = new DecimalFormat("#.##");

        logger.info("=============== Result job processing info ===============");
        logger.info("📊 Job mane: {}, task count: {}, task duration: {} {}.", jobName, taskCount, duration, unitNames[timeUnit.ordinal()]);
        logger.info("⏱ Total time: {} ms", totalTime);
        logger.info("✅ Completed tasks: {}", executedTasks);
        logger.info("❌ Aborted tasks: {}", abortedTaskCount);
        logger.info("❌ Rejected tasks: {}", rejectedTaskCount);
        logger.info("❌ Discarded tasks: {}", discardedTaskCount);
        logger.info("⏲ Average time per task: {} ms", df.format(avgTime));
    }

    public void printJobResult(JobResult jobResult) {
        String[] unitNames = new String[] { "ns", "us", "ms", "s", "min", "h", "d" };

        long totalTime = jobResult.endTime - jobResult.startTime;
        int executedTasks = taskCount - jobResult.rejectedTaskCount - jobResult.discardedTaskCount - jobResult.abortedTaskCount;
        double avgTime = executedTasks > 0 ? (double) totalTime / executedTasks : 0.0;
        DecimalFormat df = new DecimalFormat("#.#######");

        logger.info("                                                                ");
        logger.info("============= Result job processing info =============");
        logger.info("📊 Job mane: {}, Executor type: {}", jobName, executorType);
        logger.info("Task count: {}, Duration: {} {}, Interval: {} {}",  taskCount, duration, unitNames[timeUnit.ordinal()], interval, unitNames[timeUnit.ordinal()]);
        logger.info("-------------------------------------------------");
        logger.info("⏱ Total time: {} ms", totalTime);
        logger.info("✅ Completed tasks: {}", executedTasks);
        logger.info("❌ Aborted tasks: {}", jobResult.abortedTaskCount);
        logger.info("❌ Rejected tasks: {}", jobResult.rejectedTaskCount);
        logger.info("❌ Discarded tasks: {}", jobResult.discardedTaskCount);
        logger.info("⏲ Average time per task: {} ms", df.format(avgTime));
        logger.info("📚 Total queue size: {} tasks", taskExecutor.getTotalQueueSize());
        logger.info("-------------------------------------------------\n");


    }

    public String getJobName() {
        return jobName;
    }

    public long getJobId() {
        return jobId;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public long getDuration() {
        return duration;
    }

    public long getInterval() {
        return interval;
    }

//    public CustomThreadExecutor getExecutor() {
//        return executor;
//    }
//
//    public void setExecutor(CustomThreadExecutor executor) {
//        this.executor = executor;
//    }

//    public void setCustomExecutorService(CustomExecutorService customExecutorService) {
//        this.customExecutorService = customExecutorService;
//        this.standardThreadExecutor = customExecutorService.getStandardThreadExecutor();
//        this.customThreadExecutor = customExecutorService.getCustomThreadExecutor();
//    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
//        this.executorType = executorType;
    }

    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        Task task = (Task) evt.getSource();
        String propertyName = evt.getPropertyName();


        switch (propertyName){
            case "state" -> {

                TaskState oldValue = (TaskState) evt.getOldValue();
                TaskState newValue = (TaskState) evt.getNewValue();

//                System.out.println("STATE changed." +
//                        "\nCurrent task = " + task.getName() + " New value = " + task.state +
//                        "\ncurrentTaskCountAtomic = " + currentTaskCountAtomic.get() +
//                        "\ncompletedTaskCountAtomic = " + completedTaskCountAtomic.get() +
//                        "\nrejectedTaskCountAtomic = " + rejectedTaskCountAtomic.get() +
//                        "\n");
            }
            case "status" -> {
                TaskStatus oldValue = (TaskStatus) evt.getOldValue();
                TaskStatus newValue = (TaskStatus) evt.getNewValue();

                if(newValue.equals(TaskStatus.REJECTED)) {
                    rejectedTaskCountAtomic.incrementAndGet();
                }
                if(newValue.equals(TaskStatus.COMPLETED)) {
                    completedTaskCountAtomic.incrementAndGet();
                }

//                System.out.println("STATUS changed." +
//                        "\nCurrent task = " + task.getName() + " New value = " + task.status +
//                        "\ncurrentTaskCountAtomic = " + currentTaskCountAtomic.get() +
//                        "\ncompletedTaskCountAtomic = " + completedTaskCountAtomic.get() +
//                        "\nrejectedTaskCountAtomic = " + rejectedTaskCountAtomic.get() +
//                        "\n");
            }
        }

        currentTaskCountAtomic.set(rejectedTaskCountAtomic.get() + completedTaskCountAtomic.get());
        if(currentTaskCountAtomic.compareAndSet(taskCount,taskCount)){
//            jobStatus = JobStatus.COMPLETED;
            jobStatus.set(JobStatus.COMPLETED);
            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            logger.info("Job #{} completed. Duration: {} ms. Task = {}.", jobName, totalTime, task.getName());
        }
    }
    public boolean shutdownExecutor(){
        switch (executorType){
            case CUSTOM -> {
//                customThreadExecutor.shutdown();
                taskExecutor.shutdown();
            }
            case STANDARD -> {
//                standardThreadExecutor.shutdown();
                taskExecutor.shutdown();
            }
        }

        notificationExecutor.shutdown();

        return true;
    }

    public void printTaskStatus(){

        System.out.println("\nTask execution result:");
        for(Task task: tasks){
            System.out.println("Task: " + task.getName() +
                    " state: " + task.state +
                    " status: " + task.status );
        }
        System.out.println("\n\n");
    }
}
