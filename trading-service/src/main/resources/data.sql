-- ===============================
-- 🌱 3. 목데이터 삽입 (with created_at, updated_at)
-- ===============================

-- 🧩 Stock (주식)
INSERT INTO stock (code, image_uri, name, created_at, updated_at)
VALUES
('005930', 'https://logo.example.com/samsung.png', '삼성전자', NOW(), NOW()),
('000660', 'https://logo.example.com/skhynix.png', 'SK하이닉스', NOW(), NOW()),
('035720', 'https://logo.example.com/kakao.png', '카카오', NOW(), NOW()),
('051910', 'https://logo.example.com/lgchem.png', 'LG화학', NOW(), NOW()),
('035420', 'https://logo.example.com/naver.png', '네이버', NOW(), NOW());

-- 👤 Member (회원)
INSERT INTO member (member_id, member_password, member_name, member_account_number, member_app_key, member_app_secret, created_at, updated_at)
VALUES
('user1', 'pass1', '홍길동', '123-456-789', 'key1', 'secret1', NOW(), NOW()),
('user2', 'pass2', '이순신', '987-654-321', 'key2', 'secret2', NOW(), NOW());

-- 📈 MemberStock (회원-보유 종목)
INSERT INTO member_stock (stock_id, member_id, created_at, updated_at)
VALUES
(1, 1, NOW(), NOW()), -- 홍길동 - 삼성전자
(2, 1, NOW(), NOW()), -- 홍길동 - 하이닉스
(3, 2, NOW(), NOW()), -- 이순신 - 카카오
(4, 2, NOW(), NOW()), -- 이순신 - LG화학
(5, 1, NOW(), NOW()); -- 홍길동 - 네이버

-- 💰 StrategyProfitSummary (전략 수익 요약)
INSERT INTO strategy_profit_summary (strategy_profit_summary_avg_buy_price, strategy_profit_summary_current_price, strategy_profit_summary_profit_rate, created_at, updated_at)
VALUES
(100000.00, 120000.00, 20.00, NOW(), NOW()),
(80000.00, 88000.00, 10.00, NOW(), NOW()),
(130000.00, 117000.00, -10.00, NOW(), NOW()),
(62000.00, 68200.00, 10.00, NOW(), NOW()),
(180000.00, 198000.00, 10.00, NOW(), NOW()),
(54000.00, 48600.00, -10.00, NOW(), NOW());

-- 📊 Strategy (전략)
INSERT INTO strategy (member_stock_id, strategy_profit_summary_id, strategy_name, strategy_activated_status, strategy_existed_status, created_at, updated_at)
VALUES
(1, 1, '삼성전자 단기 매매 전략', 'ACTIVATED', 'EXISTED', NOW(), NOW()),
(1, 2, '삼성전자 장기 보유 전략', 'PENDING', 'EXISTED', NOW(), NOW()),
(2, 3, '하이닉스 모멘텀 전략', 'ACTIVATED', 'EXISTED', NOW(), NOW()),
(3, 4, '카카오 반등 매수 전략', 'PENDING', 'EXISTED', NOW(), NOW()),
(4, 5, 'LG화학 스윙 전략', 'ACTIVATED', 'EXISTED', NOW(), NOW()),
(5, 6, '네이버 눌림목 매수 전략', 'PENDING', 'EXISTED', NOW(), NOW());

-- 💸 StockOrder (주문)
INSERT INTO stock_order (strategy_id, order_type, order_quantity, order_price, order_kind, order_status, trade_id, created_at, updated_at)
VALUES
-- 삼성전자 전략들
(1, 'BUY', 10, 118000.00, 'LIMIT', 'FILLED', 'T20231005001', NOW(), NOW()),
(1, 'SELL', 5, 121000.00, 'LIMIT', 'PARTIALLY_FILLED', 'T20231005002', NOW(), NOW()),
(2, 'BUY', 20, 95000.00, 'MARKET', 'FILLED', 'T20231005003', NOW(), NOW()),

-- 하이닉스
(3, 'BUY', 15, 88000.00, 'MARKET', 'FILLED', 'T20231005004', NOW(), NOW()),
(3, 'SELL', 10, 91000.00, 'LIMIT', 'PARTIALLY_FILLED', 'T20231005005', NOW(), NOW()),

-- 카카오
(4, 'BUY', 30, 62000.00, 'LIMIT', 'FILLED', 'T20231005006', NOW(), NOW()),

-- LG화학
(5, 'BUY', 8, 180000.00, 'MARKET', 'FILLED', 'T20231005007', NOW(), NOW()),
(5, 'SELL', 3, 198000.00, 'LIMIT', 'FILLED', 'T20231005008', NOW(), NOW()),

-- 네이버
(6, 'BUY', 10, 54000.00, 'MARKET', 'FILLED', 'T20231005009', NOW(), NOW()),
(6, 'SELL', 5, 48600.00, 'LIMIT', 'CANCELLED', 'T20231005010', NOW(), NOW());

-- 🧾 TradeExecution (체결 내역)
INSERT INTO trade_execution (stock_order_id, trade_execution_type, trade_execution_quantity, trade_execution_price, trade_execution_status, execution_time, created_at, updated_at)
VALUES
-- 삼성전자 단기 매매
(1, 'BUY', 10, 118000.00, 'FILLED', NOW(), NOW(), NOW()),
(2, 'SELL', 3, 121000.00, 'PARTIALLY_FILLED', NOW(), NOW(), NOW()),

-- 삼성전자 장기 보유
(3, 'BUY', 20, 95000.00, 'FILLED', NOW(), NOW(), NOW()),

-- 하이닉스
(4, 'BUY', 15, 88000.00, 'FILLED', NOW(), NOW(), NOW()),
(5, 'SELL', 5, 91000.00, 'PARTIALLY_FILLED', NOW(), NOW(), NOW()),

-- 카카오
(6, 'BUY', 30, 62000.00, 'FILLED', NOW(), NOW(), NOW()),

-- LG화학
(7, 'BUY', 8, 180000.00, 'FILLED', NOW(), NOW(), NOW()),
(8, 'SELL', 3, 198000.00, 'FILLED', NOW(), NOW(), NOW()),

-- 네이버
(9, 'BUY', 10, 54000.00, 'FILLED', NOW(), NOW(), NOW()),
(10, 'SELL', 5, 48600.00, 'CANCELLED', NOW(), NOW(), NOW());

-- 📆 DailyStrategyProfit (일별 수익률)
INSERT INTO daily_strategy_profit (strategy_id, daily_profit_rate, created_at, updated_at)
VALUES
(1, 0.0123, NOW(), NOW()),
(1, 0.0150, NOW(), NOW()),
(2, -0.0050, NOW(), NOW()),
(3, 0.0221, NOW(), NOW()),
(4, 0.0300, NOW(), NOW()),
(5, -0.0100, NOW(), NOW()),
(6, 0.0080, NOW(), NOW());
