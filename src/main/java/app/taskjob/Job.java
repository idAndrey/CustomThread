package app.taskjob;

import app.executor.custom.AbortedExecutionException;
import app.executor.custom.CustomThreadExecutor;
import app.executor.custom.DiscardedExecutionException;
import app.executor.factory.CustomExecutor;
import app.executor.factory.CustomExecutorFactory;
import app.executor.factory.CustomExecutorService;
import app.executor.factory.ExecutorType;
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

public class Job implements Callable, PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private final String jobName;
    private final long jobId;
    private final int taskCount;
    private final TimeUnit timeUnit;
    private final long duration;
    private final long interval;

    public JobStatus jobStatus;

    private final long durationMS;
    private final long intervalMS;

    private List<Task> tasks;

    private CustomExecutorFactory taskExecutorFactory;
    private CustomThreadExecutor customThreadExecutor;
    private ExecutorService standardThreadExecutor;
    private CustomExecutorService customExecutorService;
    private ExecutorType executorType;
    private CustomExecutor taskExecutor;
//    private NotificationThreadExecutor notificationExecutor;
    private ExecutorService notificationExecutor;

    private final AtomicInteger startedTaskCount;
    private final AtomicInteger offeredTaskCount;
//    int offeredTaskCount = 0;
    private final AtomicInteger rejectedTaskCountAtomic;
    private final AtomicInteger completedTaskCount;

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
               @JsonProperty("timeUnit") TimeUnit timeUnit) {
        this.jobName = jobName;
        this.jobId = jobId;
        this.taskCount = taskCount;
        this.duration = duration;
        this.interval = interval;
        this.timeUnit = timeUnit;

        this.jobStatus = JobStatus.NEW;

        this.durationMS = timeUnit.toMillis(duration);
        this.intervalMS = timeUnit.toMillis(interval);

        this.offeredTaskCount = new AtomicInteger(0);
        this.startedTaskCount = new AtomicInteger(0);
        this.rejectedTaskCountAtomic = new AtomicInteger(0);
        this.completedTaskCount = new AtomicInteger(0);

        this.taskExecutorFactory = new CustomExecutorFactory();
//        this.notificationExecutor = taskExecutorFactory.getExecutor(ExecutorType.NOTIFICATION);

        this.notificationExecutor = Executors.newCachedThreadPool();

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

        JobResult result = new JobResult(
                rejectedTaskCount,
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

        logger.info("=============== Result job processing info ===============");
        logger.info("📊 Job mane: {}, task count: {}, task duration: {} {}.", jobName, taskCount, duration, unitNames[timeUnit.ordinal()]);
        logger.info("⏱ Total time: {} ms", totalTime);
        logger.info("✅ Completed tasks: {}", executedTasks);
        logger.info("❌ Aborted tasks: {}", jobResult.abortedTaskCount);
        logger.info("❌ Rejected tasks: {}", jobResult.rejectedTaskCount);
        logger.info("❌ Discarded tasks: {}", jobResult.discardedTaskCount);
        logger.info("⏲ Average time per task: {} ms", df.format(avgTime));
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

    public void setCustomExecutorService(CustomExecutorService customExecutorService) {
        this.customExecutorService = customExecutorService;
        this.standardThreadExecutor = customExecutorService.getStandardThreadExecutor();
        this.customThreadExecutor = customExecutorService.getCustomThreadExecutor();
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Task task = (Task) evt.getSource();
        String propertyName = evt.getPropertyName();


        switch (propertyName){
            case "state" -> {

                TaskState oldValue = (TaskState) evt.getOldValue();
                TaskState newValue = (TaskState) evt.getNewValue();

                System.out.println("\n\nFROM propertyChange" + " task = " + task.getName() +
                        "\npropertyName: " + propertyName +
                        "\noldValue: " + oldValue +
                        "\nnewValue: " + newValue +
                        "\ntask: " + task.getName() +
                        "\nOldValue = NEW : " + oldValue.equals(TaskState.NEW) + " and " +
                        "NewValue = OFFERED : " + newValue.equals(TaskState.OFFERED) +
                        "\n"
                );

                if(evt.getOldValue().equals(TaskState.NEW) && evt.getNewValue().equals(TaskState.OFFERED)){

                    offeredTaskCount.incrementAndGet();
                    System.out.println("\nofferedTaskCount = " + offeredTaskCount);
                    if(offeredTaskCount.get() == taskCount) System.out.println("\n\n\nJob STARTED\n\n");
                }

            }
            case "status" -> {
                TaskStatus oldValue = (TaskStatus) evt.getOldValue();
                TaskStatus newValue = (TaskStatus) evt.getNewValue();

                if(newValue.equals(TaskStatus.REJECTED)) {
                    rejectedTaskCountAtomic.incrementAndGet();
                }
                if(newValue.equals(TaskStatus.COMPLETED)) {
                    completedTaskCount.incrementAndGet();
                }
            }
        }

        if(rejectedTaskCountAtomic.get() + completedTaskCount.get() == taskCount){
            jobStatus = JobStatus.COMPLETED;
            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            logger.info("Job #{} completed. Duration: {} ms", jobName, totalTime);
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
        return true;
    }
}
