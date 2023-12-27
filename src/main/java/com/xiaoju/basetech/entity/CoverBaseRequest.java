package com.xiaoju.basetech.entity;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: charlynegaoweiwei
 * @time: 2020/4/26 7:52 PM
 */
@Data
public class CoverBaseRequest {
    /**
     * uuid是必须的，在后续查询结果时需要使用
     */
    @NotBlank(message = "uuid不能为空")
    private String uuid;

    @NotBlank(message = "gitUrl不能为空")
    private String gitUrl;

    /**
     * git工程名称
    */
    private String gitName;

    //@NotBlank(message = "baseVersion不能为空")
    private String baseVersion="master";

    @NotBlank(message = "nowVersion不能为空")
    private String nowVersion;

    /**
     * 同一个git仓库可能存在多个模块，subModule为相对路径，如果为空，则代表整个git仓库
     */
    private String subModule;

    /**
     * 1、全量；2、增量
     */
    @NotNull(message = "type不能为空")
    @Max(value = 2)
    @Min(value = 1)
    private Integer type;

    /**
     * 0=非mr请求，1=mr请求触发
     */
    @Max(value = 1)
    @Min(value = 0)
    private Integer mrRequest;


    /**
     *是否进行机器人汇报
     * 0=未进行汇报，1=进行了汇报 2=正在执行监控检查 3= 无需汇报
     */
    @Max(value = 3)
    @Min(value = 0)
    private Integer isRobotReport;

    /**
     * mr触发时候的请求入参，方便排查问题
     */
    private String requestInfo;


    private String mrUrl ;
    private String mrUserMail;
}