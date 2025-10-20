package com.pda.auth_service.scheduler;

import com.pda.common_service.config.KisAdminConfig;
import com.pda.auth_service.service.kis.KisAuthService;
import com.pda.auth_service.service.kis.dto.KisApprovalResponse;
import com.pda.auth_service.service.kis.dto.KisTokenResponse;
import com.pda.common_service.exception.KisException;
import com.pda.common_service.user.domain.Member;
import com.pda.common_service.user.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenScheduler {

    private final KisAuthService kisAuthService;
    private final MemberRepository memberRepository;
    private final KisAdminConfig kisAdminConfig;

    @PostConstruct
    public void init() {
        refreshAdminAccessToken();
        refreshAdminApprovalToken();
        refreshKisTokens();
    }

    /**
     * 매일 오전 3시(한국시간)에 회원 토큰 갱신
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void refreshKisTokens() {
        log.info("KIS Token Refresh Scheduler 시작 (Asia/Seoul)");
        try {
            List<Member> members = memberRepository.findAll();

            for (Member member : members) {
                try {
                    if (member.getMemberAppKey() != null && member.getMemberAppSecret() != null) {
                        log.info("회원 ID: {} 토큰 갱신 시작", member.getId());
                        KisTokenResponse tokenResponse = kisAuthService.saveMemberAccessToken(
                                member.getId(),
                                member.getMemberAppKey(),
                                member.getMemberAppSecret());
                        String newToken = tokenResponse.getAccessToken();

                        memberRepository.save(member);
                    }
                } catch (Exception e) {
                    log.error("회원 ID: {} 토큰 갱신 실패: {}", member.getId(), e.getMessage());
                }
            }
            log.info("KIS Token Refresh Scheduler 완료");
        } catch (KisException e) {
            log.error("KIS Token Refresh Scheduler 실행 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 매일 오전 2시(한국시간)에 관리자 AccessToken 갱신
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void refreshAdminAccessToken() {
        log.info("KIS Access-Token Scheduler 시작 (Asia/Seoul)");
        try {
            kisAuthService.saveAdminAccessToken(
                    kisAdminConfig.getAppkey(),
                    kisAdminConfig.getAppSecret());
            log.info("Admin AccessToken 갱신 완료");
        } catch (Exception e) {
            log.error("admin 토큰 발급 실패: {}", e.getMessage());
        }
    }

    /**
     * 6시간마다(한국시간 기준) ApprovalKey 갱신
     */
    @Scheduled(cron = "0 0 */6 * * *", zone = "Asia/Seoul")
    public void refreshAdminApprovalToken() {
        log.info("KIS ApprovalKey Refresh Scheduler 시작 (Asia/Seoul)");
        try {
            KisApprovalResponse response = kisAuthService.saveAdminApprovalToken(
                    kisAdminConfig.getAppkey(),
                    kisAdminConfig.getAppSecret());
            log.info("ApprovalKey 갱신 완료: {}", response.getApprovalKey());
        } catch (Exception e) {
            log.error("ApprovalKey 갱신 실패: {}", e.getMessage());
        }
    }
}
