package com.peak.vo;

import lombok.Data;

import java.util.Map;

/** POST /api/v1/files/presign 响应 data — API.md §2.2 */
@Data
public class PresignResponseVO {
    private String uploadUrl;
    private String fileId;
    private String accessUrl;
    private Map<String, String> formData;
}
