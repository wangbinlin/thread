package com.hello.chapter13;

import java.util.ArrayList;
import java.util.LinkedList;

public class SimpleThreadPool {

    // 线程池默认大小为10
    private final int size;
    private static final int DEFAULT_SIZE = 10;

    //任务队列
    private static final LinkedList<Runnable> TASK_QUEUE = new LinkedList<>();
    private static final String PREFIX = "Simple-thread-";
    // 线程队列
    private static final ArrayList<Thread> THREAD_QUEUE = new ArrayList<>();
    // 将线程池中的所有线程归于一个线程组
    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("Simple-ThreadGroup");
    private static volatile int seq = 0;


    public SimpleThreadPool() {
        this(DEFAULT_SIZE);
    }

    public SimpleThreadPool(int size) {
        this.size = size;
        init();
    }

    private void init() {
        for (int i = 0; i < size; i++) {
            createWorkThread();
        }
    }
    //创建工作线程并放入线程队列
    private void createWorkThread() {
        WorkThread t = new WorkThread(THREAD_GROUP, PREFIX + (++seq));
        t.start();
        THREAD_QUEUE.add(t);
    }

    //向任务队列中push
    public void submit(Runnable runnable) {
        synchronized (TASK_QUEUE) {
            TASK_QUEUE.addLast(runnable);
            // 任务加入后，通知处于等待中的线程
            TASK_QUEUE.notifyAll();
        }
    }

    //线程状态：空闲，运行，阻塞，消亡
    private enum TaskState {
        FREE, RUNNING, BLOCKED, DEAD
    }

    //自定义工作线程
    private class WorkThread extends Thread {
        private TaskState taskState = TaskState.FREE;

        WorkThread(ThreadGroup group, String name) {
            super(group, name);
        }
        /**
         * 线程状态为非DEAD状态时，检测任务队列中是否有任务，有则开始接任务
         * 当其他线程正在取任务时，其他线程等待（状态为阻塞状态）
         * 从任务队列中取走任务（状态变为运行中），然后开始做任务
         * 做完任务后（状态变为自由状态，等待下一个任务）
         * 线程不结束，回到线程池中
         */
        public void run() {
            OUTER:
            while (this.taskState != TaskState.DEAD) {
                Runnable runnable = null;
                synchronized (TASK_QUEUE) {
                    while (TASK_QUEUE.isEmpty()) {
                        try {
                            this.taskState = TaskState.BLOCKED;
                            TASK_QUEUE.wait();
                        } catch (InterruptedException e) {
                            // 当线程任务时被打断，回到线程池重新接任务
                            this.taskState = TaskState.FREE;
                            break OUTER;
                        }
                    }
                    // 拿到任务
                    runnable = TASK_QUEUE.removeFirst();
                }
                // 执行任务，放在synchronized外，
                // 让线程执行任务的同时释放锁，让其他线程可以接任务
                if (runnable != null) {
                    this.taskState = TaskState.RUNNING;
                    runnable.run();
                    this.taskState = TaskState.FREE;
                }
            }
        }

        private void closed() {
            this.taskState = TaskState.DEAD;
        }

        private TaskState getTaskState() {
            return this.taskState;
        }
    }


}
