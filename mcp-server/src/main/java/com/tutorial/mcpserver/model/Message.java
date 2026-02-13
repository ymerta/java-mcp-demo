package com.tutorial.mcpserver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Netmera benzeri mesaj/kampanya modeli.
 *
 * Gercek sistemdeki message collection'indan sadelestirilerek alinmistir.
 * Orjinal yaklasik 50+ alana sahip, burada en onemli ~15 alan kullanildi.
 *
 * msgType degerleri: PUSH, EMAIL, SMS, WEB_PUSH, IN_APP
 * sendStatus degerleri: DRAFT, FINISHED, STOPPED, DELETED
 * msgMethod degerleri: CAMPAIGN, TRANSACTIONAL
 */
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    private String msgType;          // PUSH, EMAIL, SMS, WEB_PUSH, IN_APP
    private String msgMethod;        // CAMPAIGN, TRANSACTIONAL
    private String title;
    private String message;
    private String sendStatus;       // DRAFT, FINISHED, STOPPED, DELETED
    private List<String> platforms;   // ANDROID, IOS, CHROME, FIREFOX
    private String creator;
    private String campaignStyle;    // TEXT, MEDIA
    private Date createDate;
    private Date updateDate;
    private int targetAudience;      // Hedef kitle sayisi
    private int targetUser;          // Ulasilan kullanici sayisi
    private Schedule schedule;       // Zamanlama bilgisi

    /**
     * Embedded document: Mesajin zamanlama bilgisi.
     * MongoDB'de ic ice (nested) document olarak saklanir.
     */
    public static class Schedule {
        private String type;         // NOW, SCHEDULED
        private Date scheduledDate;  // SCHEDULED ise tarih

        public Schedule() {}

        public Schedule(String type, Date scheduledDate) {
            this.type = type;
            this.scheduledDate = scheduledDate;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Date getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(Date scheduledDate) { this.scheduledDate = scheduledDate; }
    }

    public Message() {}

    public Message(String msgType, String msgMethod, String title, String message,
                   String sendStatus, List<String> platforms, String creator,
                   String campaignStyle, int targetAudience, int targetUser, Schedule schedule) {
        this.msgType = msgType;
        this.msgMethod = msgMethod;
        this.title = title;
        this.message = message;
        this.sendStatus = sendStatus;
        this.platforms = platforms;
        this.creator = creator;
        this.campaignStyle = campaignStyle;
        this.createDate = new Date();
        this.updateDate = new Date();
        this.targetAudience = targetAudience;
        this.targetUser = targetUser;
        this.schedule = schedule;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMsgType() { return msgType; }
    public void setMsgType(String msgType) { this.msgType = msgType; }
    public String getMsgMethod() { return msgMethod; }
    public void setMsgMethod(String msgMethod) { this.msgMethod = msgMethod; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSendStatus() { return sendStatus; }
    public void setSendStatus(String sendStatus) { this.sendStatus = sendStatus; }
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getCampaignStyle() { return campaignStyle; }
    public void setCampaignStyle(String campaignStyle) { this.campaignStyle = campaignStyle; }
    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }
    public Date getUpdateDate() { return updateDate; }
    public void setUpdateDate(Date updateDate) { this.updateDate = updateDate; }
    public int getTargetAudience() { return targetAudience; }
    public void setTargetAudience(int targetAudience) { this.targetAudience = targetAudience; }
    public int getTargetUser() { return targetUser; }
    public void setTargetUser(int targetUser) { this.targetUser = targetUser; }
    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    @Override
    public String toString() {
        return String.format("Message{id='%s', type='%s', title='%s', status='%s', audience=%d}",
                id, msgType, title, sendStatus, targetAudience);
    }
}
