package com.pda.strategy_service.repository.jpa;

import com.pda.common_service.user.domain.Member;
import com.pda.strategy_service.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // StockOrder를 통해 회원의 거래 조회
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "ORDER BY t.executionTime DESC")
    List<Transaction> findAllByStockOrderStrategyMemberOrderByExecutionTimeDesc(@Param("member") Member member);

    // StockOrder를 통해 회원의 거래 조회 (페이징)
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "ORDER BY t.executionTime DESC")
    Page<Transaction> findAllByStockOrderStrategyMemberOrderByExecutionTimeDesc(@Param("member") Member member,
                                                                                Pageable pageable);

    // 특정 종목에 대한 회원의 거래 조회
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "AND t.stock.code = :stockCode " +
            "ORDER BY t.executionTime DESC")
    List<Transaction> findAllByMemberAndStockCodeOrderByExecutionTimeDesc(
            @Param("member") Member member,
            @Param("stockCode") String stockCode);

    // 전략별 현재 보유 수량 계산
    @Query("SELECT COALESCE(SUM(CASE WHEN t.tradeExecutionType = 'BUY' THEN t.tradeExecutionQuantity " +
            "WHEN t.tradeExecutionType = 'SELL' THEN -t.tradeExecutionQuantity END), 0) " +
            "FROM Transaction t " +
            "WHERE t.stockOrder.strategy.id = :strategyId " +
            "AND t.stock.code = :stockCode")
    Integer calculateCurrentQuantity(@Param("strategyId") Long strategyId, @Param("stockCode") String stockCode);

    // 회원의 특정 종목에 대한 모든 전략의 보유 수량 합산
    @Query("SELECT COALESCE(SUM(CASE WHEN t.tradeExecutionType = 'BUY' THEN t.tradeExecutionQuantity " +
            "WHEN t.tradeExecutionType = 'SELL' THEN -t.tradeExecutionQuantity END), 0) " +
            "FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "AND t.stock.code = :stockCode")
    Integer calculateStockQuantityByMember(@Param("member") Member member, @Param("stockCode") String stockCode);

    // 회원의 특정 종목에 대한 순투자금 계산 (매수금액 - 매도금액)
    @Query("SELECT COALESCE(SUM(CASE WHEN t.tradeExecutionType = 'BUY' " +
            "THEN t.tradeExecutionPrice * t.tradeExecutionQuantity " +
            "WHEN t.tradeExecutionType = 'SELL' " +
            "THEN -t.tradeExecutionPrice * t.tradeExecutionQuantity END), 0) " +
            "FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "AND t.stock.code = :stockCode")
    BigDecimal calculateNetInvestmentByMember(@Param("member") Member member, @Param("stockCode") String stockCode);

    // 회원의 특정 종목에 대한 평균 매수 단가 계산 (매수 거래만 고려)
    @Query("SELECT CASE WHEN SUM(CASE WHEN t.tradeExecutionType = 'BUY' THEN t.tradeExecutionQuantity ELSE 0 END) > 0 "
            +
            "THEN SUM(CASE WHEN t.tradeExecutionType = 'BUY' THEN t.tradeExecutionPrice * t.tradeExecutionQuantity ELSE 0 END) / "
            +
            "SUM(CASE WHEN t.tradeExecutionType = 'BUY' THEN t.tradeExecutionQuantity ELSE 0 END) " +
            "ELSE 0 END " +
            "FROM Transaction t " +
            "WHERE t.stockOrder.strategy.member = :member " +
            "AND t.stock.code = :stockCode")
    BigDecimal calculateAverageBuyPriceByMember(@Param("member") Member member, @Param("stockCode") String stockCode);

    @Query("SELECT t FROM Transaction t " +
            "JOIN t.stockOrder so " +
            "JOIN so.strategy s " +
            "WHERE s.id = :strategyId " +
            "ORDER BY t.executionTime ASC")
    List<Transaction> findAllByStockOrderStrategy(@Param("strategyId") Long strategyId);

}

