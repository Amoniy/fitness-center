package model;

import java.util.Date;

import org.bson.Document;

public class Ticket {

    private final int id;
    private final Date expiryDate;
    private final Date creationDate;

    public Ticket(Document document) {
        this(document.getInteger("id"), document.getDate("expiryDate"), document.getDate("creationDate"));
    }

    public Ticket(int id, Date expiryDate, Date creationDate) {
        this.id = id;
        this.expiryDate = expiryDate;
        this.creationDate = creationDate;
    }

    public int getId() {
        return id;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Document toDocument() {
        return new Document("id", id).append("expiryDate", expiryDate).append("creationDate", creationDate);
    }

    @Override
    public String toString() {
        return "Ticket: id = " + id + ", expiry date = " + expiryDate.toString() + ", creation date = " + creationDate.toString();
    }
}
