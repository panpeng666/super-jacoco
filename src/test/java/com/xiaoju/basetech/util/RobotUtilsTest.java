package com.xiaoju.basetech.util;

import com.xiaoju.basetech.BaseTest;
import com.xiaoju.basetech.entity.CoverBaseWithOutUUidRequest;
import com.xiaoju.basetech.entity.CoverageReportEntity;
import com.xiaoju.basetech.service.CodeCovService;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author: panpeng
 * @Title: RobotUtilsTest
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2023/8/24 20:06
 */

@Slf4j
public class RobotUtilsTest extends BaseTest {
    @Resource
    CodeCovService codeCovService;

//    @Resource
//    RedisUtil redisUtil;
//    @Resource
//     RedisTemplate redisTemplate;
//
//    @Test
//    public void robotReport() {
//        String msg = RobotUtils.buildSuccessMarkDownMsg("user@example.com","https://example.com/report","30%","https://example.com/report");
//        RobotUtils.robotReport(msg,"https://hi-open.zhipin.com/open-apis/bot/hook/YOUR_TOKEN");
//    }
//
//    @Test
//    public void setRedisTest(){
//        CoverBaseWithOutUUidRequest cr = new CoverBaseWithOutUUidRequest();
//        String key = "panpeng001";
//        cr.setMrStatus("666");
//        cr.setType(1);
//        cr.setGitUrl("66666");
//        cr.setNowVersion("nowVersion");
//        redisUtil.setCacheObject(key,cr);
//        log.info("缓存完成");
//        System.out.println(redisUtil.hasKey(key));
//        System.out.println(redisUtil.getCacheObjects(key));
//        redisUtil.getCacheObject(key);
//        log.info("test");
////        CoverBaseWithOutUUidRequest tp = (CoverBaseWithOutUUidRequest) redisUtil.getCacheSet("panpeng001");
////        log.info(tp.toString());
//    }
//
//    @Test
//    public void getRedisTest(){
//        CoverBaseWithOutUUidRequest cr = (CoverBaseWithOutUUidRequest) redisUtil.getCacheSet("panpeng001");
//        log.info(cr.toString());
//    }
//
//    @Test
//    public void test(){
//        List<CoverageReportEntity> res = codeCovService.getResultList(1,10);
//        System.out.println(res);
//    }
}