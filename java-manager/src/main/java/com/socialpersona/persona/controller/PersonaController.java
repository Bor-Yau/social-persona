package com.socialpersona.persona.controller;

import com.socialpersona.error.SystemErrorHandler;
import com.socialpersona.persona.dto.PersonaConfigDTO;
import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.service.PersonaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private static final Logger log = LoggerFactory.getLogger(PersonaController.class);

    @Autowired
    private PersonaService personaService;

    @Autowired
    private SystemErrorHandler systemErrorHandler;

    @GetMapping
    public List<Persona> list() {
        return personaService.listAll();
    }

    @GetMapping("/{id}")
    public Persona detail(@PathVariable String id) {
        Persona persona = personaService.getById(id);
        if (persona == null) throw new RuntimeException("Persona 不存在: " + id);
        return persona;
    }

    @PutMapping("/{id}")
    public Map<String, String> update(@PathVariable String id, @RequestBody PersonaConfigDTO config) {
        personaService.update(id, config);
        return Map.of("status", "ok", "id", id);
    }

    @PostMapping("/{id}/archive")
    public Map<String, String> archive(@PathVariable String id) {
        systemErrorHandler.resetFailCount(id);
        personaService.archive(id);
        return Map.of("status", "ok", "id", id, "new_status", "archived");
    }

    @PostMapping("/{id}/toggle")
    public Map<String, String> toggle(@PathVariable String id) {
        String newStatus = personaService.toggle(id);
        return Map.of("status", "ok", "id", id, "new_status", newStatus);
    }

    @GetMapping("/{id}/export")
    public String exportConfig(@PathVariable String id) {
        String config = personaService.exportConfig(id);
        if (config == null) throw new RuntimeException("Persona 不存在: " + id);
        return config;
    }

    @PostMapping("/import")
    public Map<String, String> importConfig(@RequestBody String json) {
        String id = personaService.importFromJson(json);
        return Map.of("status", "ok", "id", id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody String json) {
        try {
            String id = personaService.createFromJson(json);
            return Map.of("status", "ok", "id", id);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/{id}/bind-channel")
    public Map<String, String> bindChannel(@PathVariable String id,
                                           @RequestBody Map<String, String> body) {
        String type = body.get("type");
        String account = body.get("account");
        personaService.bindChannel(id, type, account);
        return Map.of("status", "ok", "id", id, "channel", type);
    }
}
