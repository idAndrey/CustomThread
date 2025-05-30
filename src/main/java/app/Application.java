package app;

import app.executor.factory.CustomExecutorService;
import app.executor.factory.ExecutorType;
import app.taskjob.Job;
import app.taskjob.JobResult;
import app.taskjob.JobsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Application {


    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private static final int TASK_COUNT = 100;

    public static void main(String[] args) {

        Properties config = new Properties();
        try (InputStream is = Application.class.getClassLoader()
                .getResourceAsStream("executor.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {

            logger.error("Failed to get executor settings: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

//        CustomThreadExecutor customExecutor = new CustomThreadExecutor(
//                Integer.parseInt(config.getProperty("corePoolSize")),
//                Integer.parseInt(config.getProperty("maxPoolSize")),
//                Integer.parseInt(config.getProperty("queueSize")),
//                Long.parseLong(config.getProperty("keepAliveTime")),
//                TimeUnit.valueOf(config.getProperty("timeUnit")),
//                Integer.parseInt(config.getProperty("minSpareThreads")),
//                BalanceStrategy.valueOf(config.getProperty("balanceStrategy")),
//                RejectionPolicy.valueOf(config.getProperty("rejectionPolicy"))
//        );

        CustomExecutorService customExecutorService = new CustomExecutorService("executor.properties");

//        customExecutor = CustomThreadExecutor.getCustomThreadExecutor();
//        ExecutorService standardExecutor = new ThreadPoolExecutor();

//        Job job1 = new Job("job_1",100,TimeUnit.MILLISECONDS,100,customExecutor);
//        job1.start();

        List<Job> jobs = new ArrayList<>();
        try {
            jobs = JobsReader.readJobsFromResources("jobs.json");
        } catch (IOException e) {
            logger.error("Failed to get job list: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
//            throw new RuntimeException(e);
        }
        ConcurrentMap<Job, JobResult> jobResults = new ConcurrentHashMap<>();

        Job job = jobs.get(2);
        
        try (ExecutorService jobExecutor = newSingleThreadExecutor()){


//            jobs.forEach(job -> {
////                System.out.println(job.getJobName());
//                job.setCustomExecutorService(customExecutorService);
//                job.setExecutorType(ExecutorType.CUSTOM);
//                Future<JobResult> result = jobExecutor.submit(job);
//                try {
//                    jobResults.putIfAbsent(job,result.get());
////                    job.printJobResult(result.get());
//                } catch (InterruptedException | ExecutionException e) {
//                    throw new RuntimeException(e);
//                }
////                job.jobResults.get(job).printTaskInfo();
//
//            });

            

//            job0.setCustomExecutorService(customExecutorService);
//            job0.setExecutorType(ExecutorType.CUSTOM);

//            job0.setExecutorType(ExecutorType.CUSTOM);
//            job0.setTaskExecutor(ExecutorType.CUSTOM);

            job.setExecutorType(ExecutorType.STANDARD);
            job.setTaskExecutor(ExecutorType.STANDARD);

            Future<JobResult> result = jobExecutor.submit(job);

            try {
                jobResults.put(job,result.get());
//                    job.printJobResult(result.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }


        }
//        catch (IOException e) {
//            logger.error("Failed to get job list: {}", e.getMessage());
//            e.printStackTrace();
//            System.exit(1);
//        }
//        catch (InterruptedException e) {
//            System.err.println("tasks interrupted");
//        }
        catch (RuntimeException e) {
            logger.error("Job interrupted: {}", e.getMessage());
        }
//        finally {
//            if (!jobExecutor.isTerminated()) {
//                System.err.println("cancel non-finished tasks");
//            }
//            jobExecutor.shutdownNow();
//            System.out.println("shutdown finished");
//        }


//        customExecutor.shutdown();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n\n\nFINISHED");
//        Job job = jobs.get(0); //.customExecutor.shutdown();
        job.shutdownExecutor();
    }

}
