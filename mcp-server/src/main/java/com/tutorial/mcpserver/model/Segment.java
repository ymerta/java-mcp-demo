package com.tutorial.mcpserver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Netmera benzeri segment (kullanici segmenti) modeli.
 *
 * Gercek sistemdeki segment collection'indan sadelestirilerek alinmistir.
 * Segmentler, belirli kriterlere uyan kullanici gruplarini tanimlar.
 * Ornegin: "Son 7 gunde giris yapan Android kullanicilari"
 *
 * segmentType degerleri: STANDARD, CONNECTORS_SEGMENT
 * segmentStatus degerleri: READY, WAITING_CALCULATE, WAITING_DELETED
 * entityStatus degerleri: ACTIVE, DELETED
 */
@Document(collection = "segments")
public class Segment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;             // 5 harfli unique segment kodu (ornek: "svysw")

    private String name;             // Segment adi
    private String segmentType;      // STANDARD, CONNECTORS_SEGMENT
    private String segmentStatus;    // READY, WAITING_CALCULATE, WAITING_DELETED
    private String entityStatus;     // ACTIVE, DELETED
    private String createdBy;        // Olusturan kisi
    private Date date;               // Olusturulma tarihi
    private Date updateDate;         // Son guncelleme tarihi
    private int userCount;           // Segmentteki kullanici sayisi
    private DeviceCounts deviceCounts;  // Cihaz bazli dagilim

    /**
     * Embedded document: Cihaz bazli kullanici sayilari.
     * MongoDB'de ic ice (nested) document olarak saklanir.
     */
    public static class DeviceCounts {
        private int android;
        private int ios;
        private int total;

        public DeviceCounts() {}

        public DeviceCounts(int android, int ios, int total) {
            this.android = android;
            this.ios = ios;
            this.total = total;
        }

        public int getAndroid() { return android; }
        public void setAndroid(int android) { this.android = android; }
        public int getIos() { return ios; }
        public void setIos(int ios) { this.ios = ios; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }

    public Segment() {}

    public Segment(String code, String name, String segmentType, String segmentStatus,
                   String entityStatus, String createdBy, int userCount, DeviceCounts deviceCounts) {
        this.code = code;
        this.name = name;
        this.segmentType = segmentType;
        this.segmentStatus = segmentStatus;
        this.entityStatus = entityStatus;
        this.createdBy = createdBy;
        this.date = new Date();
        this.updateDate = new Date();
        this.userCount = userCount;
        this.deviceCounts = deviceCounts;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }
    public String getSegmentStatus() { return segmentStatus; }
    public void setSegmentStatus(String segmentStatus) { this.segmentStatus = segmentStatus; }
    public String getEntityStatus() { return entityStatus; }
    public void setEntityStatus(String entityStatus) { this.entityStatus = entityStatus; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public Date getUpdateDate() { return updateDate; }
    public void setUpdateDate(Date updateDate) { this.updateDate = updateDate; }
    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }
    public DeviceCounts getDeviceCounts() { return deviceCounts; }
    public void setDeviceCounts(DeviceCounts deviceCounts) { this.deviceCounts = deviceCounts; }

    @Override
    public String toString() {
        return String.format("Segment{id='%s', code='%s', name='%s', type='%s', status='%s', users=%d}",
                id, code, name, segmentType, segmentStatus, userCount);
    }
}
