package app.taskjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class JobsReader {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Job> readJobsFromResources(String resourcePath) throws IOException {
        try (InputStream inputStream = JobsReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            // Читаем JSON как список объектов Job
            List<Job> jobs = objectMapper.readValue(inputStream, new TypeReference<List<Job>>() {});

            // Валидация
            validateJobs(jobs);

            return jobs;
        }
    }

    private static void validateJobs(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            throw new IllegalArgumentException("Jobs list cannot be empty");
        }

        for (Job job : jobs) {
            if (job.getJobName() == null || job.getJobName().isEmpty()) {
                throw new IllegalArgumentException("Job name cannot be empty");
            }
            if (job.getTaskCount() <= 0) {
                throw new IllegalArgumentException("Task count must be positive for job: " + job.getJobName());
            }
            if (job.getDuration() <= 0) {
                throw new IllegalArgumentException("Duration must be positive for job: " + job.getJobName());
            }
            if (job.getInterval() <= 0) {
                throw new IllegalArgumentException("Interval must be positive for job: " + job.getJobName());
            }
            if (job.getTimeUnit() == null) {
                throw new IllegalArgumentException("Time unit must be specified for job: " + job.getJobName());
            }
        }
    }
}

