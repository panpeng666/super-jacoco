package com.xiaoju.basetech.dao;

import com.xiaoju.basetech.entity.UnitTestResultEntity;
import org.apache.ibatis.annotations.Param;


/**
 * @author: panpeng
 * @Title: UnitTestResultDao
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2024/3/5 10:19
 */
public interface UnitTestResultDao {


    int insert(UnitTestResultEntity unitTestResult);

    UnitTestResultEntity queryByTestCaseId(@Param("jobRecordUuid") String jobRecordUuid);



}
