package com.xiaoju.basetech.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * @author: panpeng
 * @Title: RobotUtilsTest
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2023/8/24 20:06
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class RobotUtilsTest {

    @Resource
    RobotUtils robotUtils;
    @Test
    public void robotReport() {
        String msg = robotUtils.buildSuccessMarkDownMsg("panpeng@kanzhun.com","https://qe.kanzhun-inc.com/#/performance/report/view/dc3d3575-df0d-4bf5-9a32-913f3328287a","30%","https://qe.kanzhun-inc.com/#/performance/report/view/dc3d3575-df0d-4bf5-9a32-913f3328287a");
        robotUtils.robotReport(msg,"https://hi-open.zhipin.com/open-apis/bot/hook/49fb1473329546b1a77b3ab731c0b279");
    }
}