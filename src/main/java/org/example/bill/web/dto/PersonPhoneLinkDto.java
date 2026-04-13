package org.example.bill.web.dto;

public record PersonPhoneLinkDto(
        long linkId, long personId, String personLabel, long phoneId, String mobileCn) {}
