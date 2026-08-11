package org.example.bill.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AdminLocal;
import org.example.bill.domain.AdminRegion;
import org.example.bill.repo.AdminLocalRepository;
import org.example.bill.repo.AdminRegionRepository;
import org.example.bill.web.dto.ChinaMapBreadcrumbDto;
import org.example.bill.web.dto.ChinaMapRegionDto;
import org.example.bill.web.dto.ChinaMapViewDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChinaMapServiceImpl implements ChinaMapService {

    private final AdminRegionRepository regionRepository;
    private final AdminLocalRepository localRepository;

    private static final short LEVEL_PROVINCE = 1;
    private static final short LEVEL_CITY = 2;
    private static final short LEVEL_DISTRICT = 3;
    private static final short LEVEL_TOWN = 4;

    @Override
    public ChinaMapViewDto getView(String code) {
        if (code == null || code.isBlank()) {
            return listProvinces();
        }
        return drillDown(code);
    }

    private ChinaMapViewDto listProvinces() {
        List<AdminRegion> provinces = regionRepository.findByLevelOrderByCodeAsc(LEVEL_PROVINCE);
        List<ChinaMapRegionDto> regions = new ArrayList<>();
        for (AdminRegion p : provinces) {
            boolean drillable = localRepository.existsByParentCodeAndLevel(p.getCode(), LEVEL_CITY);
            regions.add(toRegionDto(p.getCode(), p.getName(), p.getLevel(), p.getCode(), drillable));
        }
        return new ChinaMapViewDto(Collections.emptyList(), regions);
    }

    private ChinaMapViewDto drillDown(String code) {
        AdminRegion region = regionRepository.findById(code).orElse(null);
        if (region == null) {
            return new ChinaMapViewDto(Collections.emptyList(), Collections.emptyList());
        }

        List<ChinaMapBreadcrumbDto> breadcrumbs = buildBreadcrumbs(code);
        List<ChinaMapRegionDto> regions = listChildren(code, region.getLevel());
        return new ChinaMapViewDto(breadcrumbs, regions);
    }

    private List<ChinaMapBreadcrumbDto> buildBreadcrumbs(String code) {
        List<ChinaMapBreadcrumbDto> list = new ArrayList<>();
        String current = code;
        while (current != null) {
            AdminRegion r = regionRepository.findById(current).orElse(null);
            if (r == null) {
                break;
            }
            list.add(new ChinaMapBreadcrumbDto(r.getCode(), r.getName(), r.getLevel()));
            current = r.getParentCode();
        }
        Collections.reverse(list);
        return list;
    }

    private List<ChinaMapRegionDto> listChildren(String parentCode, short parentLevel) {
        short childLevel = (short) (parentLevel + 1);

        // Try admin_region first
        List<AdminRegion> regionChildren = regionRepository.findByParentCodeOrderByCodeAsc(parentCode);
        if (!regionChildren.isEmpty()) {
            return regionChildren.stream()
                    .map(r -> {
                        boolean hasChildren = localRepository.existsByParentCodeAndLevel(r.getCode(), (short)(childLevel + 1))
                                || !regionRepository.findByParentCodeOrderByCodeAsc(r.getCode()).isEmpty();
                        return toRegionDto(r.getCode(), r.getName(), r.getLevel(), r.getCode(), hasChildren);
                    })
                    .toList();
        }

        // Fallback to admin_local
        List<AdminLocal> localChildren = localRepository.findByParentCodeOrderByNameAsc(parentCode);
        return localChildren.stream()
                .map(l -> {
                    boolean hasChildren = localRepository.existsByParentCodeAndLevel(l.getCode(), (short)(childLevel + 1));
                    return toRegionDto(l.getCode(), l.getName(), l.getLevel(), l.getAreaCode(), hasChildren);
                })
                .toList();
    }

    private ChinaMapRegionDto toRegionDto(String code, String name, short level, String mapAdcode, boolean drillable) {
        return new ChinaMapRegionDto(code, name, level, mapAdcode, drillable);
    }
}
