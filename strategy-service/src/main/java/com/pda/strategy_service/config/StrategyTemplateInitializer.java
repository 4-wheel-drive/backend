package com.pda.strategy_service.config;

import com.pda.common_service.exception.StrategyTemplatesException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.service.mongodb.StrategyTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StrategyTemplateInitializer implements CommandLineRunner {
    
    private final StrategyTemplateService strategyTemplateService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            long existingCount = strategyTemplateService.getAllStrategyTemplates().size();
            
            if (existingCount == 0) {
                strategyTemplateService.initializeDefaultStrategyTemplates();
            } else {
                strategyTemplateService.updateStrategyTemplates();
            }
            
        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_INITIALIZE_FAILED);
        }
    }
}
