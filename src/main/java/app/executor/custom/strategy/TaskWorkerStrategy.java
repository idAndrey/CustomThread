package app.executor.custom.strategy;

import app.executor.custom.Worker;

import app.taskjob.Task;
import app.taskjob.TaskState;

public class TaskWorkerStrategy implements WorkerStrategy  {
    @Override
    public void run(Runnable runnable) {
        Task task = (Task) runnable;
        System.out.println("[TaskStrategy] Preparing task: " + task.getName());
//        task.prepare();
        task.run();
//        task.cleanup();
    }

    @Override
    public boolean offerTask(Worker worker, Runnable runnable) {
        Task task = (Task) runnable;
        task.setState(TaskState.OFFERED);
        boolean offered = worker.offerTask(task);
        System.out.println("\nFROM offerTask\n[" + worker.getWorkerName() + "]" +
                " Queue #" + (worker.getWorkerId() - 1) + ": " + worker.getTaskQueue().toString() + "\n");
//        if(offered) task.setState(TaskState.OFFERED);
        return offered;
    }
}
