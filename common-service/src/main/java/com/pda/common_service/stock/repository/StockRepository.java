package com.pda.common_service.stock.repository;

import com.pda.common_service.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}
