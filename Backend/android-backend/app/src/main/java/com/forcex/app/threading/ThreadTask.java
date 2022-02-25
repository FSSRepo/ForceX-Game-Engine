package com.forcex.app.threading;

import java.util.*;

public class ThreadTask {
	boolean running = true;
    ArrayList<Task> tasks = new ArrayList<>();
	
    Thread thread = new Thread(new Runnable() {
        public void run() {
            while (running) {
                if (tasks.size() == 0) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    Iterator<Task> it = tasks.iterator();
					boolean rewind = false;
                    while (it.hasNext() && !rewind) {
						// if it returns false not eliminate
                        if(it.next().execute()){
                     	   it.remove();
						}else{
							rewind = true;
						}
                    }
                }
            }
        } 
    });

    public void addTask(Task task) {
        tasks.add(task);
    }

	public boolean hasTasks(){
		return tasks.size() > 0;
	}
	
	public void waitEmptyTasks(){
		while(true){
			if(!hasTasks()){
				break;
			}
		}
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
