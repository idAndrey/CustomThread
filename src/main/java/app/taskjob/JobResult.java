package app.taskjob;

import app.executor.factory.ExecutorType;

public class JobResult {

    public int rejectedTaskCount = 0;
    public int abortedTaskCount = 0;
    public int discardedTaskCount = 0;

    public long startTime;
    public long endTime;

    public ExecutorType executorType;

    public JobResult(int rejectedTaskCount, int abortedTaskCount, int discardedTaskCount, long startTime, long endTime, ExecutorType executorType) {
        this.rejectedTaskCount = rejectedTaskCount;
        this.abortedTaskCount = abortedTaskCount;
        this.discardedTaskCount = discardedTaskCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.executorType = executorType;
    }

    @Override
    public String toString() {



        return "JobResult{" +
                "rejectedTaskCount=" + rejectedTaskCount +
                ", abortedTaskCount=" + abortedTaskCount +
                ", discardedTaskCount=" + discardedTaskCount +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", executorType=" + executorType +
                '}';
    }
}
