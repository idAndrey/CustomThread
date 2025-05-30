package app.executor.factory;

import app.Application;
import app.executor.custom.BalanceStrategy;
import app.executor.custom.CustomThreadExecutor;
import app.executor.custom.RejectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.*;

public class CustomExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(CustomExecutorService.class);

    private final Properties config;

    public CustomExecutorService(String fileName) {
        this.config = getConfig(fileName);
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
//            System.exit(1);
        }
        return config;
    }

    public CustomThreadExecutor getCustomThreadExecutor(){

        CustomThreadExecutor executor = new CustomThreadExecutor(
                Integer.parseInt(config.getProperty("corePoolSize")),
                Integer.parseInt(config.getProperty("maxPoolSize")),
                Integer.parseInt(config.getProperty("queueSize")),
                Long.parseLong(config.getProperty("keepAliveTime")),
                TimeUnit.valueOf(config.getProperty("timeUnit")),
                Integer.parseInt(config.getProperty("minSpareThreads")),
                BalanceStrategy.valueOf(config.getProperty("balanceStrategy")),
                RejectionPolicy.valueOf(config.getProperty("rejectionPolicy"))
        );

        return executor;
    }

    public ExecutorService getStandardThreadExecutor(){
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(Integer.parseInt(config.getProperty("queueSize")));
        ExecutorService executor = new ThreadPoolExecutor(
                Integer.parseInt(config.getProperty("corePoolSize")),
                Integer.parseInt(config.getProperty("maxPoolSize")),
                Long.parseLong(config.getProperty("keepAliveTime")),
                TimeUnit.valueOf(config.getProperty("timeUnit")),
                workQueue,
                new ThreadPoolExecutor.AbortPolicy()
        );

//        CustomThreadExecutor executor = new CustomThreadExecutor(
//                Integer.parseInt(config.getProperty("corePoolSize")),
//                Integer.parseInt(config.getProperty("maxPoolSize")),
//                Integer.parseInt(config.getProperty("queueSize")),
//                Long.parseLong(config.getProperty("keepAliveTime")),
//                TimeUnit.valueOf(config.getProperty("timeUnit")),
//                Integer.parseInt(config.getProperty("minSpareThreads")),
//                BalanceStrategy.valueOf(config.getProperty("balanceStrategy")),
//                RejectionPolicy.valueOf(config.getProperty("rejectionPolicy"))
//        );

        return executor;
    }


}
