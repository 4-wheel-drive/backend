package com.pda.common_service.stock.repository;

import com.pda.common_service.stock.MemberStock;
import com.pda.common_service.user.domain.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberStockRepository extends JpaRepository<MemberStock, Long> {
    List<MemberStock> findByMember(Member member);
}
