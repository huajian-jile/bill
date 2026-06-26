package org.example.bill.web.dto;

public record ChinaMapRegionDto(
        String code, String name, int level, String mapAdcode, boolean drillable) {}
