package org.example.bill.service;

import org.example.bill.web.dto.ChinaMapViewDto;

public interface ChinaMapService {

    /**
     * @param code 地区编码，null 表示全国视图，否则下钻到指定地区
     */
    ChinaMapViewDto getView(String code);
}
