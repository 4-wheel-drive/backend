package com.pda.strategy_service.repository.jpa;

import com.pda.common_service.user.domain.Member;
import com.pda.strategy_service.domain.Strategy;
import com.pda.strategy_service.domain.StrategyExistedStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    List<Strategy> findAllByMember(Member member);
    List<Strategy> findAllByMemberId(Long memberId);
    List<Strategy> findAllByMemberAndStrategyExistedStatus(Member member, StrategyExistedStatus status);
    Optional<Strategy> findByIdAndStrategyExistedStatus(Long strategyId, StrategyExistedStatus status);
    List<Strategy> findAllByMemberAndStrategyExistedStatusOrderByCreatedAtDesc(Member member,
                                                                               StrategyExistedStatus status);
}
