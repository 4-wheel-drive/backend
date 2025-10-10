package com.pda.trading_service.service.kis;

import com.pda.common_service.exception.MemberException;
import com.pda.common_service.repository.KisTokenReader;
import com.pda.common_service.response.ResponseMessage;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import com.pda.trading_service.service.dto.OrderEventDto;
import com.pda.trading_service.service.kis.dto.KisOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KisOrderService {
    private final KisApiClient kisApiClient;
    private final MemberRepository memberRepository;
    private final KisTokenReader kisTokenReader;

    public KisOrderResponse orderBuy(OrderEventDto dto) {
        Member member = memberRepository.findById(dto.memberId())
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        return kisApiClient.sendBuyOrder(
                member.getMemberAppSecret(),
                member.getMemberAppKey(),
                member.getMemberAccountNumber(),
                kisTokenReader.getMemberAccessToken(member.getId()),
                dto.stockCode(),
                dto.quantity(),
                dto.price()
        ).block();
    }

    public KisOrderResponse orderSell(OrderEventDto dto) {
        Member member = memberRepository.findById(dto.memberId())
                .orElseThrow(() -> new MemberException(ResponseMessage.MEMBER_NOT_FOUND));

        return kisApiClient.sendSellOrder(
                member.getMemberAppSecret(),
                member.getMemberAppKey(),
                member.getMemberAccountNumber(),
                kisTokenReader.getMemberAccessToken(member.getId()),
                dto.stockCode(),
                dto.quantity(),
                dto.price()
        ).block();
    }
}
