package org.example.bill.web.dto;

import java.util.List;

/**
 * 多文件单请求导入结果：与多次单文件串行一致，每文件各一条 import 记录。
 */
public record BatchImportResultDto(int fileCount, List<Long> importIds) {}
