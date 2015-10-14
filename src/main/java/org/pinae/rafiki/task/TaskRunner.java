package org.pinae.rafiki.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.pinae.rafiki.job.Job;
import org.pinae.rafiki.listener.JobListener;
import org.pinae.rafiki.listener.TaskListener;
import org.pinae.rafiki.trigger.AbstractTrigger;

/**
 * 任务执行器
 * 
 * @author Huiyugeng
 * 
 */
public final class TaskRunner implements Runnable {
	private static Logger logger = Logger.getLogger(TaskRunner.class);

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/*
	 * 需要执行的任务
	 */
	private Task task;
	/*
	 * 任务监听器
	 */
	private TaskListener taskListener;
	
	/*
	 * 需要执行的作业
	 */
	private Job job;
	/*
	 * 作业监听器
	 */
	private JobListener jobListener;
	
	/*
	 * 任务触发器
	 */
	private AbstractTrigger trigger;
	
	/**
	 * 构造函数
	 * 
	 * @param task 需要执行的任务
	 */
	protected TaskRunner(Task task) {
		this.task = task;
		
		if (task instanceof TaskListener) {
			taskListener = (TaskListener)task;
		}
		
		if (taskListener != null) {
			taskListener.start();
		}

		this.job = task.getJob();
		if (task.getTrigger() instanceof AbstractTrigger) {
			this.trigger = (AbstractTrigger)task.getTrigger();
		}
		
		if (job instanceof JobListener) {
			jobListener = (JobListener) job;
		}

	}

	public void run() {
		
		if (trigger == null) {
			return;
		}

		if (! trigger.isFinish() && task.getStatus() != Task.Status.STOP) {

			if (trigger.match() && task.getStatus() == Task.Status.RUNNING && task.getStatus() != Task.Status.PAUSE) {

				long start = System.currentTimeMillis();
				task.setStartTime(start);

				logger.debug(String.format("task=%s; group=%s; date=%s; action=start", task, task.getGroup(), dateFormat.format(new Date())));

				try {

					if (jobListener != null) {
						jobListener.beforeJobExecute();
					}

					if (job.execute() == false) {
						if (jobListener != null) {
							jobListener.executeFail();
						}
					}

					if (jobListener != null) {
						jobListener.afterJobExecute();
					}

				} catch (Exception e) {
					logger.debug(String.format("task=%s; group=%s; date=%s; exception=%s", task, task.getGroup(), dateFormat.format(new Date()),
							e.getMessage()));

					if (jobListener != null) {
						jobListener.executeException();
					}
				}

				long end = System.currentTimeMillis();
				logger.debug(String.format("task=%s; group=%s; date=%s; action=stop; used=%s ms", task, task.getGroup(),
						dateFormat.format(new Date()), Long.toString(end - start)));
				
				task.setStartTime(-1);
			}
		} else {
			logger.debug(String.format("task=%s; group=%s; date=%s; action=finish", task, task.getGroup(), dateFormat.format(new Date())));

			this.stop();
			
			if (taskListener != null) {
				taskListener.finish();
			}
		}
		
	}
	
	/**
	 * 停止任务Timer
	 * 
	 */
	public void stop() {

	}

}
