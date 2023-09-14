package com.xiaoju.basetech.util;

import com.xiaoju.basetech.entity.CoverageReportEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static com.xiaoju.basetech.util.Constants.LOG_PATH;



/**
 * @description:
 * @author: charlynegaoweiwei
 * @time: 2020/4/27 3:38 PM
 */
@Component
@Slf4j
public class UnitTester {
    // 单元测试命令设置超时时间1小时
    private static final Long UNITTEST_TIMEOUT = 3600000L;

    public void executeUnitTest(CoverageReportEntity coverageReport) {

        log.info("进入UnitTester方法，开始拼接cmd命令");

        long startTime = System.currentTimeMillis();
            //这里加个-X方便查问题，又去掉了
        String unittestCmd = "cd " + coverageReport.getNowLocalPath() + "&&mvn clean";
        if (coverageReport.getEnvType() != null && !coverageReport.getEnvType().equals("")) {
            unittestCmd = unittestCmd + " -P=" + coverageReport.getEnvType();
        }
        String logFile = coverageReport.getLogFile().replace(LocalIpUtils.getTomcatBaseUrl()+"logs/", LOG_PATH);
        //SNAPSHOT快照机制。
        //版本号以[-SNAPSHOT]结尾，每次构建都会去远程仓库检查最新版本。
 //       String[] cmd = new String[]{unittestCmd + " -Dmaven.test.skip=false org.jacoco:jacoco-maven-plugin:1.0.2:prepare-agent "
//               String[] cmd = new String[]{unittestCmd + " -Dmaven.test.skip=false org.jacoco:jacoco-maven-plugin:1.0.2-SNAPSHOT:prepare-agent "
        String[] cmd = new String[]{unittestCmd + " -Dmaven.test.skip=false org.jacoco:jacoco-maven-plugin:1.0.2-SNAPSHOT:prepare-agent "
                + "compile test-compile org.apache.maven.plugins:maven-surefire-plugin:2.22.1:test "

                       // todo  特喵的邮箱的代码在compile下就是不执行test，导致覆盖率一直是0，先写死成test试试

//                       + "test org.apache.maven.plugins:maven-surefire-plugin:2.22.1:test "
                + "org.apache.maven.plugins:maven-jar-plugin:2.4:jar org.jacoco:jacoco-maven-plugin:1.0.2-SNAPSHOT:report -Dmaven.test.failure.ignore=true -Dfile.encoding=UTF-8 "

     //                  + "org.apache.maven.plugins:maven-jar-plugin:2.4:jar org.jacoco:jacoco-maven-plugin:0.8.10:report -Dmaven.test.failure.ignore=true -Dfile.encoding=UTF-8 "
                + (StringUtils.isEmpty(coverageReport.getDiffMethod()) ? "" : ("-Djacoco.diffFile=" + coverageReport.getDiffMethod()))
                + ">" + logFile};
        // 超时时间设置为一小时,

        int exitCode;
        try {
            log.info("cmd命令拼接完成，如下：");
            log.info(Arrays.stream(cmd).toArray().toString());
            exitCode = CmdExecutor.executeCmd(cmd, UNITTEST_TIMEOUT);
            log.info("单元测试执行结果exitCode={} uuid={}", exitCode, coverageReport.getUuid());
            if (exitCode == 0) {
                log.info("执行单元测试成功...");
                coverageReport.setRequestStatus(Constants.JobStatus.UNITTEST_DONE.val());
            } else {
                    coverageReport.setRequestStatus(Constants.JobStatus.UNITTEST_FAIL.val());
                    coverageReport.setErrMsg("执行单元测试报错");
            }
        } catch (TimeoutException e) {
            coverageReport.setRequestStatus(Constants.JobStatus.TIMEOUT.val());
            coverageReport.setErrMsg("执行单元测试超时");
        } catch (Exception e) {
            log.error("执行单元测试异常", e);
            coverageReport.setErrMsg("执行单元测试异常:" + e.getMessage());
            coverageReport.setRequestStatus(Constants.JobStatus.UNITTEST_FAIL.val());
        } finally {
            log.info("uuid={} 执行单元测试耗时{}ms", coverageReport.getUuid(), (System.currentTimeMillis() - startTime));
        }
    }
}