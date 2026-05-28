package com.socialpersona.persona.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("personas")
public class Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

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
    private String apiKeyEncrypted;
    private String sampleChatsJson;
    private String characterInitialWorldTime;
    private String birthday;
    private String characterCurrentContext;
    /** 生命阶段简标：student | working | traveling | at_home | job_hunting */
    private String lifeStage;
    /** 生命阶段详细描述：如 "大二计算机系学生" */
    private String lifeStageDetail;
    /** 当前所在地：如 "杭州" */
    private String currentLocation;
    private String aiQq;
    private String ownerQq;
    private String relationshipPhase;
    private String status;
    private String lastUserMessageTime;
    private String matchmakerRawData;
    private String createdAt;
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLastUserMessageTime() { return lastUserMessageTime; }
    public void setLastUserMessageTime(String lastUserMessageTime) { this.lastUserMessageTime = lastUserMessageTime; }

    public String getMatchmakerRawData() { return matchmakerRawData; }
    public void setMatchmakerRawData(String matchmakerRawData) { this.matchmakerRawData = matchmakerRawData; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
