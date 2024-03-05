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
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

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


    /**
     *  单元测试结果展示html
     * @param uuid
     * @return
     */

    @RequestMapping(value ="/UnitTestResult", method = RequestMethod.GET)
    public ModelAndView unitTestResult(@RequestParam("uuid") String uuid){
        UnitTestResultEntity u = codeCovService.queryResById(uuid);
        if (Objects.nonNull(u.getLogPath())&&u.getLogPath().equals("ERROR")){
            log.error("生成单元测试报告失败，请检查+uuid="+uuid);
        }
        ModelAndView modelAndView = new ModelAndView();
        //组装module
        modelAndView.addObject("totalCases", u.getCaseNum());
        modelAndView.addObject("successes", u.getSuccessNum());
        modelAndView.addObject("failures", u.getFailNum());
        modelAndView.addObject("skips", u.getSkipNum());
        modelAndView.addObject("passRate", u.getPassRate());

        //处理子模块链接
        List<String> modulePaths = u.getModulePathList();
        List<LinkDto> links = new ArrayList<>();
        for (String path : modulePaths) {
            links.add(new LinkDto(path));
        }
        modelAndView.addObject("links", links);
        modelAndView.setViewName("UnitTestResult");
        return modelAndView;
    }

    // LinkDto 类用来包装每个链接的信息
    static class LinkDto {
        private final String url;

        public LinkDto(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

}
