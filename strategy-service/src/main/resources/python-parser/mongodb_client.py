import requests
import json
from typing import Dict, Any, Optional, List
from datetime import datetime


class MongoDBStrategyClient:
    def __init__(self, base_url: str = "http://localhost:8082"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.timeout = 10
    
    def get_all_strategies(self) -> Optional[List[Dict[str, Any]]]:
        try:
            response = self.session.get(f"{self.base_url}/api/strategy-templates")
            
            if response.status_code == 200:
                strategies = response.json()
                return strategies
            else:
                return None
                
        except requests.exceptions.ConnectionError:
            return None
        except Exception as e:
            return None
    
    def get_strategy_by_name_and_version(self, strategy_name: str, version: int) -> Optional[Dict[str, Any]]:
        strategies = self.get_all_strategies()
        
        if not strategies:
            return None
        
        for strategy in strategies:
            if (strategy.get('strategyName') == strategy_name and 
                strategy.get('version') == version):
                return strategy
        
        return None
    
    def get_strategy_by_id(self, _id: str) -> Optional[Dict[str, Any]]:
        try:
            response = self.session.get(f"{self.base_url}/api/strategy-templates/{_id}")
            
            if response.status_code == 200:
                strategy = response.json()
                return strategy
            else:
                return None
                
        except Exception as e:
            print(f"ID로 전략 조회 실패: {e}")
            return None
    
    def get_strategy_for_execution(self, owner_id: str, strategy_name: str, 
                                  version: int, universe: str) -> Optional[Dict[str, Any]]:
        try:
            response = self.session.get(
                f"{self.base_url}/api/strategy-templates/execution",
                params={
                    "ownerId": owner_id,
                    "strategyName": strategy_name,
                    "version": version,
                    "universe": universe
                }
            )
            
            if response.status_code == 200:
                execution_data = response.json()
                return execution_data
            elif response.status_code == 404:
                return None
            else:
                return None
                
        except Exception as e:
            return None
    
    def get_strategies_by_owner(self, owner_id: str) -> Optional[List[Dict[str, Any]]]:
        strategies = self.get_all_strategies()
        
        if not strategies:
            return None
        
        owner_strategies = [
            strategy for strategy in strategies 
            if strategy.get('ownerId') == owner_id
        ]
        
        return owner_strategies
    
    def get_active_strategies(self) -> Optional[List[Dict[str, Any]]]:
        strategies = self.get_all_strategies()
        
        if not strategies:
            return None
        
        active_strategies = [
            strategy for strategy in strategies 
            if strategy.get('meta', {}).get('enabled', False)
        ]
        
        return active_strategies