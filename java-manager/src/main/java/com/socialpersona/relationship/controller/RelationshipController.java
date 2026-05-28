package com.socialpersona.relationship.controller;

import com.socialpersona.relationship.engine.RelationshipEngine;
import com.socialpersona.relationship.entity.RelationshipState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关系状态 REST API
 */
@RestController
public class RelationshipController {

    @Autowired
    private RelationshipEngine relationshipEngine;

    @GetMapping("/api/relationships/{personaId}")
    public RelationshipState getRelationState(@PathVariable String personaId) {
        return relationshipEngine.getState(personaId);
    }
}
