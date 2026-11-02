package com.peak.vo;

import lombok.Data;

/** 参考文件 VO — OrderDetailVO.referenceFiles 子项 */
@Data
public class ReferenceFileVO {
    private Long id;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
