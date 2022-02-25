package com.forcex.app.threading;

import java.util.*;
import com.forcex.utils.*;

public class ThreadTask {
	boolean running = true;
    PoolArray<Task> queue;
	
    Thread thread;
	
	public ThreadTask(){
		queue = new PoolArray<>(4);
		thread = new Thread(new Runnable() {
				public void run() {
					while (running) {
						if (queue.count() == 0) {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
							}
						} else {
							Task t = null;
							while((t = queue.pop()) != null){
								if(!t.execute()){
									queue.push(t);
								}
							}
						}
					}
				} 
			});
	}
	
    public void addTask(Task task) {
        queue.push(task);
    }

	public boolean hasTasks(){
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
