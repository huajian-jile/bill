package org.example.bill.service;

import java.util.Collections;
import java.util.List;
import org.example.bill.web.dto.ChinaMapBreadcrumbDto;
import org.example.bill.web.dto.ChinaMapRegionDto;
import org.example.bill.web.dto.ChinaMapViewDto;
import org.springframework.stereotype.Service;

@Service
public class ChinaMapServiceImpl implements ChinaMapService {

    @Override
    public ChinaMapViewDto getView(String code) {
        if (code == null || code.isBlank()) {
            // 全国视图
            return new ChinaMapViewDto(
                    Collections.emptyList(),
                    List.of() // TODO: 填充全国省份列表
            );
        }
        // TODO: 根据 code 下钻到省/市/区县
        return new ChinaMapViewDto(
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
