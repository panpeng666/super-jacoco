package com.xiaoju.basetech.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.hutool.http.HttpUtil;

import java.util.List;

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



    @Value("${user.roboturl}")
    private String userRobotUrl;

    @Value("${search.roboturl}")
    private String searchRobotUrl;

    /**
     * 发送msg通知到对应的群机器人
     * @param msg
     */

    public void robotReport(String msg,String robotUrl){
        log.info("进入机器人通知"+msg);
        String json = "{\"timestamp\":1665470557810,\"content\":{\"title\":{\"text\":\"super-jacoco机器人通知\",\"style\":1,\"color\":\"#0D0D1A\"},\"content\":{\"color\":\"#0D0D1A\",\"atIds\":\"userOpenId\",\"style\":1,\"text\":\""+msg +"\"}},\"msg_type\":\"system_card\"}\n";
        String res = HttpUtil.post(robotUrl, json);
        log.info("完成机器人通知"+res);
    }

    public void checkBelong(String gitUrl,String msg){
        if (isUser(gitUrl,msg)){
            return;
        }
        if (isUser(gitUrl,msg)){
            return;
        }
    }

    public boolean isUser(String gitUrl,String msg){
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

}
