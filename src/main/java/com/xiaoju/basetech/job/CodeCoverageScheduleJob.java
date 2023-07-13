package com.xiaoju.basetech.job;

import com.xiaoju.basetech.dao.CoverageReportDao;
import com.xiaoju.basetech.entity.CoverageReportEntity;
import com.xiaoju.basetech.service.CodeCovService;
import com.xiaoju.basetech.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 *  定时任务代码，定时捞取数据库的jobstatus，去执行覆盖率任务
 *
 * @author: charlynegaoweiwei
 * @time: 2020/4/26 7:45 PM
 */
@Slf4j
@Component
public class CodeCoverageScheduleJob {

    @Autowired
    private CoverageReportDao coverageReportDao;

    @Autowired
    private CodeCovService codeCovService;


    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");

    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     这段代码创建了一个线程池 executor，并设置了一些配置参数。让我们逐个解释每个参数的含义：

     2 和 2: 这是核心线程数和最大线程数，表示线程池中同时可以运行的线程数量。在这个例子中，线程池中始终保持两个线程，无论任务是否繁忙。

     5 * 60: 这是线程的空闲时间超时时间，以秒为单位。如果一个线程在指定的时间内没有执行任何任务，那么它将被终止并从线程池中移除，以节省系统资源。

     TimeUnit.SECONDS: 这是空闲时间超时的时间单位，这里设置为秒。

     new SynchronousQueue<>(): 这是线程池使用的工作队列。在这个例子中，使用了一个同步队列，它没有容量限制，意味着它可以根据需要立即创建新线程来处理任务。

     r -> new Thread(r, "Code-Coverage-Thread-pool" + counter.getAndIncrement()): 这是线程工厂，用于创建新的线程。在这个例子中，通过 lambda 表达式定义了一个线程工厂，它会创建一个新的线程，并为每个线程设置一个名称，名称以 "Code-Coverage-Thread-pool" 开头，后面加上一个递增的计数器。

     通过以上配置，线程池 executor 将会维护两个核心线程，如果有更多的任务到达，它会创建额外的线程来处理任务，直到达到最大线程数。当线程空闲一段时间后，超过设定的空闲时间，它将被终止并从线程池中移除。新创建的线程会以特定的命名规则命名，方便调试和追踪。
    */

//    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 5 * 60, TimeUnit.SECONDS,
//            new SynchronousQueue<>(), r -> new Thread(r, "Code-Coverage-Thread-pool" + counter.getAndIncrement()));
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 5 * 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(), r -> new Thread(r, "Code-Coverage-Thread-pool" + counter.getAndIncrement()));

    /**
     * clone代码定时任务
     * <p>
     * 查询状态是Constants.JobStatus.INITIAL的任务
     */
    @Scheduled(fixedDelay = 10_000L, initialDelay = 10_000L)
    public void codeCloneJob() {
        // 1. 查询需要diff的数据，对初始数据 0进行判断
        List<CoverageReportEntity> resList = coverageReportDao.queryCoverByStatus(Constants.JobStatus.INITIAL.val(),
                Constants.CoverageFrom.UNIT.val(), 1);
        log.info("查询需要diff的数据{}条", resList.size());
        resList.forEach(o -> {

            try {
                int num = coverageReportDao.casUpdateByStatus(Constants.JobStatus.INITIAL.val(),
                        Constants.JobStatus.WAITING.val(), o.getUuid());
                if (num > 0) {
                    //有未执行的代码下载任务，开始执行
                    log.info("有未执行的代码下载任务，开始执行"+o.getUuid());
                    executor.execute(() -> codeCovService.calculateUnitCover(o));
                } else {
                    log.info("others execute task :{}", o.getUuid());
                }
            } catch (Exception e) {
                log.info("定时任务轮询失败");
                coverageReportDao.casUpdateByStatus(Constants.JobStatus.WAITING.val(),
                        Constants.JobStatus.INITIAL.val(), o.getUuid());
            }
        }
        );
    }

    /**
     * @Description: 定时任务（60秒执行一次），检查mr触发的任务是否完成，是否进行了机器人报告
     * @param:
     * @return * @return void
     * @author panpeng
     * @date 2023/7/12 17:28
    */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 10_000L)
    public void checkMRJobDone() {

        List<CoverageReportEntity> resList = coverageReportDao.queryCoverByIsMrAndIsMrReport(1,
                0, 1);
        log.info("查询需要检查的MR触发的数据{}条", resList.size());
        resList.forEach(o -> {
                    log.info("修改uuid" + o.getUuid() + "的Robot report状态为2");
                    int num = coverageReportDao.updateReportStatusByUUid(2, o.getUuid());
                    if (num > 0) {
                        log.info("启动一个线程去监控");
                        try {
                            executor.execute(() -> {
                                try {
                                    codeCovService.checkJobDone(o.getUuid(), o.getMrUrl(), o.getMrUserMail());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (Exception e) {
                            log.info("定时任务轮询checkMRJobDone失败");
                        }
                    }
                }
        );
    }




    /**
     * 未执行完的任务， 超过120分钟时间任务状态未更新，将任务状态设置未初始化,status=0
     */
    @Scheduled(fixedDelay = 600_000L, initialDelay = 10_000L)
    public void resetJobStatus() {
        try {
            log.info("重置任务状态开始执行............");
            long currentTime = System.currentTimeMillis();
            Date date = new Date(currentTime - 120 * 60 * 1000);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String expireTime = df.format(date);
            coverageReportDao.casUpdateStatusByExpireTime(expireTime);
        } catch (Exception e) {
            log.error("重置任务执行失败");
        }
    }

    /**
     * 每五分钟从项目机器上拉取exec执行文件，计算环境的增量方法覆盖率
     */
  //  @Scheduled(fixedDelay = 600_000L, initialDelay = 100_000L)
    @Scheduled(fixedDelay = 300_000L, initialDelay = 300_000L)
    public void calculateEnvCov() {
        List<CoverageReportEntity> resList = coverageReportDao.queryCoverByStatus(Constants.JobStatus.SUCCESS.val(),
                Constants.CoverageFrom.ENV.val(), 10);
        log.info("查询需要拉取exec文件的数据{}条", resList.size());
        resList.forEach(o -> {
            try {
                int num = coverageReportDao.casUpdateByStatus(Constants.JobStatus.SUCCESS.val(),
                        Constants.JobStatus.WAITING.val(), o.getUuid());
                if (num > 0) {
                    // 代码目录不存在说明代码不在这一台机器上，这里会重新下载代码编译，此时若代码有更新，会出现统计代码和本地class不一致
                    // 建议使用commitID替换branch来避免这个问题
                    if (!new File(o.getNowLocalPath()).exists()) {
                        codeCovService.cloneAndCompileCode(o);
                        if (o.getRequestStatus() != Constants.JobStatus.COMPILE_DONE.val()) {
                            log.info("{}计算覆盖率具体步骤...编译失败uuid={}", Thread.currentThread().getName(), o.getUuid());
                            return;
                        }
                    }
                    log.info("others execute exec task uuid={}", o.getUuid());
                    if (o.getType() == Constants.ReportType.DIFF.val() && StringUtils.isEmpty(o.getDiffMethod())) {
                        codeCovService.calculateDeployDiffMethods(o);
                        if (o.getRequestStatus() != Constants.JobStatus.DIFF_METHOD_DONE.val()) {
                            log.info("{}计算覆盖率具体步骤...计算增量代码失败，uuid={}", Thread.currentThread().getName(), o.getUuid());
                            return;
                        }
                    }
                    codeCovService.calculateEnvCov(o);
                    log.info("任务执行结束，uuid={}", o.getUuid());
                } else {
                    log.info("任务已被领取，uuid={}", o.getUuid());
                    return;
                }

            } catch (Exception e) {
                log.error("uuid={}拉取exec文件异常", o.getUuid(), e);
            }
        });
    }

}