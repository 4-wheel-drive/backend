package com.pda.strategy_service.controller;

import com.pda.strategy_service.domain.mongodb.StrategyTemplate;
import com.pda.strategy_service.service.mongodb.StrategyTemplateService;
import com.pda.strategy_service.service.mongodb.StrategyTemplateFileLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strategy-templates")
@RequiredArgsConstructor
public class StrategyTemplateController {
    
    private final StrategyTemplateService strategyTemplateService;
    private final StrategyTemplateFileLoader fileLoader;
    
    @GetMapping
    public ResponseEntity<List<StrategyTemplate>> getAllStrategyTemplates() {
        List<StrategyTemplate> templates = strategyTemplateService.getAllStrategyTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<StrategyTemplate>> getStrategyTemplatesByOwner(@PathVariable String ownerId) {
        List<StrategyTemplate> templates = strategyTemplateService.getStrategyTemplatesByOwner(ownerId);
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<StrategyTemplate>> searchStrategyTemplates(@RequestParam String name) {
        List<StrategyTemplate> templates = strategyTemplateService.searchStrategyTemplatesByName(name);
        return ResponseEntity.ok(templates);
    }
    
    @PostMapping("/load-from-file/{strategyName}")
    public ResponseEntity<StrategyTemplate> loadStrategyTemplateFromFile(@PathVariable String strategyName) {
        Map<String, Object> templateData = fileLoader.loadStrategyTemplateByName(strategyName);
        if (templateData != null) {
            StrategyTemplate template = strategyTemplateService.saveStrategyTemplate(templateData);
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{strategyName}/{version}")
    public ResponseEntity<StrategyTemplate> getStrategyTemplate(
            @PathVariable String strategyName, 
            @PathVariable Integer version) {
        StrategyTemplate template = strategyTemplateService.getStrategyTemplateByNameAndVersion(strategyName, version);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<StrategyTemplate> saveStrategyTemplate(@RequestBody Map<String, Object> strategyJson) {
        StrategyTemplate template = strategyTemplateService.saveStrategyTemplate(strategyJson);
        return ResponseEntity.ok(template);
    }
    
    @DeleteMapping("/{strategyName}/{version}")
    public ResponseEntity<Void> deleteStrategyTemplate(
            @PathVariable String strategyName, 
            @PathVariable Integer version) {
        strategyTemplateService.deleteStrategyTemplate(strategyName, version);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/available-files")
    public ResponseEntity<List<String>> getAvailableStrategyTemplateFiles() {
        List<String> files = fileLoader.getAvailableStrategyTemplates();
        return ResponseEntity.ok(files);
    }
    
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeStrategyTemplates() {
        try {
            strategyTemplateService.initializeDefaultStrategyTemplates();
            return ResponseEntity.ok("Strategy templates initialized successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to initialize strategy templates: " + e.getMessage());
        }
    }
    
    @PostMapping("/force-reinitialize")
    public ResponseEntity<String> forceReinitializeStrategyTemplates() {
        try {
            strategyTemplateService.forceReinitializeStrategyTemplates();
            return ResponseEntity.ok("Strategy templates force reinitialized successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to force reinitialize strategy templates: " + e.getMessage());
        }
    }
    
    @PostMapping("/update")
    public ResponseEntity<String> updateStrategyTemplates() {
        try {
            strategyTemplateService.updateStrategyTemplates();
            return ResponseEntity.ok("Strategy templates updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update strategy templates: " + e.getMessage());
        }
    }
}
