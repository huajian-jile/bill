package org.example.bill.web.dto;

import java.util.List;

public record ChinaMapViewDto(List<ChinaMapBreadcrumbDto> breadcrumbs, List<ChinaMapRegionDto> regions) {}
