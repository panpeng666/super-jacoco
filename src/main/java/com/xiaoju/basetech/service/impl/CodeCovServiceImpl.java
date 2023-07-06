package com.xiaoju.basetech.service.impl;


import com.google.common.base.Preconditions;
import com.xiaoju.basetech.dao.CoverageReportDao;
import com.xiaoju.basetech.dao.DeployInfoDao;
import com.xiaoju.basetech.entity.*;
import com.xiaoju.basetech.service.CodeCovService;
import com.xiaoju.basetech.util.*;
import jodd.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.tomcat.jni.Time;
import org.jacoco.core.tools.ExecFileLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static com.xiaoju.basetech.util.Constants.*;


/**
 * @description:
 * @author: gaoweiwei_v
 * @time: 2019/7/9 3:26 PM
 */
@Slf4j
@Service
public class CodeCovServiceImpl implements CodeCovService {
    private static final String JACOCO_PATH = System.getProperty("user.home") + "/org.jacoco.cli-1.0.2-SNAPSHOT-nodeps.jar";
    private static final String COV_PATH = System.getProperty("user.home") + "/cover/";
    //普通命令超时时间是10分钟,600000L 143
    private static final Long CMD_TIMEOUT = 600000L;
    @Value("${whitelist.names}")
    private String[] whiteListNames;

    @Autowired
    private CoverageReportDao coverageReportDao;
    @Autowired
    private DeployInfoDao deployInfoDao;

    @Autowired
    private DiffMethodsCalculator diffMethodsCalculator;
    @Autowired
    private CodeCloneExecutor codeCloneExecutor;

    @Autowired
    private CodeCompilerExecutor codeCompilerExecutor;

    @Autowired
    private ReportCopyExecutor reportCopyExecutor;

    @Autowired
    private MavenModuleUtil mavenModuleUtil;

    @Autowired
    private UnitTester unitTester;

    @Autowired
    private ReportParser reportParser;

    @Autowired
    private RobotUtils robotUtils;

    //mr群机器人地址
   //
    //
    //
    //
    // private  String robotUrl = "https://hi-open.zhipin.com/open-apis/bot/hook/c1855c2ba5da4f2bb35097c7deb2e45a";
    //测试机器人地址
    private String robotUrl = "https://hi-open.zhipin.com/open-apis/bot/hook/49fb1473329546b1a77b3ab731c0b279";
    /**
     * 新增单元覆盖率增量覆盖率任务
     *
     * @param unitCoverRequest
     */
    @Override
    public void triggerUnitCov(UnitCoverRequest unitCoverRequest) {
        CoverageReportEntity history = coverageReportDao.queryCoverageReportByUuid(unitCoverRequest.getUuid());
        double x =10.1;

        //先对uuid查库判断是否存在，存在的时候报错

        if (history != null) {
            throw new ResponseException(ErrorCode.FAIL, String.format("uuid:%s已经调用过，请勿重复触发！",
                    unitCoverRequest.getUuid()));
        }

        CoverageReportEntity coverageReport = new CoverageReportEntity();
        try {
            //复制属性
            BeanUtils.copyProperties(coverageReport, unitCoverRequest);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //设置单元测试的标识参数
        coverageReport.setFrom(Constants.CoverageFrom.UNIT.val());
        //设置JobStatus为 初始数据 0
        coverageReport.setRequestStatus(Constants.JobStatus.INITIAL.val());
        if (StringUtils.isEmpty(coverageReport.getSubModule())) {
            coverageReport.setSubModule("");
        }
        //数据写入数据库，等待定时任务去捞取
        coverageReportDao.insertCoverageReportById(coverageReport);
    }

    /**
     * 获取覆盖率结果
     *
     * @param uuid
     * @return
     */
    @Override
    public CoverResult getCoverResult(String uuid) {
        Preconditions.checkArgument(!StringUtils.isEmpty(uuid), "uuid不能为空");

        CoverageReportEntity coverageReport = coverageReportDao.queryCoverageReportByUuid(uuid);
        CoverResult result = new CoverResult();
        if (coverageReport == null) {
            result.setCoverStatus(-1);
            result.setLineCoverage(-1);
            result.setBranchCoverage(-1);
            result.setErrMsg("uuid对应的报告不存在");
            return result;
        }

        try {
            BeanUtils.copyProperties(result, coverageReport);
            String logFile = coverageReport.getLogFile().replace(LOG_PATH, LocalIpUtils.getTomcatBaseUrl());
            result.setLogFile(logFile);
            if (coverageReport.getRequestStatus() < Constants.JobStatus.SUCCESS.val()) {
                result.setCoverStatus(0);
                result.setErrMsg("正在统计增量覆盖率..." + Constants.JobStatus.desc(coverageReport.getRequestStatus()));
            } else if (coverageReport.getRequestStatus() > Constants.JobStatus.SUCCESS.val()) {
                result.setCoverStatus(-1);
                result.setErrMsg("统计失败:" + coverageReport.getErrMsg());
                result.setBranchCoverage(-1);
                result.setLineCoverage(-1);
            } else {
                result.setCoverStatus(1);
            }
            return result;

        } catch (Exception e) {
            throw new ResponseException(e.getMessage());
        }
    }


    /**
     * 计算覆盖率具体步骤
     *
     * @param coverageReport
     */
    @Override
    public void calculateUnitCover(CoverageReportEntity coverageReport) {
        long s = System.currentTimeMillis();
        log.info("{}计算覆盖率具体步骤...开始执行uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());

        // 下载代码
        coverageReport.setRequestStatus(Constants.JobStatus.CLONING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        codeCloneExecutor.cloneCode(coverageReport);
        // 更新状态
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.CLONE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...克隆失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }

        // 计算增量方法,（此处是对比2个分支的代码区别，先生成初步的git diff报告
        coverageReport.setRequestStatus(Constants.JobStatus.DIFF_METHODS_EXECUTING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        diffMethodsCalculator.executeDiffMethods(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.DIFF_METHOD_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...计算增量方法uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        // 添加集成模块（去pom增加对应的maven
        coverageReport.setRequestStatus(Constants.JobStatus.ADDMODULING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        mavenModuleUtil.addMavenModule(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.ADDMODULE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...添加集成模块失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }

        // 执行单元测试（cmd 执行 mvn test
        coverageReport.setRequestStatus(Constants.JobStatus.UNITTESTEXECUTING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        unitTester.executeUnitTest(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.UNITTEST_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...单元测试失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }

        //分析覆盖率报告
        coverageReport.setRequestStatus(Constants.JobStatus.REPORTPARSING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        reportParser.parseReport(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.PARSEREPORT_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...分析报告失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }

        //复制报告到指定目录
        coverageReport.setRequestStatus(Constants.JobStatus.REPORTCOPYING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        reportCopyExecutor.copyReport(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.COPYREPORT_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...复制报告失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        try {
            coverageReport.setRequestStatus(Constants.JobStatus.SUCCESS.val());
            //这里会删除代码
//            FileUtil.cleanDir(new File(coverageReport.getNowLocalPath()).getParent());
//        } catch (IOException e) {
        } catch (Exception e) {
            log.error("uuid={}删除代码失败..", coverageReport.getUuid(), e);
            coverageReport.setRequestStatus(Constants.JobStatus.REMOVE_FILE_FAIL.val());
        }
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        log.info("{}计算覆盖率具体步骤...执行完成，耗时{}ms", Thread.currentThread().getName(),
                System.currentTimeMillis() - s);
        return;

    }

    @Override
    public void calculateDeployDiffMethods(CoverageReportEntity coverageReport) {
        // 计算增量方法
        coverageReport.setRequestStatus(Constants.JobStatus.DIFF_METHODS_EXECUTING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        diffMethodsCalculator.executeDiffMethods(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
    }

    @Override
    public void cloneAndCompileCodeTest(CoverageReportEntity coverageReport) {
        // 下载代码
        coverageReport.setRequestStatus(Constants.JobStatus.CLONING.val());
        //数据先落库
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        //进行代码拉取
        codeCloneExecutor.cloneCodeTest(coverageReport);
        // 更新数据库状态
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.CLONE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...克隆失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        //编译代码
        coverageReport.setRequestStatus(Constants.JobStatus.COMPILING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);

        codeCompilerExecutor.compileCode(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != JobStatus.COMPILE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...编译失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        DeployInfoEntity deployInfo = new DeployInfoEntity();
        deployInfo.setUuid(coverageReport.getUuid());
        deployInfo.setCodePath(coverageReport.getNowLocalPath());
        String pomPath = deployInfo.getCodePath() + "/pom.xml";
        ArrayList<String> moduleList = MavenModuleUtil.getValidModules(pomPath);
        StringBuilder moduleNames = new StringBuilder("");
        for (String module : moduleList) {
            moduleNames.append(module + ",");
        }
        deployInfo.setChildModules(moduleNames.toString());
        int i = deployInfoDao.updateDeployInfo(deployInfo);
        if (i < 1) {
            log.info("{}计算覆盖率具体步骤...获取ChildModules失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
    }


    @Override
    public void cloneAndCompileCode(CoverageReportEntity coverageReport) {
        // 下载代码
        coverageReport.setRequestStatus(Constants.JobStatus.CLONING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        codeCloneExecutor.cloneCode(coverageReport);
        // 更新状态
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != Constants.JobStatus.CLONE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...克隆失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        //编译代码
        coverageReport.setRequestStatus(Constants.JobStatus.COMPILING.val());
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        codeCompilerExecutor.compileCode(coverageReport);
        coverageReportDao.updateCoverageReportByReport(coverageReport);
        if (coverageReport.getRequestStatus() != JobStatus.COMPILE_DONE.val()) {
            log.info("{}计算覆盖率具体步骤...编译失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
        DeployInfoEntity deployInfo = new DeployInfoEntity();
        deployInfo.setUuid(coverageReport.getUuid());
        deployInfo.setCodePath(coverageReport.getNowLocalPath());
        String pomPath = deployInfo.getCodePath() + "/pom.xml";
        ArrayList<String> moduleList = MavenModuleUtil.getValidModules(pomPath);
        StringBuilder moduleNames = new StringBuilder("");
        for (String module : moduleList) {
            moduleNames.append(module + ",");
        }
        deployInfo.setChildModules(moduleNames.toString());
        int i = deployInfoDao.updateDeployInfo(deployInfo);
        if (i < 1) {
            log.info("{}计算覆盖率具体步骤...获取ChildModules失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
            return;
        }
    }

    /**
     * @param envCoverRequest
     */

    @Override
    public void triggerEnvCov(EnvCoverRequest envCoverRequest) {
        try {
            CoverageReportEntity coverageReport = new CoverageReportEntity();
            coverageReport.setFrom(Constants.CoverageFrom.ENV.val());
            coverageReport.setEnvType("");
            coverageReport.setUuid(envCoverRequest.getUuid());
            coverageReport.setGitUrl(envCoverRequest.getGitUrl());
            coverageReport.setNowVersion(envCoverRequest.getNowVersion());
            coverageReport.setType(envCoverRequest.getType());

            if (!StringUtils.isEmpty(envCoverRequest.getBaseVersion())) {
                coverageReport.setBaseVersion(envCoverRequest.getBaseVersion());
            }
            if (!StringUtils.isEmpty(envCoverRequest.getSubModule())) {
                coverageReport.setSubModule(envCoverRequest.getSubModule());
            }

            if (envCoverRequest.getBaseVersion().equals(envCoverRequest.getNowVersion()) && envCoverRequest.getType() == Constants.ReportType.DIFF.val()) {
                coverageReport.setBranchCoverage((double) 100);
                coverageReport.setLineCoverage((double) 100);
                coverageReport.setRequestStatus(Constants.JobStatus.NODIFF.val());
                coverageReport.setErrMsg("没有增量方法");
                coverageReportDao.insertCoverageReportById(coverageReport);
                return;
            }

            coverageReport.setRequestStatus(Constants.JobStatus.WAITING.val());
            coverageReportDao.insertCoverageReportById(coverageReport);
            deployInfoDao.insertDeployId(envCoverRequest.getUuid(), envCoverRequest.getAddress(), envCoverRequest.getPort());
            new Thread(() -> {
                cloneAndCompileCode(coverageReport);
                if (coverageReport.getRequestStatus() != Constants.JobStatus.COMPILE_DONE.val()) {
                    log.info("{}计算覆盖率具体步骤...编译失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
                    return;
                }
                if (coverageReport.getType() == Constants.ReportType.DIFF.val()) {
                    calculateDeployDiffMethods(coverageReport);
                    if (coverageReport.getRequestStatus() != Constants.JobStatus.DIFF_METHOD_DONE.val()) {
                        log.info("{}计算覆盖率具体步骤...计算增量代码失败，uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
                        return;
                    }
                }
                calculateEnvCov(coverageReport);
            }).start();
        } catch (Exception e) {
            throw new ResponseException(e.getMessage());
        }

    }

    /**
     * @param envCoverRequest
     * 改动原方法，由于目标服务器内存不足，无法执行mvn ，改写该方法，用于本机拉取git代码并执行mvn
     */

    @Override
    public void triggerEnvCovTest(EnvCoverRequest envCoverRequest) {
        try {
            //进行数据处理
            CoverageReportEntity coverageReport = new CoverageReportEntity();
            coverageReport.setFrom(Constants.CoverageFrom.ENV.val());
            coverageReport.setEnvType("");
            coverageReport.setUuid(envCoverRequest.getUuid());
            coverageReport.setGitUrl(envCoverRequest.getGitUrl());
            coverageReport.setNowVersion(envCoverRequest.getNowVersion());
            coverageReport.setType(envCoverRequest.getType());
            //baseVersion字段为null时不处理
            if (!StringUtils.isEmpty(envCoverRequest.getBaseVersion())) {
                coverageReport.setBaseVersion(envCoverRequest.getBaseVersion());
            }
            //setSubModule非空处理加一
            if (!StringUtils.isEmpty(envCoverRequest.getSubModule())) {
                coverageReport.setSubModule(envCoverRequest.getSubModule());
            }
            //判断分支名称是否一致，若是增量type，会return异常
            if (envCoverRequest.getBaseVersion().equals(envCoverRequest.getNowVersion()) && envCoverRequest.getType() == Constants.ReportType.DIFF.val()) {
                coverageReport.setBranchCoverage((double) 100);
                coverageReport.setLineCoverage((double) 100);
                coverageReport.setRequestStatus(Constants.JobStatus.NODIFF.val());
                coverageReport.setErrMsg("没有增量方法");
                coverageReportDao.insertCoverageReportById(coverageReport);
                return;
            }

            coverageReport.setRequestStatus(Constants.JobStatus.WAITING.val());
            //数据准备完毕，先落库
            coverageReportDao.insertCoverageReportById(coverageReport);
            deployInfoDao.insertDeployId(envCoverRequest.getUuid(), envCoverRequest.getAddress(), envCoverRequest.getPort());
            //起一个线程开始执行命令
            new Thread(() -> {
                //拉取代码
                cloneAndCompileCodeTest(coverageReport);
                if (coverageReport.getRequestStatus() != Constants.JobStatus.COMPILE_DONE.val()) {
                    log.info("{}计算覆盖率具体步骤...编译失败uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
                    return;
                }
                if (coverageReport.getType() == Constants.ReportType.DIFF.val()) {
                    calculateDeployDiffMethods(coverageReport);
                    if (coverageReport.getRequestStatus() != Constants.JobStatus.DIFF_METHOD_DONE.val()) {
                        log.info("{}计算覆盖率具体步骤...计算增量代码失败，uuid={}", Thread.currentThread().getName(), coverageReport.getUuid());
                        return;
                    }
                }
                calculateEnvCov(coverageReport);
            }).start();
        } catch (Exception e) {
            throw new ResponseException(e.getMessage());
        }

    }


    /**
     * 从项目机器上拉取功能测试的执行轨迹.exec文件，计算增量方法覆盖率
     *
     * @param coverageReport
     * @return
     */
    @Override
    public void calculateEnvCov(CoverageReportEntity coverageReport) {
        String logFile = coverageReport.getLogFile().replace(LocalIpUtils.getTomcatBaseUrl() + "logs/", LOG_PATH);
        String uuid = coverageReport.getUuid();
        DeployInfoEntity deployInfoEntity = deployInfoDao.queryDeployId(uuid);
        String reportName = "ManualDiffCoverage";
        if (coverageReport.getType() == 1) {
            reportName = "ManualCoverage";
        }

        try {
            int exitCode = CmdExecutor.executeCmd(new String[]{"cd " + coverageReport.getNowLocalPath() + "&&java -jar " +
                    JACOCO_PATH + " dump --address " + deployInfoEntity.getAddress() + " --port " +
                    deployInfoEntity.getPort() + " --destfile ./jacoco.exec"}, CMD_TIMEOUT);

            if (exitCode == 0) {
                CmdExecutor.executeCmd(new String[]{"rm -rf " + REPORT_PATH + coverageReport.getUuid()}, CMD_TIMEOUT);
                String[] moduleList = deployInfoEntity.getChildModules().split(",");
                StringBuilder builder = new StringBuilder("java -jar " + JACOCO_PATH + " report " + deployInfoEntity.getCodePath() + "/jacoco.exec ");
                // 单模块的时候没有moduleList
                if (moduleList.length == 0) {
                    builder.append("--sourcefiles ./src/main/java/ ");
                    //builder.append("--classfiles ./target/classes/com/ ");
                    //可以说是非常坑了，面霸只有cn
                    builder.append("--classfiles ./target/classes/ ");
                } else {
                    // 多模块
                    for (String module : moduleList) {
                        builder.append("--sourcefiles ./" + module + "/src/main/java/ ");
                       // builder.append("--classfiles ./" + module + "/target/classes/com/ ");
                        builder.append("--classfiles ./" + module + "/target/classes/ ");

                    }
                }
                if (!StringUtils.isEmpty(coverageReport.getDiffMethod())) {
                    builder.append("--diffFile " + coverageReport.getDiffMethod());

                }
                builder.append(" --html ./jacocoreport/ --encoding utf-8 --name " + reportName + ">>" + logFile);
                int covExitCode = CmdExecutor.executeCmd(new String[]{"cd " + deployInfoEntity.getCodePath() + "&&" + builder.toString()}, CMD_TIMEOUT);
                File reportFile = new File(deployInfoEntity.getCodePath() + "/jacocoreport/index.html");
                if (covExitCode == 0 && reportFile.exists()) {
                    try {
                        // 解析并获取覆盖率
                        Document doc = Jsoup.parse(reportFile.getAbsoluteFile(), "UTF-8", "");
                        Elements bars = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("bar");
                        Elements lineCtr1 = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("ctr1");
                        Elements lineCtr2 = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("ctr2");
                        double lineCoverage = 100;
                        double branchCoverage = 100;
                        // 以上这里初始化都换成了1
                        if (doc != null && bars != null) {
                            float lineNumerator = Float.valueOf(lineCtr1.get(1).text().replace(",", ""));
                            float lineDenominator = Float.valueOf(lineCtr2.get(3).text().replace(",", ""));
                            lineCoverage = (lineDenominator - lineNumerator) / lineDenominator * 100;
                            String[] branch = bars.get(1).text().split(" of ");
                            float branchNumerator = Float.valueOf(branch[0].replace(",", ""));
                            float branchDenominator = Float.valueOf(branch[1].replace(",", ""));
                            if (branchDenominator > 0.0) {
                                branchCoverage = (branchDenominator - branchNumerator) / branchDenominator * 100;
                            }
                        }
                        // 复制report报告
                        String[] cppCmd = new String[]{"cp -rf " + reportFile.getParent() + " " + REPORT_PATH + coverageReport.getUuid() + "/"};
                        CmdExecutor.executeCmd(cppCmd, CMD_TIMEOUT);
                        coverageReport.setReportUrl(LocalIpUtils.getTomcatBaseUrl() + coverageReport.getUuid() + "/index.html");
                        coverageReport.setRequestStatus(Constants.JobStatus.SUCCESS.val());
                        coverageReport.setLineCoverage(lineCoverage);
                        coverageReport.setBranchCoverage(branchCoverage);
                        return;
                    } catch (Exception e) {
                        coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
                        coverageReport.setErrMsg("解析jacoco报告失败");
                        log.error("uuid={}", coverageReport.getUuid(), e.getMessage());
                    }
                } else {
                    coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
                    // 可能不同子项目存在同一类名
                    int littleExitCode = 0;
                    ArrayList<String> childReportList = new ArrayList<>();
                    for (String module : moduleList) {
                        StringBuilder buildertmp = new StringBuilder("java -jar " + JACOCO_PATH + " report ./jacoco.exec");
                        buildertmp.append(" --sourcefiles ./" + module + "/src/main/java/");
                      //  buildertmp.append(" --classfiles ./" + module + "/target/classes/com/");
                        buildertmp.append(" --classfiles ./" + module + "/target/classes/ ");

                        if (!StringUtils.isEmpty(coverageReport.getDiffMethod())) {
                            builder.append("--diffFile " + coverageReport.getDiffMethod());
                        }
                        buildertmp.append(" --html jacocoreport/" + module + " --encoding utf-8 --name " + reportName + ">>" + logFile);
                        littleExitCode += CmdExecutor.executeCmd(new String[]{"cd " + deployInfoEntity.getCodePath() + "&&" + buildertmp.toString()}, CMD_TIMEOUT);
                        if (littleExitCode == 0) {
                            childReportList.add(deployInfoEntity.getCodePath() + "/jacocoreport/" + module + "/index.html");
                        }
                    }
                    if (littleExitCode == 0) {
                        // 合并
                        CmdExecutor.executeCmd(new String[]{"cd " + deployInfoEntity.getCodePath() + "&&cp -rf jacocoreport " + REPORT_PATH + coverageReport.getUuid() + "/"}, CMD_TIMEOUT);
                        Integer[] result = MergeReportHtml.mergeHtml(childReportList, REPORT_PATH + coverageReport.getUuid() + "/index.html");

                        if (result[0] > 0) {
                            coverageReport.setReportUrl(LocalIpUtils.getTomcatBaseUrl() + coverageReport.getUuid() + "/index.html");
                            coverageReport.setRequestStatus(Constants.JobStatus.SUCCESS.val());
                            FileUtil.cleanDir(new File(coverageReport.getNowLocalPath()).getParent());
                            CmdExecutor.executeCmd(new String[]{"cp -r " + JACOCO_RESOURE_PATH + " " + REPORT_PATH + coverageReport.getUuid()}, CMD_TIMEOUT);
                            coverageReport.setLineCoverage(Double.valueOf(result[2]));
                            coverageReport.setBranchCoverage(Double.valueOf(result[1]));
                            return;
                        } else {
                            coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
                            coverageReport.setErrMsg("生成jacoco报告失败 ");
                        }
                    } else {
                        // 生成报告错误
                        coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
                        coverageReport.setErrMsg("生成jacoco报告失败");
                    }
                }
            } else {
                coverageReport.setErrMsg("获取jacoco.exec 文件失败");
                coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
                log.error("uuid={}", coverageReport.getUuid(), coverageReport.getErrMsg());
                FileUtil.cleanDir(new File(coverageReport.getNowLocalPath()).getParent());
            }
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("uuid={}获取超时", coverageReport.getUuid());
        } catch (Exception e) {
            coverageReport.setRequestStatus(Constants.JobStatus.ENVREPORT_FAIL.val());
            coverageReport.setErrMsg("获取jacoco.exec 文件发生未知错误");
            log.error("uuid={}获取jacoco.exec 文件发生未知错误", coverageReport.getUuid(), e);
            log.error("uuid={}", coverageReport.getUuid(), coverageReport.getErrMsg());
        } finally {
            coverageReportDao.updateCoverageReportByReport(coverageReport);
        }
    }

    @Override
    public CoverResult getLocalCoverResult(LocalHostRequestParam localHostRequestParam) {
        //path 处理
        localHostRequestParam.setBasePath(localHostRequestParam.getBasePath().endsWith("/") ? localHostRequestParam.getBasePath() : (localHostRequestParam.getBasePath() + "/"));
        localHostRequestParam.setNowPath(localHostRequestParam.getNowPath().endsWith("/") ? localHostRequestParam.getNowPath() : (localHostRequestParam.getNowPath() + "/"));
        //1、计算增量代码
        String diffFiles = diffMethodsCalculator.executeDiffMethodsForEnv(localHostRequestParam.getBasePath(), localHostRequestParam.getNowPath(), localHostRequestParam.getBaseVersion(), localHostRequestParam.getNowVersion());
        CoverResult result = new CoverResult();
        if (diffFiles == null) {
            result.setCoverStatus(-1);
            result.setLineCoverage(-1);
            result.setBranchCoverage(-1);
            result.setErrMsg("未检测到增量代码");
            return result;
        }
        //2、拉取jacoco.exec文件并解析
        if (StringUtils.isEmpty(localHostRequestParam.getAddress())) {
            localHostRequestParam.setAddress("127.0.0.1");
        }
        CoverResult coverResult = pullExecFile(localHostRequestParam, diffFiles, localHostRequestParam.getSubModule());
        //3、tomcat整合
        //todo
        return coverResult;
    }

    /**
     * 拉取jacoco文件并转换为报告
     */
    private CoverResult pullExecFile(LocalHostRequestParam localHostRequestParam, String diffFiles, String subModule) {
        String reportName = "ManualDiffCoverage";
        localHostRequestParam.setClassFilePath(localHostRequestParam.getClassFilePath().endsWith("/") ? localHostRequestParam.getClassFilePath() : (localHostRequestParam.getClassFilePath() + "/"));
        CoverResult coverResult = new CoverResult();
        try {
            int exitCode = CmdExecutor.executeCmd(new String[]{"cd " + localHostRequestParam.getNowPath() + "&&java -jar " +
                    JACOCO_PATH + " dump --address " + localHostRequestParam.getAddress() + " --port " +
                    localHostRequestParam.getPort() + " --destfile  ./jacoco.exec"}, CMD_TIMEOUT);

            if (exitCode == 0) {
                //todo 删除原有报告
                // CmdExecutor.executeCmd(new String[]{"rm -rf " + REPORT_PATH + coverageReport.getUuid()}, CMD_TIMEOUT);

                StringBuilder builder = new StringBuilder("java -jar " + JACOCO_PATH + " report " + localHostRequestParam.getNowPath() + "jacoco.exec ");
                // 单模块的时候没有moduleList
                if (subModule.isEmpty()) {
                    builder.append("--sourcefiles ./src/main/java/ ");
                    //builder.append("--classfiles ./target/classes/com/ ");
                    builder.append("--classfiles ./target/classes/ ");

                } else {
                    // 多模块

                    builder.append("--sourcefiles ./" + subModule + "/src/main/java/ ");
                  //  builder.append("--classfiles ./" + subModule + "/target/classes/com/ ");
                    builder.append("--classfiles ./" + subModule + "/target/classes/ ");

                }
                if (!StringUtils.isEmpty(diffFiles)) {
                    builder.append("--diffFile " + diffFiles);

                }
                builder.append(" --html ./jacocoreport/ --encoding utf-8 --name " + reportName);
                log.info("builder={}", builder);
                int covExitCode = CmdExecutor.executeCmd(new String[]{"cd " + localHostRequestParam.getNowPath() + "&&" + builder.toString()}, CMD_TIMEOUT);
                File reportFile = new File(localHostRequestParam.getNowPath() + "jacocoreport/index.html");

                if (covExitCode == 0 && reportFile.exists()) {
                    try {
                        // 解析并获取覆盖率
                        log.info("开始解析html元素");
                        Document doc = Jsoup.parse(reportFile.getAbsoluteFile(), "UTF-8", "");
                        Elements bars = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("bar");
                        Elements lineCtr1 = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("ctr1");
                        Elements lineCtr2 = doc.getElementById("coveragetable").getElementsByTag("tfoot").first().getElementsByClass("ctr2");
                        double lineCoverage = 100;
                        double branchCoverage = 100;
                        // 以上这里初始化都换成了1
                        if (doc != null && bars != null) {
                            float lineNumerator = Float.valueOf(lineCtr1.get(1).text().replace(",", ""));
                            float lineDenominator = Float.valueOf(lineCtr2.get(3).text().replace(",", ""));
                            log.info("lineNumerator={},lineDenominator={}", lineNumerator, lineDenominator);
                            lineCoverage = (lineDenominator - lineNumerator) / lineDenominator * 100;
                            String[] branch = bars.get(1).text().split(" of ");
                            float branchNumerator = Float.valueOf(branch[0].replace(",", ""));
                            float branchDenominator = Float.valueOf(branch[1].replace(",", ""));
                            log.info("branchNumerator={},branchDenominator={}", branchNumerator, branchDenominator);
                            if (branchDenominator > 0.0) {
                                branchCoverage = (branchDenominator - branchNumerator) / branchDenominator * 100;
                            }

                        }
                        coverResult.setCoverStatus(200);
                        coverResult.setLineCoverage(lineCoverage);
                        coverResult.setBranchCoverage(branchCoverage);
                        coverResult.setReportUrl(localHostRequestParam.getNowPath() + "jacocoreport/index.html");
                        return coverResult;
                        // todo 复制report报告

                    } catch (RuntimeException e) {
                        log.error("解析jacoco报告失败，msg={}", e.getMessage());
                        throw new RuntimeException("解析jacoco报告失败，msg=" + e.getMessage());
                    }
                } else {
                    // 可能不同子项目存在同一类名
                    int littleExitCode = 0;
                    ArrayList<String> childReportList = new ArrayList<>();

                    StringBuilder buildertmp = new StringBuilder("java -jar " + JACOCO_PATH + " report ./jacoco.exec");
                    buildertmp.append(" --sourcefiles ./" + subModule + "/src/main/java/");
                   // buildertmp.append(" --classfiles ./" + subModule + "/target/classes/com/");
                    buildertmp.append(" --classfiles ./" + subModule + "/target/classes/ ");
                    if (!StringUtils.isEmpty(diffFiles)) {
                        builder.append("--diffFile " + diffFiles);
                    }
                    buildertmp.append(" --html jacocoreport/" + subModule + " --encoding utf-8 --name " + reportName);
                    littleExitCode += CmdExecutor.executeCmd(new String[]{"cd " + localHostRequestParam.getNowPath() + "&&" + buildertmp.toString()}, CMD_TIMEOUT);
                    if (littleExitCode == 0) {
                        childReportList.add(localHostRequestParam.getNowPath() + "/jacocoreport/" + subModule + "/index.html");
                    }

                    if (littleExitCode == 0) {
                        // 合并
                        //todo 报告地址
                        CmdExecutor.executeCmd(new String[]{"cd " + localHostRequestParam.getNowPath() + "&&cp -rf jacocoreport " + COV_PATH + localHostRequestParam.getUuid() + "/"}, CMD_TIMEOUT);
                        Integer[] result = MergeReportHtml.mergeHtml(childReportList, COV_PATH + localHostRequestParam.getUuid() + "/index.html");

                        if (result[0] > 0) {
                            //todo 清理
                            coverResult.setCoverStatus(200);
                            coverResult.setLineCoverage(Double.valueOf(result[2]));
                            coverResult.setBranchCoverage(Double.valueOf(result[1]));
                            coverResult.setReportUrl(COV_PATH + localHostRequestParam.getUuid() + "/jacocoreport/index.html");
                            return coverResult;
                        } else {
                            coverResult.setCoverStatus(-1);
                            coverResult.setErrMsg("拉取执行文件失败");
                            return coverResult;
                        }
                    } else {
                        // 生成报告错误
                        coverResult.setCoverStatus(-1);
                        coverResult.setErrMsg("拉取执行文件失败");
                        return coverResult;
                    }
                }
            } else {
                coverResult.setCoverStatus(-1);
                coverResult.setErrMsg("拉取执行文件失败");
                log.error("获取jacoco.exec 文件失败，uuid={}", localHostRequestParam.getUuid());
                return coverResult;
            }
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("获取jacoco.exec 文件失败，uuid={}获取超时", localHostRequestParam.getUuid());
            throw new ResponseException(e.getMessage());
        } catch (Exception e) {
            log.error("uuid={}获取jacoco.exec 文件发生未知错误", localHostRequestParam.getUuid(), e);
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * @Description: 定时轮询uuid的报告是否完成
     * @param: uuid
     * @return * @return void
     * @author panpeng
     * @date 2023/6/8 11:44
    */
    @Override
    public void checkJobDone(String uuid,String url,String userMail) throws Exception {
        log.info("定时轮询检测任务启动，检测uuid为"+uuid);
        int count = 60;
        CoverResult coverResult = new CoverResult();
        while (coverResult.getCoverStatus()==0){
            Thread.sleep(60000);
            coverResult = getCoverResult(uuid);
            count--;
            if (count<=0){
                log.info("uuid为"+uuid+"的轮询任务已经超时，结束轮询任务");
                return;
            }
        }
        //todo 如果代码已经合并，git diff的数据为null时，此时应该不通知（先自己加判断，后续还是改入参会比较好）
        CoverageReportEntity cr = coverageReportDao.queryCoverageReportByUuid(uuid);
        if (cr.getErrMsg().equals("没有增量代码")){
            log.info(uuid+"没有增量代码");
            return;
        }

        //发送机器人通知
        if (coverResult.getCoverStatus()==1){
            log.info("uuid为"+uuid+"的报告生成成功，开始发送机器人消息至mr群");

            String reportUrl = coverResult.getReportUrl();
            cr = coverageReportDao.queryCoverageReportByUuid(uuid);
            String baseVersion  = cr.getBaseVersion();
            String nowVersion = cr.getNowVersion();
            Double lineCoverage = coverResult.getLineCoverage();
            String msg = "用户"+userMail+"的mr请求\\n"+url+"\\n单测覆盖率完成\\n"+"\\n增量代码单测的行覆盖率为"+lineCoverage+"；\\n具体报告可见"+reportUrl;
            //先加一个搜索的判断
            //这里需要把判断应用隶属于哪个群组的判断迁移到robotUtils中
            robotUtils.checkBelong(cr.getGitUrl(), msg);

//            if(cr.getGitUrl().contains("zhipin-cockatiel-store") || cr.getGitUrl().contains("zhipin-cockatiel-search")){
//                log.info("搜索团队的应用，对搜索平台进行机器人通知");
//                robotUtils.robotReport(msg,"https://hi-open.zhipin.com/open-apis/bot/hook/61b1f3b519e34c7e9ccb84e2d03c4e0e");
//            }else {
//            robotUtils.robotReport(msg,robotUrl);
//            }
        }else if( coverResult.getCoverStatus()==-1) {
            String logUrl = coverResult.toString();
            String msg = "生成增量代码覆盖率失败，请检查日志"+logUrl;
            cr = coverageReportDao.queryCoverageReportByUuid(uuid);
            robotUtils.checkBelong(cr.getGitUrl(), msg);
//            if(cr.getGitUrl().contains("zhipin-cockatiel-store") || cr.getGitUrl().contains("zhipin-cockatiel-search")){
//                log.info("搜索团队的应用，对搜索平台进行机器人通知");
//                robotUtils.robotReport(msg,"https://hi-open.zhipin.com/open-apis/bot/hook/61b1f3b519e34c7e9ccb84e2d03c4e0e");
//            }else {
//                robotUtils.robotReport(msg, robotUrl);
//            }
        }

    }



    private void mergeExec(List<String> ExecFiles, String NewFileName) {
        ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            for (String ExecFile : ExecFiles) {
                execFileLoader.load(new File(ExecFile));
            }
        } catch (Exception e) {
            log.error("ExecFiles 合并失败 errorMessege is {}", e.fillInStackTrace());
        }
        try {
            execFileLoader.save(new File(NewFileName), false);
        } catch (Exception e) {
            log.error("ExecFiles 保存失败 errorMessege is {}", e.fillInStackTrace());
        }
    }


    @Override
    public Boolean whiteList(String gitName){
        for (String whiteListName : whiteListNames) {
            if (gitName.contains(whiteListName)) {
                return true;
            }
        }
        return false;
    }

}
