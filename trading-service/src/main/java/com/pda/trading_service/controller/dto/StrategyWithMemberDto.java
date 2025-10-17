package com.pda.trading_service.controller.dto;

import com.pda.common_service.user.domain.dto.MemberDto;

public record StrategyWithMemberDto(
        StrategyMetaDto strategyMetaDto,
        MemberDto memberDto
) {
}

