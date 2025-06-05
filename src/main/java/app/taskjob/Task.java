package app.taskjob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Task implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(Task.class);

    public long jobId;
    public int id;
    public String name;
    public String description;

    TimeUnit timeUnit;

    public long duration;
    private final long durationMS;

    public TaskState state;
    public TaskStatus status;

    private final PropertyChangeSupport support;
    private final ExecutorService notificationExecutor;

    public Task(long jobId, int id, String name, String description,
                TimeUnit timeUnit, long duration, ExecutorService notificationExecutor) {
        this.jobId = jobId;
        this.id = id;
        this.name = name + String.valueOf(id);
        this.description = description;
        this.timeUnit = timeUnit;
        this.duration = duration;

        this.durationMS = timeUnit.toMillis(duration);
        this.support = new PropertyChangeSupport(this);
        this.state = TaskState.NEW;
        this.status = TaskStatus.NEW;

        this.notificationExecutor = notificationExecutor;
//        this.notificationExecutor = Executors.newCachedThreadPool();
    }

    public String getName() {
        return name;
    }

    public void setStatus(TaskStatus status) {

        setStatusThread(status);

//        support.firePropertyChange(new PropertyChangeEvent(this,"status",this.status, status));
//        this.status = status;
    }

    public void setStatusThread(TaskStatus status) {
//        System.out.println("\nTask: " + this.id + " Number of listeners: " + support.getPropertyChangeListeners().length);

        TaskStatus oldValue = this.status;
        TaskStatus newValue = status;
        this.status = status;

        PropertyChangeEvent pce = new PropertyChangeEvent(this,"status",oldValue, newValue);

        this.notificationExecutor.execute(()->{
                    try{
                        this.support.firePropertyChange(pce);

//                        System.out.println("STATUS notification is fired. " + getName());

//                        System.out.println("STATUS notification is fired. " + getName() +
////                        "\nCurrent executor name: " + notificationExecutor .getName() +
//                                        "\nCurrent thread name: " + Thread.currentThread().getName() +
////                                        "ActiveCount: " + this.notificationExecutor.
//                                        "\nThread group: " + Thread.currentThread().getThreadGroup().getName() +
//                                        "\n"
//                        );
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());
                    }
                }

        );

    }


    public void setState(TaskState state) {

        setStateThread(state);

//        System.out.println("\nTask: " + this.id + " Number of listeners: " + support.getPropertyChangeListeners().length);
//
//        TaskState oldState = this.state;
//        TaskState newState = state;
//        this.state = state;
//
//        PropertyChangeEvent pce = new PropertyChangeEvent(this,"state",oldState, newState);
//
//        support.firePropertyChange(pce);
//
//        System.out.println("STATE notification is fired. Task: " + getName() +
//                "\nCurrent thread name: " + Thread.currentThread().getName() +
//                "\nThread group: " + Thread.currentThread().getThreadGroup() +
//                "\n"
//        );
    }

    public void setStateThread(TaskState state) {
//        System.out.println("\nTask: " + this.id + " Number of listeners: " + support.getPropertyChangeListeners().length);

        TaskState oldValue = this.state;
        TaskState newValue = state;
        this.state = state;

        PropertyChangeEvent pce = new PropertyChangeEvent(this,"state",oldValue, newValue);

        this.notificationExecutor.execute(()->{
            try{
                this.support.firePropertyChange(pce);

//                System.out.println("STATE notification is fired. " + getName());

//                System.out.println("STATE notification is fired. " + getName() +
////                        "\nCurrent executor name: " + notificationExecutor .getName() +
//                        "\nCurrent thread name: " + Thread.currentThread().getName() +
//                        "\nThread group: " + Thread.currentThread().getThreadGroup().getName() +
//                        "\n"
//                );
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
                }

        );

    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
//        System.out.println("Adding listener: " + pcl);
        support.addPropertyChangeListener(pcl);
    }

    @Override
    public void run() {
        logger.info("▶ Task #job{}-{} is started.", jobId, name);
        setState(TaskState.STARTED);
        try {
            Thread.sleep(durationMS);
            setState(TaskState.PROCESSED);
        } catch (InterruptedException e) {
            setState(TaskState.INTERRUPTED);
            Thread.currentThread().interrupt();
        }
        logger.info("✔ Task #job{}-{} is completed.", jobId, name);
        setStatus(TaskStatus.COMPLETED);

    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", duration=" + duration +
                '}';
    }

    public int getId() {
        return this.id;
    }
}
