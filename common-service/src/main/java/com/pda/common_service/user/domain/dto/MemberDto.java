package com.pda.common_service.user.domain.dto;

public record MemberDto(
        Long id,
        String memberId,
        String memberName,
        String memberAccountNumber,
        String appKey,
        String appSecret
) {
}
