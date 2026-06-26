package org.example.bill.web.dto;

/**
 * @param status {@code immediate} 已直接绑定；{@code pending_review} 已提交管理员审核
 */
public record PhoneBindResponse(String status) {}
