package com.abernathy.note.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notes")
public class Note {

    @Id
    private String id;
    
    private Long patId;
    private String patient;
    private String note;

    public Note() {
    }

    public Note(Long patId, String patient, String note) {
        this.patId = patId;
        this.patient = patient;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getPatId() { return patId; }
    public void setPatId(Long patId) { this.patId = patId; }

    public String getPatient() { return patient; }
    public void setPatient(String patient) { this.patient = patient; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}