package com.xiaoju.basetech.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.hutool.http.HttpUtil;

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

}
