import json
from typing import Dict, Any, Callable, Optional
from data_server_client import DataServerClient


class RealTimeStrategyParser:
    
    def __init__(self):
        self.data_server_client = DataServerClient()
    
    def parse_strategy_to_function(self, strategy_json: Dict[str, Any]) -> Callable:
        strategy_name = strategy_json.get("strategyName", "unknown_strategy")
        universe = strategy_json.get("meta", {}).get("universe", [])
        buy_node = strategy_json.get("buy", {}).get("node") if strategy_json.get("buy") else None
        sell_node = strategy_json.get("sell", {}).get("node") if strategy_json.get("sell") else None
        
        def execute_strategy(symbol=None, base_timeframe='1d'):
            results = []
            target_symbols = [symbol] if symbol else universe
            
            for target_symbol in target_symbols:
                current_price = self._get_data(target_symbol, 'price', 'close', base_timeframe, 0)
                
                buy_signal = False
                if buy_node:
                    buy_signal = self._evaluate_conditions(buy_node, target_symbol)
                
                sell_signal = False
                if sell_node:
                    sell_signal = self._evaluate_conditions(sell_node, target_symbol)
                
                results.append({
                    'symbol': target_symbol,
                    'current_price': current_price,
                    'buy_signal': buy_signal,
                    'sell_signal': sell_signal
                })
            
            return results
        
        execute_strategy.strategy_name = strategy_name
        execute_strategy.universe = universe
        execute_strategy.has_buy = bool(buy_node)
        execute_strategy.has_sell = bool(sell_node)
        
        return execute_strategy
    
    def _evaluate_conditions(self, node: Dict[str, Any], symbol: str) -> bool:
        node_type = node.get("type")
        
        if node_type == "GROUP":
            logic = node.get("logic")
            children = node.get("children", [])
            
            if not children:
                return True
            
            results = [self._evaluate_conditions(child, symbol) for child in children]
            
            if logic == "ALL":
                return all(results)
            elif logic == "ANY":
                return any(results)
            else:
                return False
                
        elif node_type == "COMPARE":
            operator = node.get("operator")
            left_node = node.get("left")
            right_node = node.get("right")
            
            left_value = self._get_node_value(left_node, symbol)
            right_value = self._get_node_value(right_node, symbol)
            
            if left_value is None or right_value is None:
                return False
            
            if operator == ">":
                return left_value > right_value
            elif operator == "<":
                return left_value < right_value
            elif operator == ">=":
                return left_value >= right_value
            elif operator == "<=":
                return left_value <= right_value
            elif operator == "==":
                return left_value == right_value
            elif operator == "!=":
                return left_value != right_value
            else:
                return False
                
        elif node_type == "CROSS":
            direction = node.get("direction")
            left_node = node.get("left")
            right_node = node.get("right")
            
            current_left = self._get_node_value(left_node, symbol, 0)
            prev_left = self._get_node_value(left_node, symbol, 1)
            current_right = self._get_node_value(right_node, symbol, 0)
            prev_right = self._get_node_value(right_node, symbol, 1)
            
            if any(v is None for v in [current_left, prev_left, current_right, prev_right]):
                return False
                
            if direction == "UP":
                return (prev_left <= prev_right) and (current_left > current_right)
            elif direction == "DOWN":
                return (prev_left >= prev_right) and (current_left < current_right)
            else:
                return False
        
        elif node_type == "CANDLE_PATTERN":
            pattern_names = node.get("pattern_names", [])
            timeframe = node.get("timeframe", "1d")
            return self._detect_candle_pattern(symbol, pattern_names, timeframe)
        
        elif node_type == "HOLD":
            bars = node.get("bars", 1)
            child = node.get("child", {})
            timeframe = node.get("timeframe", "1d")
            
            if not child:
                return False
            
            return self._check_hold_condition(child, symbol, bars, timeframe)
        
        else:
            return False
    
    def _get_node_value(self, node: Dict[str, Any], symbol: str, lookback: int = 0):
        kind = node.get("kind")
        
        if kind == "PRICE":
            field = node.get("field")
            timeframe = node.get("timeframe", "1d")
            node_lookback = node.get("lookback", 0)
            total_lookback = lookback + node_lookback
            return self._get_data(symbol, 'price', field, timeframe, total_lookback)
        
        elif kind == "INDICATOR":
            name = node.get("name")
            args = node.get("args", {})
            timeframe = node.get("timeframe", "1d")
            subfield = node.get("subfield")
            node_lookback = node.get("lookback", 0)
            total_lookback = lookback + node_lookback
            
            indicator_name = self._build_indicator_name(name, args, subfield)
            return self._get_data(symbol, 'indicator', indicator_name, timeframe, total_lookback, **args)
        
        elif kind == "LEVEL":
            level_name = node.get("level_name")
            timeframe = node.get("timeframe", "1d")
            node_lookback = node.get("lookback", 0)
            total_lookback = lookback + node_lookback
            return self._get_data(symbol, 'level', level_name, timeframe, total_lookback)
        
        elif kind == "CONSTANT":
            value = node.get("constant", {}).get("value")
            unit = node.get("constant", {}).get("unit")
            if unit == "percent":
                return float(value) / 100.0 if isinstance(value, (int, float)) else float(value)
            return float(value) if isinstance(value, (int, float)) else value
        
        elif kind == "PROFIT_AND_LOSS":
            field = node.get("profit_and_loss_field")
            # Strategy-Service API로 손익 데이터 가져와야 함 --------------------------------------------
            return self._get_portfolio_pnl(symbol, field)
        
        elif kind == "EXPRESSION":
            return self._evaluate_expression(node.get("expression", {}), symbol, lookback)
        
        elif node.get("operator"):
            return self._evaluate_expression(node, symbol, lookback)
        
        else:
            return None
    
    def _evaluate_expression(self, expression_node: Dict[str, Any], symbol: str, lookback: int = 0):
        operator = expression_node.get("operator")
        left = expression_node.get("left")
        right = expression_node.get("right")
        
        left_value = self._get_node_value(left, symbol, lookback)
        right_value = self._get_node_value(right, symbol, lookback)
        
        if left_value is None or right_value is None:
            return None
        
        if operator == '+':
            return left_value + right_value
        elif operator == '-':
            return left_value - right_value
        elif operator == '*':
            return left_value * right_value
        elif operator == '/':
            if right_value == 0:
                return None
            return left_value / right_value
        else:
            return None
    
    def _get_data(self, symbol: str, data_type: str, field: str, timeframe: str, lookback: int, **params):
        try:
            if data_type == 'price':
                return self.data_server_client.get_price_data(symbol, field, timeframe, lookback)
            elif data_type == 'indicator':
                return self.data_server_client.get_indicator_value(symbol, field, timeframe, **params)
            elif data_type == 'level':
                return self.data_server_client.get_level_data(symbol, field, timeframe, lookback)
            elif data_type == 'pattern':
                pattern_names = params.get('pattern_names', [field])
                return self.data_server_client.get_candle_pattern(symbol, pattern_names, timeframe)
            elif data_type == 'hold':
                condition = params.get('condition', {})
                bars = params.get('bars', 1)
                return self.data_server_client.check_hold_condition(symbol, condition, bars, timeframe)
            else:
                return None
        except Exception as e:
            print(f"데이터 가져오기 실패 ({symbol}, {data_type}, {field}): {e}")
            return None
    
    def _build_indicator_name(self, name: str, args: dict, subfield: str = None) -> str:
        if name == 'SMA':
            period = args.get('period', 20)
            return f"sma{period}"
        elif name == 'EMA':
            period = args.get('period', 21)
            return f"ema{period}"
        elif name == 'RSI':
            period = args.get('period', 14)
            return f"rsi{period}"
        elif name == 'BOLLINGER_BANDS':
            if subfield:
                if subfield == 'middle':
                    subfield = 'ma'
                return f"bollinger.{subfield}"
            return "bollinger"
        elif name == 'MACD':
            if subfield:
                return f"macd.{subfield}"
            return "macd"
        elif name == 'ATR':
            period = args.get('period', 14)
            return f"atr{period}"
        elif name == 'RELATIVE_VOLUME':
            return "rvol"
        elif name == 'STOCHASTIC':
            if subfield:
                return f"stochastic.{subfield}"
            return "stochastic"
        elif name == 'VWAP':
            return "vwap"
        else:
            return name.lower()
    
    def _get_portfolio_pnl(self, symbol: str, field: str):
        try:
            import requests
            
            # Strategy-Service API 호출 (실제 구현 시)
            # response = requests.get(f"http://localhost:8082/api/strategies?symbol={symbol}")
            # if response.status_code == 200:
            #     data = response.json()
            #     return data.get(field)
            
            # 현재는 Mock 데이터 반환중임, 나중에 제거해야 됨
            mock_pnl_data = {
                "total_profit": 150000.0,
                "profit_rate": 0.15,
                "avg_buy_price": 100000.0,
                "current_price": 115000.0
            }
            return mock_pnl_data.get(field)
            
        except Exception as e:
            print(f"손익 데이터 가져오기 실패: {e}")
            return None
    
    def _detect_candle_pattern(self, symbol: str, pattern_names: list, timeframe: str):
        try:
            return self.data_server_client.get_candle_pattern(symbol, pattern_names, timeframe)
        except Exception as e:
            print(f"캔들 패턴 감지 실패: {e}")
            return False
    
    def _check_hold_condition(self, child_condition: Dict[str, Any], symbol: str, bars: int, timeframe: str):
        try:
            return self.data_server_client.check_hold_condition(symbol, child_condition, bars, timeframe)
        except Exception as e:
            print(f"HOLD 조건 확인 실패: {e}")
            return self._evaluate_conditions(child_condition, symbol)

if __name__ == "__main__":
    parser = RealTimeStrategyParser()
    
    from mongodb_client import MongoDBStrategyClient
    mongo_client = MongoDBStrategyClient()
    strategy_json = mongo_client.get_strategy_by_name_and_version("bollinger_band_breakout_buy", 1)
    
    if strategy_json:
        execute_strategy = parser.parse_strategy_to_function(strategy_json)
        results = execute_strategy(symbol="005930")
        print(f"전략 실행 결과: {results}")
    else:
        print("전략을 가져올 수 없습니다.")