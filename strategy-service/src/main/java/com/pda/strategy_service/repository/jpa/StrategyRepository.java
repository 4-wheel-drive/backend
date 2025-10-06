package com.pda.strategy_service.repository.jpa;

import com.pda.common_service.user.domain.Member;
import com.pda.strategy_service.domain.Strategy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    List<Strategy> findAllByMember(Member member);
}
