package com.xiaoju.basetech.util;

import com.xiaoju.basetech.entity.CoverageReportEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.hutool.http.HttpUtil;

import java.util.List;
import java.util.Objects;

/**
 * @author: panpeng
 * @Title: RobotUtils
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2023/6/8 11:00
 */
@Component
@Slf4j
public class RobotUtils {
    @Value("${user.server}")
    private String[] userListNames;
    @Value("${search.server}")
    private String[] searchListNames;
    @Value("${cockatiel.server}")
    private String[] cockatielListNames;



    @Value("${user.roboturl}")
    private String userRobotUrl;

    @Value("${search.roboturl}")
    private String searchRobotUrl;

    @Value("${cockatiel.roboturl}")
    private String cockatielRobotUrl;

    /**
     * 发送msg通知到对应的群机器人
     * @param msg
     */
    //todo 这里的逻辑太不优雅了，应该使用对象传递，而不是一个个参数传递

    public  void robotReport(String msg, String robotUrl){
        log.info("进入机器人通知"+msg);
        String json = "{\"msg_type\":\"compressive_card\",\"content\":{\"compressiveCardContent\":\"{\\\"modules\\\":[{\\\"tag\\\":\\\"markdown\\\",\\\"content\\\":\\\""+msg+"\\\",\\\"textAlign\\\":\\\"left\\\"}],\\\"header\\\":{\\\"text\\\":{\\\"content\\\":\\\"super-jacoco报告\\\",\\\"tag\\\":\\\"plain_text\\\"},\\\"color\\\":\\\"blue\\\"}}\"}}";

        log.info(json);
        String res = HttpUtil.post(robotUrl, json);
        log.info("完成机器人通知"+res);
    }

    /**
     * @Description: 生成robot机器人的markDown文本
     * @param: user
     * @param: mrUrl
     * @param: reportUrl
     * @return * @return java.lang.String
     * @author panpeng
     * @date 2023/8/24 20:38
    */
    public  String buildSuccessMarkDownMsg(String user, String mrUrl, String result, String reportUrl){
        if (Objects.isNull(user)||Objects.isNull(mrUrl)||Objects.isNull(reportUrl)||Objects.isNull(result)){
            log.error("构建机器人通知markDown文本时异常");
            return "";
        }
        String msg = "用户**"+user+"**的mr请求\\n[点击查看mr请求详情]("+mrUrl+")\\n单测增量覆盖率完成，结果为"+result+"\\n[点击查看覆盖率报告]("+reportUrl+")\\n";
        log.info(msg);
        return msg;
    }

    public  String buildSuccessMarkDownMsg(String user, String mrUrl,String gitName,String commit, String diff_result, String diff_reportUrl,String full_result, String full_reportUrl){
        if (Objects.isNull(user)||Objects.isNull(mrUrl)||Objects.isNull(diff_result)||Objects.isNull(diff_reportUrl)||Objects.isNull(full_result)||Objects.isNull(full_reportUrl)||Objects.isNull(gitName)||Objects.isNull(commit)){
            log.error("构建机器人通知markDown文本时异常");
            return "";
        }
        String msg = "**"+gitName+"**工程的**"+commit+"**分支\\n"+"用户**"+user+"**的mr请求\\n[点击查看mr请求详情]("+mrUrl+")\\n单测增量覆盖率完成，结果为"+diff_result+"\\n[点击查看覆盖率报告]("+diff_reportUrl+")\\n单测全量覆盖率完成，结果为"+full_result+"\\n[点击查看覆盖率报告]("+full_reportUrl+")\\n";
        log.info(msg);
        return msg;
    }

    public  String buildFailMarkDownMsg(String user, String mrUrl ,String gitName,String commit,String result, String reportUrl){
        if (Objects.isNull(user)||Objects.isNull(mrUrl)||Objects.isNull(reportUrl)||Objects.isNull(result)||Objects.isNull(gitName)||Objects.isNull(commit)){
            log.error("构建机器人通知markDown文本时异常");
            return "";
        }
        String msg = "**"+gitName+"**工程的**"+commit+"**分支\\n"+"用户**"+user+"**的mr请求\\n[点击查看mr请求详情]("+mrUrl+")\\n单测增量覆盖率执行失败，原因为"+result+"\\n[点击查看报错日志]("+reportUrl+")\\n";
        log.info(msg);
        return msg;
    }

    /**
     * @Description: 改成对象入参，减少后续改动风险
     * @param: cr_diff
     * @param: cr_full
     * @return * @return java.lang.String
     * @author panpeng
     * @date 2023/10/13 15:39
    */
    public  String buildSuccessMarkDownMsg(CoverageReportEntity cr_diff,CoverageReportEntity cr_full){
        String user = cr_diff.getMrUserMail();
        String mrUrl = cr_diff.getGitUrl();
        String diff_result = String.valueOf(cr_diff.getBranchCoverage());
        String full_result = String.valueOf(cr_full.getBranchCoverage());
        String diff_reportUrl = cr_diff.getReportUrl();
        String full_reportUrl = cr_full.getReportUrl();
        //新增git工程，commit分支的消息通知
        String gitName = cr_diff.getGitName();
        String commit = cr_diff.getNowVersion();
        if (Objects.isNull(user)||Objects.isNull(mrUrl)||Objects.isNull(diff_result)||Objects.isNull(diff_reportUrl)||Objects.isNull(full_result)||Objects.isNull(full_reportUrl)||Objects.isNull(gitName)||Objects.isNull(commit)){
            log.error("构建机器人通知markDown文本时异常");
            return "";
        }
        String msg = "**"+gitName+"**工程的**"+commit+"**分支\\n"+"用户**"+user+"**的mr请求\\n[点击查看mr请求详情]("+mrUrl+")\\n单测增量覆盖率完成，结果为"+diff_result+"\\n[点击查看覆盖率报告]("+diff_reportUrl+")\\n单测全量覆盖率完成，结果为"+full_result+"\\n[点击查看覆盖率报告]("+full_reportUrl+")\\n";
        log.info(msg);
        return msg;
    }

    public  String buildFailMarkDownMsg(CoverageReportEntity cr){
        String user = cr.getMrUserMail();
        String mrUrl = cr.getGitUrl();
        String gitName = cr.getGitName();
        String commit = cr.getNowVersion();
        String reportUrl = cr.getLogFile();
        String result = cr.getErrMsg();



        if (Objects.isNull(user)||Objects.isNull(mrUrl)||Objects.isNull(reportUrl)||Objects.isNull(result)||Objects.isNull(gitName)||Objects.isNull(commit)){
            log.error("构建机器人通知markDown文本时异常");
            return "";
        }
        String msg = "**"+gitName+"**工程的**"+commit+"**分支\\n"+"用户**"+user+"**的mr请求\\n[点击查看mr请求详情]("+mrUrl+")\\n单测增量覆盖率执行失败，原因为"+result+"\\n[点击查看报错日志]("+reportUrl+")\\n";
        log.info(msg);
        return msg;
    }

    public  void checkBelong(String gitUrl, String msg){
        if (isUser(gitUrl,msg)){
            return;
        }
        if (isSearch(gitUrl,msg)){
            return;
        }
        if (isCockatiel(gitUrl,msg)){
            return;
        }
    }

    public  boolean isUser(String gitUrl,String msg){
        for (String userName:userListNames){
            if (gitUrl.contains(userName)){
                robotReport(msg,userRobotUrl);
                return true;
            }
        }
        return false;
    }
    public boolean isSearch(String gitUrl,String msg){
        for (String searchName:searchListNames){
            if (gitUrl.contains(searchName)){
                robotReport(msg,searchRobotUrl);
                return true;
            }
        }
        return false;
    }

    public boolean isCockatiel(String gitUrl,String msg){
        for (String cockatielName:cockatielListNames){
            if (gitUrl.contains(cockatielName)){
                robotReport(msg,cockatielRobotUrl);
                return true;
            }
        }
        return false;
    }


}
