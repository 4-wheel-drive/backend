package com.pda.common_service.user.domain;

import com.pda.common_service.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255, unique = true)
    private String memberId;

    @Column(columnDefinition = "TEXT")
    private String memberPassword;

    @Column(length = 255)
    private String memberName;

    @Column(length = 255)
    private String memberAccountNumber;

    @Column(columnDefinition = "TEXT")
    private String memberAppKey;

    @Column(columnDefinition = "TEXT")
    private String memberAppSecret;

    public static Member create(String memberId, String memberPassword, String memberName,
                                String memberAccountNumber, String memberAppKey, String memberAppSecret) {
        return Member.builder()
                .memberId(memberId)
                .memberPassword(memberPassword)
                .memberName(memberName)
                .memberAccountNumber(memberAccountNumber)
                .memberAppKey(memberAppKey)
                .memberAppSecret(memberAppSecret)
                .build();
    }
}
