package com.example.seminariofinal;

public class CallHistory {

    private int id;
    private String contactName;
    private String contactPhone;
    private String date;
    private String type;
    private boolean outgoing;

    public CallHistory(
            int id,
            String contactName,
            String contactPhone,
            String date,
            String type,
            boolean outgoing
    ) {
        this.id = id;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.date = date;
        this.type = type;
        this.outgoing = outgoing;
    }

    public int getId() {
        return id;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public boolean isOutgoing() {
        return outgoing;
    }
}