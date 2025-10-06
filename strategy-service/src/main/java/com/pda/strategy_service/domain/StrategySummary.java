package com.pda.strategy_service.domain;

import com.pda.common_service.BaseEntity;
import com.pda.strategy_service.domain.dto.StrategySummaryDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StrategySummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Strategy strategy;

    @Column(columnDefinition = "TEXT")
    private String summaryOverview;

    @Column(columnDefinition = "TEXT")
    private String summaryCondition;

    @Column(columnDefinition = "TEXT")
    private String summaryRisk;

    public StrategySummaryDto toDto() {
        return new StrategySummaryDto(summaryOverview, summaryCondition, summaryRisk);
    }
}
