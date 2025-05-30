package app.executor.custom.strategy;

import app.executor.custom.Worker;

import app.taskjob.Task;

import java.util.HashMap;
import java.util.Map;

public class WorkerStrategies {
    private final Map<Class<?>, WorkerStrategy> strategies = new HashMap<>();

    public WorkerStrategies() {
        // Регистрируем стратегии
        strategies.put(Task.class, new TaskWorkerStrategy());
        // Можно добавить другие стратегии для разных типов задач
    }

    public void run(Runnable runnable) {
        WorkerStrategy strategy = strategies.getOrDefault(
                runnable.getClass(),
                new DefaultWorkerStrategy()
        );
        strategy.run(runnable);
    }

    public boolean offerTask(Worker worker, Runnable runnable){
        WorkerStrategy strategy = strategies.getOrDefault(
                runnable.getClass(),
                new DefaultWorkerStrategy()
        );
        return strategy.offerTask(worker, runnable);
    }
}
