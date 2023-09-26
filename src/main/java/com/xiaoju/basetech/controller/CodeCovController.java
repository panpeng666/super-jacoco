package com.xiaoju.basetech.controller;


import com.google.common.collect.Lists;
import com.xiaoju.basetech.entity.*;
import com.xiaoju.basetech.service.CodeCovService;
import com.xiaoju.basetech.util.CodeCloneExecutor;
import com.xiaoju.basetech.util.RobotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

/**
 * @author guojinqiong
 */
@RestController
@RequestMapping(value = "/cov")
@Slf4j
public class CodeCovController {

    @Autowired
    private CodeCovService codeCovService;
    @Autowired
    private CodeCloneExecutor codeCloneExecutor;

    //todo 需要实现的功能
    /**
     0。写一个影子应用，将原有的服务器的请求转发过来 over
     1。用户中心的应用要给对应的机器人发送报告 over
     2。单测失败的代码应该进行清理
     3。用户中心dubbo应用要修改保证不启动应用 over（ 服务器端也需要写轮询kill）
     4。入参的用户和请求应该落库保存 over
     5。需要实现一个清理任务状态的修复脚本，防止卡住
     6。同上，还需要对运行的服务进行监控
     7。修改轮询方式，改为定时任务中轮询 over

     分母去掉一些枚举、工具类

     0913 todo
     1。增加幂等判断 done
     2。增加延迟触发，减少因为未打包导致的单测报错 done
     3。修改底层逻辑，需要同时生成增量单测&全量单测报告 done
        设计思路
        1。修改底层库表结构，在执行单测，生成报告时，同时生成2份报告，存储到一个uuid下（成本较高，改动较大）（放弃）
        2。修改触发逻辑，在进行增量覆盖的时候，同时生成2个uuid，采用uuid+A uuid+D 来区分是否为增量覆盖率（改动成本较低，缺点是性能/硬盘空间占用双倍，在后期设计数据库查询时，还需要注意区别）采用


     4。实现list接口进行分页返回结果
     */

    /**
     * 触发单元测试diff覆盖率，不入参uuid
     *
     * @param
     * @return
     */
    @PostMapping(value = "/triggerUnitCoverTest")
    public HttpResult<Boolean> triggerUnitCoverTest(@RequestBody @Validated CoverBaseWithOutUUidRequest coverBaseWithOutUUidRequest) {
        if (!codeCovService.checkInRule(coverBaseWithOutUUidRequest)){
            log.info("不进行覆盖率检查");
            return HttpResult.build(501,coverBaseWithOutUUidRequest.getGitUrl()+"checkInRule不通过，不进行覆盖率检查");
        }
        String uuid = codeCovService.createUidAndSave(coverBaseWithOutUUidRequest);
        return HttpResult.build(200,uuid);
    }


    /**
     * 触发单元测试diff覆盖率
     *
     * @param unitCoverRequest
     * @return
     */
    @PostMapping(value = "/triggerUnitCover")
    public HttpResult<Boolean> triggerUnitCover(@RequestBody @Validated UnitCoverRequest unitCoverRequest) {
        codeCovService.triggerUnitCov(unitCoverRequest);
        return HttpResult.success();
    }


    /**
     * 返回单元测试覆盖率报告或者任务执行状态
     *
     * @param uuid，触发时携带的UUID
     * @return coverStatus：-1、失败;1、成功；0、进行中
     */
    @GetMapping(value = "/getUnitCoverResult")
    @ResponseBody
    public HttpResult<CoverResult> getCoverResult(@RequestParam(value = "uuid") String uuid) {
        return HttpResult.success(codeCovService.getCoverResult(uuid));
    }

    /**
     *
     * @param envCoverRequest
     * @return
     */
    @RequestMapping(value = "/triggerEnvCov", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpResult<Boolean> triggerEnvCov(@RequestBody @Validated EnvCoverRequest envCoverRequest) {
        codeCovService.triggerEnvCov(envCoverRequest);
        return HttpResult.success();

    }


    /**
     * 获取功能测试增量代码覆盖率
     *
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/getEnvCoverResult", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpResult<CoverResult> getEnvCoverResult(@RequestParam(value = "uuid") String uuid) {
        return HttpResult.success(codeCovService.getCoverResult(uuid));

    }

    /**
     * 手动获取env增量代码覆盖率，代码部署和覆盖率服务在同一机器上，可直接读取本机源码和本机class文件
     *
     * @return
     */
    @RequestMapping(value = "/getLocalCoverResult", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpResult<CoverResult> getEnvLocalCoverResult(@RequestBody @Valid LocalHostRequestParam localHostRequestParam) {

        return HttpResult.success(codeCovService.getLocalCoverResult(localHostRequestParam));

    }




    /**
     *
     * @param envCoverRequest
     * @return
     */
    @RequestMapping(value = "/triggerEnvCovTest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public HttpResult<Boolean> triggerEnvCovTest(@RequestBody @Validated EnvCoverRequest envCoverRequest) {
        codeCovService.triggerEnvCovTest(envCoverRequest);
        return HttpResult.success();

    }

    //分页查询

    @RequestMapping("/getResultList")
    public HttpResult<List<CoverageReportEntity>>getUserIdOrderByPy( @RequestParam("page") int page, @RequestParam("size") int size) {
        List<CoverageReportEntity> res = codeCovService.getResultList(page,size);
        return HttpResult.success(res);
    }


}
