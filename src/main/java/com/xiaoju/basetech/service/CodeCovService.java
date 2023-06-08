package com.xiaoju.basetech.service;

import com.xiaoju.basetech.entity.*;


/**
 * @author didi
 */
public interface CodeCovService {

    /**
     * 新增单元覆盖率增量覆盖率任务
     *
     * @param unitCoverRequest
     */
    void triggerUnitCov(UnitCoverRequest unitCoverRequest);


    /**
     * @Description: 一个检测单测是否完成的轮训任务
     * @param: uuid
     * @return * @return void
     * @author panpeng
     * @date 2023/6/8 11:08
    */
    void checkJobDone(String uuid) throws Exception;


    /**
     * 获取覆盖率结果，单元测试和功能测试统一用这个接口
     *
     * @param uuid
     * @return
     */
    CoverResult getCoverResult(String uuid);

    /**
     * @param envCoverRequest
     */

    void triggerEnvCov(EnvCoverRequest envCoverRequest);

    void cloneAndCompileCodeTest(CoverageReportEntity coverageReport);

    /**
     * 克隆代码&&编译代码
     *
     * @param coverageReport
     */
    void cloneAndCompileCode(CoverageReportEntity coverageReport);

    /**
     * 获取diff
     *
     * @param coverageReport
     */
    void calculateDeployDiffMethods(CoverageReportEntity coverageReport);

    /**
     * 计算单元测试覆盖率
     *
     * @param coverageReport
     */
    void calculateUnitCover(CoverageReportEntity coverageReport);

    /**
     * 计算单元测试覆盖率（改写方法）
     *
     * @param
     */
    void triggerEnvCovTest(EnvCoverRequest envCoverRequest);

    /**
     * 计算手工测试覆盖率，和环境配合使用
     *
     * @param coverageReport
     */
    void calculateEnvCov(CoverageReportEntity coverageReport);

    /**
     * 手动获取手工测试覆盖率
     *
     * @param localHostRequestParam
     * @return
     * @throws Exception
     */
    CoverResult getLocalCoverResult(LocalHostRequestParam localHostRequestParam);

}
