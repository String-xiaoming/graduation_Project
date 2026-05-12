package org.txd.guizhoujob.admin.dto;

import lombok.Data;

@Data
public class AdminUserQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String role;
    private Integer status;

    public Integer getOffset() {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (currentPage - 1) * size;
    }
}
