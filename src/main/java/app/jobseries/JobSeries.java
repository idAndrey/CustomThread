package app.jobseries;

import app.Application;
import app.taskjob.Job;
import app.taskjob.JobResult;
import app.taskjob.JobsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JobSeries {

    private static final Logger logger = LoggerFactory.getLogger(JobSeries.class);

    private final Properties config;
    private List<Job> jobs;
    private Map<Job, JobSeriesResult> jobSeriesResults;

    private long jobId;
    private int repeatMultiplier;

    public JobSeries(){

        this.config = getConfig("jobseries.properties");

        this.jobId = Long.parseLong(config.getProperty("jobId"));
        this.repeatMultiplier = Integer.parseInt(config.getProperty("repeatMultiplier"));

//        this.jobs = new ArrayList<>();
        this.jobs = readJobs("jobs.json");
        this.jobs = JobsReader.readJobsFromResources("jobs.json");

        this.jobSeriesResults = new HashMap<>();

    }

    private Properties getConfig(String fileName){
        Properties config = new Properties();
        try (InputStream is = Application.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (is != null) {
                config.load(is);
            }

        } catch (IOException e) {
            logger.error("Failed to get executor settings: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return config;
    }

    private List<Job> readJobs(String jobsJsonFileName){
        jobs = JobsReader.readJobsFromResources(jobsJsonFileName);
        return jobs;
    }
}
