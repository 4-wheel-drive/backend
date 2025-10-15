//package com.pda.trading_service.scheduler;
//
//import com.pda.trading_service.repository.StockOrderRepository;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ExecutionCommitScheduler {
//
//    private final StockOrderRepository stockOrderRepository;
//    private final KisTradeExecutionService kisTradeExecutionService;
//
//    @Scheduled(cron = "0 30 15 * * *", zone = "Asia/Seoul") // 오후 3시 30분
//    public void runDailyMarketTask() {
//        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
//
//        log.info("[ExecutionCommitScheduler] 체결 배치 시작: {}", today);
//
//        try {
//            // ✅ 오늘 날짜 기준 체결조회 API 호출
//            KisDailyCcldResponse response = kisTradeExecutionService.getDailyCcld(today);
//
//            if (response == null || response.output1().isEmpty()) {
//                log.info("🚫 [{}] 장 휴장일 또는 체결내역 없음 → 작업 스킵", today);
//                return;
//            }
//
//            // ✅ 정상 영업일 → 체결 처리 로직 수행
//            log.info("✅ [{}] 영업일 확인 → 체결 배치 수행", today);
//
//            // TODO: stockOrderRepository 기반 체결 반영 로직
//            // 예: stockOrderService.commitExecutions(response);
//
//        } catch (Exception e) {
//            log.error("❌ [{}] 체결 배치 중 오류 발생", today, e);
//        }
//    }
//}
