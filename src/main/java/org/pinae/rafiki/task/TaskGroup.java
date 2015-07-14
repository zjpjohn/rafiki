package org.pinae.rafiki.task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import org.pinae.rafiki.job.Job;
import org.pinae.rafiki.trigger.Trigger;

/**
 * Task Group
 * 
 * @author Huiyugeng
 * 
 */
public class TaskGroup {

	public static final String DEFAULT = "default";

	/** Task Group Status, 0: Not any task 1: Tasks in group but not execute 2: Tasks executing **/
	public int status = 0;

	private String name = TaskGroup.DEFAULT;

	private Timer timer;

	private Map<String, Task> taskMap = new HashMap<String, Task>();
	private Map<String, TaskRunner> taskRunnerMap = new HashMap<String, TaskRunner>();

	public TaskGroup(String name) {
		this.name = name;
	}

	public Collection<Task> getTasks() {
		return taskMap.values();
	}

	public void add(Task task) {
		String taskName = task.getName();
		Job job = task.getJob();

		if (taskName == null) {
			task.setName(job.getName() != null ? job.getName() : job.toString());
		}

		task.setGroup(this);
		taskMap.put(taskName, task);
		
		if (status == 0) {
			status = 1;
		} else if (status == 2) {
			start(task);
		}
	}

	public Task remove(String taskName) {
		Task task = null;

		if (taskMap.size() > 0 && status != 2) {
			if (taskMap.get(taskName) != null) {
				task = taskMap.remove(taskName);
				if (taskMap.size() == 0) {
					status = 0;
				}
			}
		}

		return task;
	}

	public void start() {
		timer = new Timer();
		
		for (Iterator<String> iterTaskName = taskMap.keySet().iterator(); iterTaskName.hasNext();) {
			Task task = taskMap.get(iterTaskName.next());
			start(task);
		}
		status = 2;
	}
	
	public void start(Task task){
		if (task != null) {
			Trigger trigger = task.getTrigger();
			
			TaskRunner taskRunner = new TaskRunner(task);
			if (task.getTrigger().isRepeat()) {
				timer.schedule(taskRunner, trigger.getStartTime(), trigger.getRepeatInterval());
			} else {
				timer.schedule(taskRunner, trigger.getStartTime());
			}
			
			task.start();
			
			taskMap.put(task.getName(), task);
			taskRunnerMap.put(task.getName(), taskRunner);
		}
	}

	public void stop() {
		
		for (Iterator<String> iterTaskName = taskMap.keySet().iterator(); iterTaskName.hasNext();) {
			stop(iterTaskName.next());
		}
		
		timer.cancel();
		timer.purge();

		status = 1;
	}

	public void stop(String taskName) {
		
		Task task = taskMap.get(taskName);
		if (task != null) {
			task.stop();
		}
		
		TaskRunner taskRunner = taskRunnerMap.get(taskName);
		if (taskRunner != null) {
			taskRunner.cancel();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getStatus() {
		return status;
	}

	public String toString() {
		return name;
	}
}
