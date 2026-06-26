package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.service.ChinaMapService;
import org.example.bill.web.dto.ChinaMapViewDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/china-map")
@RequiredArgsConstructor
public class ChinaMapController {

    private final ChinaMapService chinaMapService;

    /** 获取当前层级视图：默认全国，传 code 下钻到省/市/区县 */
    @GetMapping("/view")
    public ChinaMapViewDto view(@RequestParam(value = "code", required = false) String code) {
        return chinaMapService.getView(code);
    }
}
