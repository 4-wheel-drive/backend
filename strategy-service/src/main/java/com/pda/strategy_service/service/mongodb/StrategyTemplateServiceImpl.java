package com.pda.strategy_service.service.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pda.common_service.exception.StrategyTemplatesException;
import com.pda.common_service.response.ResponseMessage;
import com.pda.strategy_service.domain.mongodb.StrategyTemplate;
import com.pda.strategy_service.repository.mongodb.StrategyTemplateRepository;
import com.pda.strategy_service.service.StrategySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StrategyTemplateServiceImpl implements StrategyTemplateService {
    private final StrategyTemplateRepository strategyTemplateRepository;
    private final StrategyTemplateFileLoader fileLoader;
    private final StrategySummaryService strategySummaryService;


    @Override
    @Transactional
    public StrategyTemplate saveStrategyTemplate(Long strategyMetaId, Map<String, Object> strategyJson) {
        try {
            StrategyTemplate strategyTemplate = StrategyTemplate.builder()
                    .strategyId(strategyMetaId)
                    .strategyName((String) strategyJson.get("strategy_name"))
                    .version((Integer) strategyJson.get("version"))
                    .ownerId((String) strategyJson.get("owner_id"))
                    .meta((Map<String, Object>) strategyJson.get("meta"))
                    .buy((Map<String, Object>) strategyJson.get("buy"))
                    .sell((Map<String, Object>) strategyJson.get("sell"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            StrategyTemplate savedStrategyTemplate = strategyTemplateRepository.save(strategyTemplate);

            String jsonString = new ObjectMapper().writeValueAsString(strategyJson);

            strategySummaryService.generateSummaryAndSave(strategyMetaId, jsonString);

            return savedStrategyTemplate;

        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_SAVE_FAILED);
        }
    }

    @Override
    public StrategyTemplate saveStrategyTemplate(Map<String, Object> strategyJson) {
        try {
            StrategyTemplate strategyTemplate = StrategyTemplate.builder()
                    .strategyName((String) strategyJson.get("strategy_name"))
                    .version((Integer) strategyJson.get("version"))
                    .ownerId((String) strategyJson.get("owner_id"))
                    .meta((Map<String, Object>) strategyJson.get("meta"))
                    .buy((Map<String, Object>) strategyJson.get("buy"))
                    .sell((Map<String, Object>) strategyJson.get("sell"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return strategyTemplateRepository.save(strategyTemplate);
        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_SAVE_FAILED);
        }
    }
    
    @Override
    public List<StrategyTemplate> getAllStrategyTemplates() {
        return strategyTemplateRepository.findAll();
    }
    
    @Override
    public List<StrategyTemplate> getStrategyTemplatesByOwner(String ownerId) {
        return strategyTemplateRepository.findByOwnerId(ownerId);
    }
    
    @Override
    public StrategyTemplate getStrategyTemplateByNameAndVersion(String strategyName, Integer version) {
        return strategyTemplateRepository.findByStrategyNameAndVersion(strategyName, version)
                .orElse(null);
    }
    
    @Override
    public List<StrategyTemplate> searchStrategyTemplatesByName(String strategyName) {
        return strategyTemplateRepository.findByStrategyNameContainingIgnoreCase(strategyName);
    }
    
    @Override
    public void deleteStrategyTemplate(String strategyName, Integer version) {
        strategyTemplateRepository.deleteByStrategyNameAndVersion(strategyName, version);
    }

    @Override
    public void initializeDefaultStrategyTemplates() {
        try {
            List<Map<String, Object>> templates = fileLoader.loadAllStrategyTemplates();
            if (templates.isEmpty()) {
                return;
            }

            for (Map<String, Object> template : templates) {
                try {
                    saveStrategyTemplate(template);
                } catch (Exception e) {
                    throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_INITIALIZE_FAILED);
                }
            }

        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_INITIALIZE_FAILED);
        }
    }

    @Override
    public void forceReinitializeStrategyTemplates() {
        try {
            strategyTemplateRepository.deleteAll();

            List<Map<String, Object>> templates = fileLoader.loadAllStrategyTemplates();

            if (templates.isEmpty()) {
                return;
            }

            for (Map<String, Object> template : templates) {
                try {
                    saveStrategyTemplate(template);
                } catch (Exception e) {
                    throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_FORCE_REINITIALIZE_FAILED);
                }
            }

        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_FORCE_REINITIALIZE_FAILED);
        }
    }

    @Override
    public void updateStrategyTemplates() {
        try {
            List<Map<String, Object>> fileTemplates = fileLoader.loadAllStrategyTemplates();

            if (fileTemplates.isEmpty()) {
                return;
            }

            int updatedCount = 0;
            int createdCount = 0;

            for (Map<String, Object> fileTemplate : fileTemplates) {
                try {
                    String strategyName = (String) fileTemplate.get("strategy_name");
                    Integer version = (Integer) fileTemplate.get("version");

                    Optional<StrategyTemplate> existingTemplate =
                        strategyTemplateRepository.findByStrategyNameAndVersion(strategyName, version);

                    if (existingTemplate.isPresent()) {
                        StrategyTemplate updatedTemplate = StrategyTemplate.builder()
                                .id(existingTemplate.get().getId())
                                .strategyName(strategyName)
                                .version(version)
                                .ownerId((String) fileTemplate.get("owner_id"))
                                .meta((Map<String, Object>) fileTemplate.get("meta"))
                                .buy((Map<String, Object>) fileTemplate.get("buy"))
                                .sell((Map<String, Object>) fileTemplate.get("sell"))
                                .createdAt(existingTemplate.get().getCreatedAt())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        strategyTemplateRepository.save(updatedTemplate);
                        updatedCount++;

                    } else {
                        saveStrategyTemplate(fileTemplate);
                        createdCount++;
                    }

                } catch (Exception e) {
                    throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_UPDATE_FAILED);
                }
            }

        } catch (Exception e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_UPDATE_FAILED);
        }
    }
}
