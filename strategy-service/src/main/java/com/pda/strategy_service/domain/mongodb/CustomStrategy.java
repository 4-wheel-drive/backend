package com.pda.strategy_service.domain.mongodb;


import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "custom_strategy")
public class CustomStrategy {
    @Id
    private String id;

    @Field("strategy_name")
    private String strategyName;

    private Integer version;

    private Long strategyId;

    @Field("owner_id")
    private String ownerId;

    private Map<String, Object> meta;

    private Map<String, Object> buy;

    private Map<String, Object> sell;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
