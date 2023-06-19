package com.xiaoju.basetech.controller;


import com.xiaoju.basetech.entity.*;
import com.xiaoju.basetech.service.CodeCovService;
import com.xiaoju.basetech.util.RobotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    //todo 需要实现的功能
    /**
     * 1。需要有一个controller去接受rust的请求，来触发启动增量单测检查 done
     * 2。触发完成后，需要走单测覆盖率流程 done
     * 3。需要自动生成这个uuid，并且每分钟轮询这个线程是否完成 done
     * 4。需要实现一个机器人通知的server（可以抄mqtt的） done
     * 5。最好对用户信息进行记录
     */

    /**
     * 触发单元测试diff覆盖率，不入参uuid
     *
     * @param
     * @return
     */
    @PostMapping(value = "/triggerUnitCoverTest")
    public HttpResult<Boolean> triggerUnitCoverTest(@RequestBody @Validated CoverBaseWithOutUUidRequest coverBaseWithOutUUidRequest) {
        //加一个rd21组判断，不然没权限直接gg
        String giturl = coverBaseWithOutUUidRequest.getGitUrl();
        if (!giturl.contains("git.kanzhun-inc.com/rd21/")){
            log.info("为非后端代码url，不进行增量代码覆盖率检查"+ coverBaseWithOutUUidRequest.toString());
            return HttpResult.build(500,giturl+"为非后端代码url，不进行增量代码覆盖率检查");
        }

        //uuid由时间戳生成
        String uuid = String.valueOf(System.currentTimeMillis());
        UnitCoverRequest unitCoverRequest = new UnitCoverRequest();
        unitCoverRequest.setUuid(uuid);
        //如果type为null设置成全量执行      * 1、全量；2、增量
        if (Objects.isNull(coverBaseWithOutUUidRequest.getType())){
            unitCoverRequest.setType(1);
            coverBaseWithOutUUidRequest.setType(1);
        }
        //如果对比分支为null，写成develop
        if (Objects.isNull(coverBaseWithOutUUidRequest.getBaseVersion())){
            unitCoverRequest.setBaseVersion("develop");
        }
        //入参全部打印到日志
        log.info("uuid=" +uuid+ "开始执行增量代码检查，入参为"+ coverBaseWithOutUUidRequest.toString());

        BeanUtils.copyProperties(coverBaseWithOutUUidRequest,unitCoverRequest);
        String url = coverBaseWithOutUUidRequest.getUrl();
        String userMail = coverBaseWithOutUUidRequest.getUserMail();
        codeCovService.triggerUnitCov(unitCoverRequest);
        //启动一个轮询检查，60min后超时
        new Thread(()->{
            try {
                codeCovService.checkJobDone(uuid,url,userMail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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


}
