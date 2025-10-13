package com.pda.trading_service.websocket;

import com.pda.trading_service.service.TradeExecutionListener;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketClient {
    private final TradeExecutionListener executionListener;
    private static final String TR_ID = "H0STCNI9";

    public void connect(String approvalKey, String orderNo) {
        try {
            URI uri = URI.create("ws://ops.koreainvestment.com:31000"); // /websocket 제거

            StandardWebSocketClient client = new StandardWebSocketClient();

            CompletableFuture<WebSocketSession> sessionFuture = client.execute(
                    new WebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) {

                            String subscribeMessage = """
                                {
                                  "header": {
                                    "approval_key": "%s",
                                    "custtype": "P",
                                    "tr_type": "1",
                                    "content_type": "utf-8"
                                  },
                                  "body": {
                                    "input": {
                                      "tr_id": "%s",
                                      "tr_key": "%s"
                                    }
                                  }
                                }
                            """.formatted(approvalKey, TR_ID, orderNo);

                            try {
                                session.sendMessage(new TextMessage(subscribeMessage));
                            } catch (Exception e) {
                                log.error("[❌ Subscription failed]", e);
                            }
                        }

                        @Override
                        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                            String payload = message.getPayload().toString();
                            log.info("[💬 Message received] {}", payload);

                            boolean isFilled = executionListener.onExecutionMessage(orderNo, payload);

                            if (isFilled) {
                                try {
                                    log.info("[✅ 체결 완료] → WebSocket 닫기 (orderNo={})", orderNo);
                                    session.close(CloseStatus.NORMAL);
                                } catch (Exception e) {
                                    log.error("[⚠️ 세션 종료 실패]", e);
                                }
                            }
                        }

                        @Override
                        public void handleTransportError(WebSocketSession session, Throwable exception) {
                            log.error("[🚨 Transport error]", exception);
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                            log.info("[🔌 Connection closed] {}", closeStatus);
                        }

                        @Override
                        public boolean supportsPartialMessages() {
                            return false;
                        }
                    },
                    null,
                    uri
            );

            sessionFuture.join();

        } catch (Exception e) {
            log.error("❌ WebSocket connection failed", e);
        }
    }
}
