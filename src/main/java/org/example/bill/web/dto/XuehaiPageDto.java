package org.example.bill.web.dto;

import java.util.List;

public record XuehaiPageDto<T>(List<T> items, int page, int size, long total) {}
