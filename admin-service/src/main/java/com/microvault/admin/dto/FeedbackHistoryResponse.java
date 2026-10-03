package com.microvault.admin.dto;

public class FeedbackHistoryResponse {
    private String action;
    private String note;
    private String at;
    private String by;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }
    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }
}
