package org.example.bill.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wx_phone_session")
public class WxPhoneSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Column(nullable = false, length = 256)
    private String sessionKey;

    @Column(length = 128)
    private String openid;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    public WxPhoneSession() {}

    public WxPhoneSession(String code, String sessionKey, String openid, Instant expiresAt) {
        this.code = code;
        this.sessionKey = sessionKey;
        this.openid = openid;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
