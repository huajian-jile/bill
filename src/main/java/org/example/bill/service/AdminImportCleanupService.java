package org.example.bill.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillImport;
import org.example.bill.repo.WechatBillImportRepository;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.util.PhoneUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminImportCleanupService {

    private final WechatBillImportRepository importRepo;
    private final WechatBillTransactionRepository txRepo;

    @Transactional
    public DeleteResult deleteImportsByMobileAndChannel(String mobileCnRaw, String channelRaw) {
        PhoneUtil.requireValidCnMobile(mobileCnRaw);
        String mobileCn = PhoneUtil.normalizeCnMobile(mobileCnRaw);
        String ch = normalizeChannel(channelRaw);

        List<String> channels = new ArrayList<>();
        if ("ALL".equals(ch)) {
            channels.add("WECHAT");
            channels.add("ALIPAY");
        } else {
            channels.add(ch);
        }

        long deletedImports = 0;
        long deletedTx = 0;
        for (String channel : channels) {
            List<WechatBillImport> imports = importRepo.findByMobileCnAndChannel(mobileCn, channel);
            if (imports.isEmpty()) {
                continue;
            }
            List<Long> importIds = imports.stream().map(WechatBillImport::getId).toList();
            deletedTx += txRepo.deleteByBillImportIdIn(importIds);
            importRepo.deleteAllById(importIds);
            deletedImports += importIds.size();
        }
        return new DeleteResult(mobileCn, ch, deletedImports, deletedTx);
    }

    private static String normalizeChannel(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "ALL";
        }
        s = s.toUpperCase();
        return switch (s) {
            case "WECHAT", "ALIPAY", "ALL" -> s;
            case "WX" -> "WECHAT";
            default -> throw new IllegalArgumentException("未知渠道: " + raw);
        };
    }

    public record DeleteResult(String mobileCn, String channel, long deletedImportRecords, long deletedTransactions) {}
}

