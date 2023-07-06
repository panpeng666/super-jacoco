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
     0。写一个影子应用，将原有的服务器的请求转发过来 over
     1。用户中心的应用要给对应的机器人发送报告 over
     2。单测失败的代码应该进行清理
     3。用户中心dubbo应用要修改保证不启动应用（服务器端也需要写轮询kill）
     4。入参的用户和请求应该落库保存
     5。需要实现一个清理任务状态的修复脚本，防止卡住
     6。同上，还需要对运行的服务进行监控
     7。修改轮询方式，改为定时任务中轮询
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


        //加一个白名单，只对白名单进行单测检查
        if (!codeCovService.whiteList(giturl)){
            log.info("为非白名单代码url，不进行增量代码覆盖率检查"+ coverBaseWithOutUUidRequest.toString());
            return HttpResult.build(500,giturl+"为非白名单代码url，不进行增量代码覆盖率检查");
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
        //todo 这里最好改一下轮询检查，放到job中进行触发
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
