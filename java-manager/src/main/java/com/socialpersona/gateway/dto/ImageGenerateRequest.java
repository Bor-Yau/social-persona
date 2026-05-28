package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageGenerateRequest {

    @JsonProperty("image_config")
    private Object imageConfig;

    private String prompt;

    @JsonProperty("persona_id")
    private String personaId;

    @JsonProperty("persona_dir_name")
    private String personaDirName;

    private String context;

    public Object getImageConfig() { return imageConfig; }
    public void setImageConfig(Object imageConfig) { this.imageConfig = imageConfig; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }
    public String getPersonaDirName() { return personaDirName; }
    public void setPersonaDirName(String personaDirName) { this.personaDirName = personaDirName; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
