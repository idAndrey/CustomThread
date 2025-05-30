package app.executor.factory;

import app.executor.custom.CustomThreadExecutor;
import app.executor.standard.StandardThreadExecutor;
import app.executor.notification.NotificationThreadExecutor;

public class CustomExecutorFactory {

    public CustomExecutor getExecutor(ExecutorType executorType) {
        switch (executorType) {
            case CUSTOM:
                return new CustomThreadExecutor();
            case STANDARD:
                return new StandardThreadExecutor();
            case NOTIFICATION:
                return new NotificationThreadExecutor();

            default:
                throw new IllegalArgumentException("Unsupported ExecutorType: " + executorType);
        }
    }
}