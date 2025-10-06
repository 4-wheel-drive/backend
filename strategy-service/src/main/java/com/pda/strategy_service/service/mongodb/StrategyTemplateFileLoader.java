package com.pda.strategy_service.service.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pda.common_service.exception.StrategyTemplatesException;
import com.pda.common_service.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StrategyTemplateFileLoader {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> loadAllStrategyTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:strategy-templates/*.json");

            for (Resource resource : resources) {
                try {
                    Map<String, Object> template = loadStrategyTemplateFromFile(resource);
                    if (template != null) {
                        templates.add(template);
                    }
                } catch (Exception e) {
                    throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_FILE_READ_FAILED);
                }
            }

        } catch (IOException e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_FILE_READ_FAILED);
        }

        return templates;
    }

    public Map<String, Object> loadStrategyTemplateFromFile(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> template = objectMapper.readValue(inputStream, Map.class);

            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(".json") && !template.containsKey("strategy_name")) {
                String strategyName = filename.substring(0, filename.lastIndexOf(".json"));
                template.put("strategy_name", strategyName);
            }

            return template;
        } catch (IOException e) {
            return null;
        }
    }

    public Map<String, Object> loadStrategyTemplateByName(String strategyName) {
        try {
            String filePath = "classpath:strategy-templates/" + strategyName + ".json";
            Resource resource = resourceLoader.getResource(filePath);

            if (!resource.exists()) {
                return null;
            }

            return loadStrategyTemplateFromFile(resource);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getAvailableStrategyTemplates() {
        List<String> templates = new ArrayList<>();

        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:strategy-templates/*.json");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".json")) {
                    templates.add(filename.substring(0, filename.lastIndexOf(".json")));
                }
            }

        } catch (IOException e) {
            throw new StrategyTemplatesException(ResponseMessage.STRATEGY_TEMPLATE_FILE_READ_FAILED);
        }

        return templates;
    }
}
