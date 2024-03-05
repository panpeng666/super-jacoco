package com.xiaoju.basetech.entity;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: panpeng
 * @Title: UnitTestResultEntity
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2024/3/5 10:09
 */
@Data
public class UnitTestResultEntity {
    private Long id;
    private String jobRecordUuid;
    private Integer caseNum;
    private Integer successNum;
    private Integer failNum;
    private Integer skipNum;
    private Double passRate;
    private String modulePath;
    private Date createTime;
    private Date updateTime;
    private String logPath;

    public List<String> getModulePathList() {
        if (this.modulePath != null) {
            return Arrays.stream(this.modulePath.split("\\s*,\\s*"))  // 使用逗号作为分隔符，可以根据实际情况调整
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    // 如果需要设置List<String>到logFilePathStr，也需要添加相应的方法，这里也假设使用逗号分隔
    public void setModulePathList(List<String> logFilePathList) {
        this.modulePath = logFilePathList.stream()
                .collect(Collectors.joining(","));
    }
}
