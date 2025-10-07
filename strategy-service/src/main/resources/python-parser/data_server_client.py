from typing import Optional, Any
from datetime import datetime, timedelta

try:
    import redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False


class DataServerClient:
    
    def __init__(self, redis_host: str = "localhost", redis_port: int = 6379, redis_db: int = 0):
        if REDIS_AVAILABLE:
            self.redis_client = redis.Redis(
                host=redis_host,
                port=redis_port,
                db=redis_db,
                decode_responses=True
            )
        else:
            self.redis_client = None
        
        self._cached_data = {}
        self._cache_expiry = {}
        self._cache_duration = 60
    
    def get_price_data(self, symbol: str, field: str, timeframe: str, lookback: int = 0) -> Optional[float]:
        cache_key = f"price:{symbol}:{field}:{timeframe}:{lookback}"
        cached_value = self._get_cached_data(cache_key)
        if cached_value is not None:
            return cached_value
        
        if self.redis_client:
            try:
                key = f"candles:{symbol}:{timeframe}"
                if lookback == 0:
                    data = self.redis_client.lindex(key, -1)
                else:
                    data = self.redis_client.lindex(key, -(lookback + 1))
                
                if data:
                    import json
                    candle_data = json.loads(data)
                    value = candle_data.get(field)
                    if value is not None:
                        float_value = float(value)
                        self._set_cached_data(cache_key, float_value)
                        return float_value
            except Exception as e:
                print(f"Redis에서 가격 데이터 가져오기 실패: {e}")
        
        return None
    
    def get_indicator_value(self, symbol: str, indicator_name: str, timeframe: str, **args) -> Optional[float]:
        cache_key = f"indicator:{symbol}:{indicator_name}:{timeframe}"
        cached_value = self._get_cached_data(cache_key)
        if cached_value is not None:
            return cached_value
        
        if self.redis_client:
            try:
                individual_key = f"indicators:{symbol}:{timeframe}:{indicator_name}"
                value = self.redis_client.get(individual_key)
                
                if value is not None:
                    float_value = float(value)
                    self._set_cached_data(cache_key, float_value)
                    return float_value
                
                hash_key = f"indicators:{symbol}:{timeframe}"
                hash_data = self.redis_client.hget(hash_key, indicator_name)
                
                if hash_data is not None:
                    float_value = float(hash_data)
                    self._set_cached_data(cache_key, float_value)
                    return float_value
                
                if '.' in indicator_name:
                    base_name, subfield = indicator_name.split('.', 1)
                    object_key = f"indicators:{symbol}:{timeframe}:{base_name}"
                    object_data = self.redis_client.hget(object_key, subfield)
                    
                    if object_data is not None:
                        float_value = float(object_data)
                        self._set_cached_data(cache_key, float_value)
                        return float_value
                        
            except Exception as e:
                print(f"Redis에서 지표 데이터 가져오기 실패 ({indicator_name}): {e}")
        
        return None
    
    def _get_cached_data(self, key: str) -> Optional[Any]:
        if key in self._cached_data and self._cache_expiry[key] > datetime.now():
            return self._cached_data[key]
        return None
    
    def _set_cached_data(self, key: str, value: Any):
        self._cached_data[key] = value
        self._cache_expiry[key] = datetime.now() + timedelta(seconds=self._cache_duration)
    
    def clear_cache(self):
        self._cached_data.clear()
        self._cache_expiry.clear()
    
    def get_level_data(self, symbol: str, level_name: str, timeframe: str = "1d", lookback: int = 0) -> Optional[float]:
        try:
            if not REDIS_AVAILABLE or self.redis_client is None:
                return None
            
            key = f"levels:{symbol}:{timeframe}:{level_name}"
            value = self.redis_client.get(key)
            
            if value is not None:
                return float(value)
            
            hash_key = f"levels:{symbol}:{timeframe}"
            hash_data = self.redis_client.hget(hash_key, level_name)
            
            if hash_data is not None:
                return float(hash_data)
            
            return None
            
        except Exception as e:
            print(f"레벨 데이터 가져오기 실패 ({level_name}): {e}")
            return None
    
    def get_candle_pattern(self, symbol: str, pattern_names: list, timeframe: str = "1d") -> bool:
        try:
            if not REDIS_AVAILABLE or self.redis_client is None:
                return False
            
            key = f"patterns:{symbol}:{timeframe}"
            
            for pattern_name in pattern_names:
                pattern_key = f"{key}:{pattern_name}"
                pattern_detected = self.redis_client.get(pattern_key)
                
                if pattern_detected and pattern_detected.lower() == 'true':
                    return True
            
            hash_data = self.redis_client.hget(key, ":".join(pattern_names))
            if hash_data and hash_data.lower() == 'true':
                return True
            
            return False
            
        except Exception as e:
            print(f"캔들 패턴 감지 실패: {e}")
            return False
    
    def check_hold_condition(self, symbol: str, condition: dict, bars: int, timeframe: str = "1d") -> bool:
        try:
            if not REDIS_AVAILABLE or self.redis_client is None:
                return False
            
            key = f"hold:{symbol}:{timeframe}:{bars}"
            hold_result = self.redis_client.get(key)
            
            if hold_result is not None:
                return hold_result.lower() == 'true'
            
            hash_key = f"hold:{symbol}:{timeframe}"
            hash_data = self.redis_client.hget(hash_key, str(bars))
            
            if hash_data is not None:
                return hash_data.lower() == 'true'
            
            return False
            
        except Exception as e:
            print(f"HOLD 조건 확인 실패: {e}")
            return False
    
    def test_connection(self) -> bool:
        if not REDIS_AVAILABLE or self.redis_client is None:
            return False
            
        try:
            self.redis_client.ping()
            return True
        except Exception as e:
            print(f"Redis 연결 실패: {e}")
            return False
