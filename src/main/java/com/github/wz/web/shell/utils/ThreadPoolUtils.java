package com.github.wz.web.shell.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public final class ThreadPoolUtils {
    private static ThreadPoolTaskExecutor threadPool = null;

    private ThreadPoolUtils() {
    }

    public static void execute(Runnable runnable) {
        get().execute(runnable);
    }

    public static <T> Future<T> submit(Callable<T> callable) {
        return get().submit(callable);
    }

    private static ThreadPoolTaskExecutor get() {
        if (threadPool != null) {
            log.debug("线程池已创建");
            return threadPool;
        } else {
            synchronized (ThreadPoolUtils.class) {
                //二次检查
                if (threadPool == null) {
                    // 获取处理器数量 Ncpu = CPU核心数
                    int cpuNum = Runtime.getRuntime().availableProcessors();
                    // 根据cpu数量,计算出合理的线程并发数
                    // Nthreads = Ncpu x Ucpu x (1 + W/C)，其中 Ucpu = CPU使用率，0~1；W/C = 等待时间与计算时间的比率
                    int threadNum = cpuNum * 2;
                    // 创建线程池
                    threadPool = new ThreadPoolTaskExecutor();
                    threadPool.setThreadNamePrefix("web-shell-thread");
                    // 核心线程数
                    threadPool.setCorePoolSize(threadNum);
                    // 最大线程数
                    threadPool.setMaxPoolSize(threadNum + 1);
                    // 队列已满,而且当前线程数已经超过最大线程数时的异常处理策略 来电运行政策
                    threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    //用来设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean，这样这些异步任务的销毁就会先于Redis线程池的销毁。
                    threadPool.setWaitForTasksToCompleteOnShutdown(true);
                    //该方法用来设置线程池中任务的等待时间，如果超过这个时候还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
                    threadPool.setAwaitTerminationSeconds(30);
                    // 初始化
                    threadPool.initialize();
                    log.info("创建线程池完成:{}", threadPool.toString());
                }
            }
        }
        return threadPool;
    }

    public static void shutdown() {
        if (threadPool != null) {
            threadPool.shutdown();
            log.info("关闭线程池");
        }
    }
}
