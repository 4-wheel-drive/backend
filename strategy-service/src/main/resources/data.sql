-- ===============================
-- Stock (주식)
-- ===============================
INSERT INTO stock (code, image_uri, name)
VALUES ('005930', 'https://logo.example.com/samsung.png', '삼성전자');

INSERT INTO stock (code, image_uri, name)
VALUES ('000660', 'https://logo.example.com/skhynix.png', 'SK하이닉스');

-- ===============================
-- Member (회원)
-- BaseEntity(created_at, updated_at) NOT NULL 이므로 값 넣어줌
-- ===============================
INSERT INTO member (
    id, member_id, member_password, member_name, member_account_number,
    member_app_key, member_app_secret, created_at, updated_at
)
VALUES (
    1, 'user1', 'pass1', '홍길동', '123-456-7890',
    'appkey-111', 'appsecret-111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO member (
    id, member_id, member_password, member_name, member_account_number,
    member_app_key, member_app_secret, created_at, updated_at
)
VALUES (
    2, 'user2', 'pass2', '이순신', '987-654-3210',
    'appkey-222', 'appsecret-222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- ===============================
-- MemberStock (회원 보유 주식)
-- ===============================
INSERT INTO member_stock (id, stock_code, member_id)
VALUES (1, '005930', 1);

INSERT INTO member_stock (id, stock_code, member_id)
VALUES (2, '000660', 2);

-- ===============================
-- StrategyProfitSummary (전략 요약)
-- ===============================
INSERT INTO strategy_profit_summary (id, avg_buy_price, current_price, profit_rate)
VALUES (1, 100000, 120000, 0.15);   -- 삼성전자 단기 매매 전략

INSERT INTO strategy_profit_summary (id, avg_buy_price, current_price, profit_rate)
VALUES (2, 104000, 130000, 0.25);   -- 삼성전자 장기 보유 전략

INSERT INTO strategy_profit_summary (id, avg_buy_price, current_price, profit_rate)
VALUES (3, 100000, 107000, 0.07);   -- 삼성전자 단타 전략

INSERT INTO strategy_profit_summary (id, avg_buy_price, current_price, profit_rate)
VALUES (4, 100000, 95000, -0.05);   -- 하이닉스 장기 투자 전략

INSERT INTO strategy_profit_summary (id, avg_buy_price, current_price, profit_rate)
VALUES (5, 110000, 125000, 0.13);   -- 하이닉스 단기 매매 전략

-- ===============================
-- Strategy (전략)
-- ===============================
INSERT INTO strategy (id, member_stock_id, strategy_profit_summary_id, strategy_name, strategy_is_activated, strategy_is_deleted)
VALUES (1, 1, 1, '삼성전자 단기 매매 전략', 'ACTIVATED', 'EXISTED');

INSERT INTO strategy (id, member_stock_id, strategy_profit_summary_id, strategy_name, strategy_is_activated, strategy_is_deleted)
VALUES (2, 1, 2, '삼성전자 장기 보유 전략', 'PENDING', 'EXISTED');

INSERT INTO strategy (id, member_stock_id, strategy_profit_summary_id, strategy_name, strategy_is_activated, strategy_is_deleted)
VALUES (3, 1, 3, '삼성전자 단타 전략', 'ACTIVATED', 'EXISTED');

INSERT INTO strategy (id, member_stock_id, strategy_profit_summary_id, strategy_name, strategy_is_activated, strategy_is_deleted)
VALUES (4, 2, 4, '하이닉스 장기 투자 전략', 'PENDING', 'EXISTED');

INSERT INTO strategy (id, member_stock_id, strategy_profit_summary_id, strategy_name, strategy_is_activated, strategy_is_deleted)
VALUES (5, 2, 5, '하이닉스 단기 매매 전략', 'ACTIVATED', 'EXISTED');
