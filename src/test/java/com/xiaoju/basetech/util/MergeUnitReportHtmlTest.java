package com.xiaoju.basetech.util;

import com.xiaoju.basetech.dao.CoverageReportDao;
import com.xiaoju.basetech.dao.UnitTestResultDao;
import com.xiaoju.basetech.entity.CoverageReportEntity;
import com.xiaoju.basetech.entity.UnitTestResultEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author: panpeng
 * @Title: MergeUnitReportHtmlTest
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2024/2/22 20:41
 */
@SpringBootTest
public class MergeUnitReportHtmlTest {
    @Resource
    private CoverageReportDao coverageReportDao;


    @Test
    public void getSubModuleSitePaths() {
//        List<String> temp = MergeUnitReportHtml.getSubModuleSitePaths("/Users/pandoudou/app/super_jacoco/clonecode/1708585957272/feature_flow_color");
//        System.out.println(666);
//        Iterator<String> iterator = temp.iterator();
//// 遍历并打印
//        while (iterator.hasNext()) {
//            String item = iterator.next();
//            System.out.println(item);
//
//        }
//        CoverageReportEntity cr = new CoverageReportEntity();
//        cr.setUuid("15757164357");
//        cr.setGitName("pp");
//
//        List<String> temp2 = MergeUnitReportHtml.copyReport(temp,cr);
//        Iterator<String> iterator2 = temp2.iterator();
//        while (iterator2.hasNext()) {
//            String item = iterator2.next();
//            System.out.println(item);
//
//        }
//        HashMap map = MergeUnitReportHtml.calculateUnitTestResult(temp2);
//        System.out.println(map.toString());
//
//        MergeUnitReportHtml.buildHtmlReport(map,temp2,"/Users/pandoudou/app/1.html");
    }

    @Test
    public void testSql(){

//        List<String> temp = new ArrayList<>();
//        temp.add("/Users/pandoudou/report/1709640648786_FULL/cockatiel-gaea-flow/cockatiel-gaea-flow-test/");
//        temp.add("/Users/pandoudou/report/1709640648786_FULL/cockatiel-gaea-flow/cockatiel-gaea-flow-facade/");
//        CoverageReportEntity cr = new CoverageReportEntity();
//        cr.setUuid("1709640648786_FULL");
//        List<String> res = MergeUnitReportHtml.buildWebPath(temp,cr);
//        System.out.println(res);

    }


}