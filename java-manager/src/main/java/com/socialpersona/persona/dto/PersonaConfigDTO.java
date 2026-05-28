package com.socialpersona.persona.dto;

/**
 * 人格配置 DTO —— PersonaService 创建/更新时的传参对象
 *
 * ★ 为什么用 DTO 而非直接传 Persona 实体：
 *   实体与数据库字段一一映射，负责持久化。
 *   DTO 面向业务层——过滤掉不该由调用方传入的字段（如 id、apiKeyEncrypted、status、createdAt）。
 *
 * ★ 与 Python 的 PersonaConfig 的关系：
 *   这个是 Java 业务层的传参 DTO。传给 Python 的 PersonaConfig 对象
 *   需要在调用前组装更多运行时数据（如 relationshipState）。
 */
public class PersonaConfigDTO {

    private String name;
    private String bigFiveJson;
    private Double attachmentAnxiety;
    private Double attachmentAvoidance;
    private Double selfEsteemStability;
    private String socialRhythm;
    private String conflictStyle;
    private Double initiativeTendency;
    private String inputMethod;
    private String typingStyleJson;
    private Double typingSpeed;
    private String imageStylePrompt;
    private String characterAppearance;
    private Integer imageEnabled;
    private String sampleChatsJson;
    private String characterInitialWorldTime;
    private String birthday;
    private String characterCurrentContext;
    private String lifeStage;
    private String lifeStageDetail;
    private String currentLocation;
    private String aiQq;
    private String ownerQq;
    private String relationshipPhase;
    private String matchmakerRawData;

    // ==================== Getter / Setter ====================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBigFiveJson() { return bigFiveJson; }
    public void setBigFiveJson(String bigFiveJson) { this.bigFiveJson = bigFiveJson; }

    public Double getAttachmentAnxiety() { return attachmentAnxiety; }
    public void setAttachmentAnxiety(Double attachmentAnxiety) { this.attachmentAnxiety = attachmentAnxiety; }

    public Double getAttachmentAvoidance() { return attachmentAvoidance; }
    public void setAttachmentAvoidance(Double attachmentAvoidance) { this.attachmentAvoidance = attachmentAvoidance; }

    public Double getSelfEsteemStability() { return selfEsteemStability; }
    public void setSelfEsteemStability(Double selfEsteemStability) { this.selfEsteemStability = selfEsteemStability; }

    public String getSocialRhythm() { return socialRhythm; }
    public void setSocialRhythm(String socialRhythm) { this.socialRhythm = socialRhythm; }

    public String getConflictStyle() { return conflictStyle; }
    public void setConflictStyle(String conflictStyle) { this.conflictStyle = conflictStyle; }

    public Double getInitiativeTendency() { return initiativeTendency; }
    public void setInitiativeTendency(Double initiativeTendency) { this.initiativeTendency = initiativeTendency; }

    public String getInputMethod() { return inputMethod; }
    public void setInputMethod(String inputMethod) { this.inputMethod = inputMethod; }

    public String getTypingStyleJson() { return typingStyleJson; }
    public void setTypingStyleJson(String typingStyleJson) { this.typingStyleJson = typingStyleJson; }

    public Double getTypingSpeed() { return typingSpeed; }
    public void setTypingSpeed(Double typingSpeed) { this.typingSpeed = typingSpeed; }

    public String getImageStylePrompt() { return imageStylePrompt; }
    public void setImageStylePrompt(String imageStylePrompt) { this.imageStylePrompt = imageStylePrompt; }

    public String getCharacterAppearance() { return characterAppearance; }
    public void setCharacterAppearance(String characterAppearance) { this.characterAppearance = characterAppearance; }

    public Integer getImageEnabled() { return imageEnabled; }
    public void setImageEnabled(Integer imageEnabled) { this.imageEnabled = imageEnabled; }

    public String getSampleChatsJson() { return sampleChatsJson; }
    public void setSampleChatsJson(String sampleChatsJson) { this.sampleChatsJson = sampleChatsJson; }

    public String getCharacterInitialWorldTime() { return characterInitialWorldTime; }
    public void setCharacterInitialWorldTime(String characterInitialWorldTime) { this.characterInitialWorldTime = characterInitialWorldTime; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getCharacterCurrentContext() { return characterCurrentContext; }
    public void setCharacterCurrentContext(String characterCurrentContext) { this.characterCurrentContext = characterCurrentContext; }

    public String getLifeStage() { return lifeStage; }
    public void setLifeStage(String lifeStage) { this.lifeStage = lifeStage; }

    public String getLifeStageDetail() { return lifeStageDetail; }
    public void setLifeStageDetail(String lifeStageDetail) { this.lifeStageDetail = lifeStageDetail; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getAiQq() { return aiQq; }
    public void setAiQq(String aiQq) { this.aiQq = aiQq; }

    public String getOwnerQq() { return ownerQq; }
    public void setOwnerQq(String ownerQq) { this.ownerQq = ownerQq; }

    public String getRelationshipPhase() { return relationshipPhase; }
    public void setRelationshipPhase(String relationshipPhase) { this.relationshipPhase = relationshipPhase; }

    public String getMatchmakerRawData() { return matchmakerRawData; }
    public void setMatchmakerRawData(String matchmakerRawData) { this.matchmakerRawData = matchmakerRawData; }
}
