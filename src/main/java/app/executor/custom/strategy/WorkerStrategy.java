package app.executor.custom.strategy;

import app.executor.custom.Worker;

public interface WorkerStrategy {
    void run(Runnable runnable);
    boolean offerTask(Worker worker, Runnable runnable);
}
