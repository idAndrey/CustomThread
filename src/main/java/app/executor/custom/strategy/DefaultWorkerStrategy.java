package app.executor.custom.strategy;

import app.executor.custom.Worker;

public class DefaultWorkerStrategy implements WorkerStrategy{
    @Override
    public void run(Runnable runnable) {
//        System.out.println("[Default] Executing task");
        runnable.run();
    }

    @Override
    public boolean offerTask(Worker worker, Runnable runnable) {
        return worker.offerTask(runnable);
    }
}
