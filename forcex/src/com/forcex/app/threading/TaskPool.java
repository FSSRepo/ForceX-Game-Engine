package com.forcex.app.threading;

import com.forcex.utils.PoolArray;

public class TaskPool {
    boolean running = true;
    private final PoolArray<Task> queue;
    private final Thread thread;

    public TaskPool() {
        queue = new PoolArray<>(4);
        thread = new Thread(() -> {
            while (running) {
                if (queue.count() == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Task t;
                    while ((t = queue.pop()) != null) {
                        if (!t.execute()) {
                            queue.push(t);
                        }
                    }
                }
            }
        });
    }

    public void addTask(Task task) {
        queue.push(task);
    }

    public boolean hasTasks() {
        return queue.count() > 0;
    }

    public void start() {
        thread.start();
    }

    public boolean isAvailable() {
        return running;
    }

    public void finish() {
        running = false;
    }
}
