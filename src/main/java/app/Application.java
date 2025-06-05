package app;

import app.executor.factory.ExecutorType;
import app.taskjob.Job;
import app.taskjob.JobResult;
import app.taskjob.JobsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Application {


    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private static final int TASK_COUNT = 100;

    public static void main(String[] args) {

//        System.out.println("Проверка кодировки");

        ExecutorType executorType = ExecutorType.STANDARD;
        if (args.length > 0) {
            String executorTypeString = args[0];

            if ("CUSTOM".equalsIgnoreCase(executorTypeString)) {
                executorType = ExecutorType.CUSTOM;
                logger.info("Указан пользовательский пул потоков, executorType = '{}'", executorType);
            } else if ("STANDARD".equalsIgnoreCase(executorTypeString)) {
                executorType = ExecutorType.STANDARD;
                logger.info("Указан стандартный пул потоков, executorType = '{}'", executorType);
            } else {
                logger.info("Неверно указан тип пула потоков, executorType = '{}'.", executorTypeString);
                logger.info("Возможные значения 'CUSTOM' или 'STANDARD'.");
                logger.info("Используйте: java -jar custom-thread-1.0-SNAPSHOT.jar [CUSTOM|STANDARD]");
                return;
            }
        } else {
            logger.info("Тип пула потоков не передан.");
            logger.info("Установлен стандартный тип пул потоков, executorType = '{}'", executorType);
        }

        List<Job> jobs = new ArrayList<>();
        jobs = JobsReader.readJobsFromResources("jobs.json");
        ConcurrentMap<Job, JobResult> jobResults = new ConcurrentHashMap<>();

        Job job = jobs.get(0);
        
        try (ExecutorService jobExecutor = newSingleThreadExecutor()){

//            job.setTaskExecutor(ExecutorType.STANDARD);
//            job.setTaskExecutor(ExecutorType.CUSTOM);
            job.setTaskExecutor(executorType);

            Future<JobResult> result = jobExecutor.submit(job);

            try {

                jobResults.put(job,result.get());

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            job.printJobResult(jobResults.get(job));

        } catch (RuntimeException e) {
            logger.error("Job interrupted: {}", e.getMessage());
        }


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

//        job.printTaskStatus();

        System.out.println("\n\n\nFINISHED");

        job.shutdownExecutor();
    }

}
