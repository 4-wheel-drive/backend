package com.pda.strategy_service.domain.dto;

import com.pda.common_service.user.domain.dto.MemberDto;

public record StrategyWithMemberDto(
        StrategyMetaDto strategyMetaDto,
        MemberDto memberDto
) {
}
