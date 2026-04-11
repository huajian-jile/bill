package org.example.bill.repo;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.web.dto.BkpSearchParams;
import org.springframework.data.jpa.domain.Specification;

public final class BkpSpecifications {

    private BkpSpecifications() {}

    public static Specification<BkpWechatBillTransaction> fromParams(BkpSearchParams p) {
        return (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            if (p.channel() != null
                    && !p.channel().isBlank()
                    && !"ALL".equalsIgnoreCase(p.channel())) {
                list.add(cb.equal(root.get("billChannel"), p.channel().trim().toUpperCase()));
            }
            if (p.billImportId() != null) {
                list.add(cb.equal(root.get("billImportId"), p.billImportId()));
            }
            String tf = normalizeStart(p.tradeTimeFrom());
            String tt = normalizeEnd(p.tradeTimeTo());
            if (tf != null) {
                list.add(cb.greaterThanOrEqualTo(root.get("tradeTime"), tf));
            }
            if (tt != null) {
                list.add(cb.lessThanOrEqualTo(root.get("tradeTime"), tt));
            }
            if (p.tradeType() != null && !p.tradeType().isBlank()) {
                list.add(
                        cb.like(
                                cb.lower(root.get("tradeType")),
                                "%" + p.tradeType().trim().toLowerCase() + "%"));
            }
            if (p.counterparty() != null && !p.counterparty().isBlank()) {
                list.add(
                        cb.like(
                                cb.lower(root.get("counterparty")),
                                "%" + p.counterparty().trim().toLowerCase() + "%"));
            }
            if (p.incomeExpense() != null && !p.incomeExpense().isBlank()) {
                list.add(
                        cb.like(
                                cb.lower(root.get("incomeExpense")),
                                "%" + p.incomeExpense().trim().toLowerCase() + "%"));
            }
            if (p.amountMin() != null) {
                list.add(cb.ge(root.get("amountYuan"), p.amountMin()));
            }
            if (p.amountMax() != null) {
                list.add(cb.le(root.get("amountYuan"), p.amountMax()));
            }
            return cb.and(list.toArray(Predicate[]::new));
        };
    }

    private static String normalizeStart(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return s + " 00:00:00";
        }
        return s;
    }

    private static String normalizeEnd(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return s + " 23:59:59";
        }
        return s;
    }
}
