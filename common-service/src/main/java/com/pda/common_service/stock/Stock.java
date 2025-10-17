package com.pda.common_service.stock;

import com.pda.common_service.BaseEntity;
import com.pda.common_service.stock.dto.StockInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {
    @Id
    String code;

    String imageUri;

    String name;

    public StockInfo toDto() {
        return new StockInfo(code, imageUri, name);
    }
}
