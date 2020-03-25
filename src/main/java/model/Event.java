package model;

import java.util.Date;

import org.bson.Document;

public class Event {

    private final int ticketId;
    private final Date time;
    private final Type type;

    public Event(Document document) {
        this(document.getInteger("ticketId"),
                document.getDate("time"),
                Enum.valueOf(Type.class, document.getString("type").toUpperCase()));
    }

    public Event(int ticketId, Date time, Type type) {
        this.ticketId = ticketId;
        this.time = time;
        this.type = type;
    }

    public int getTicketId() {
        return ticketId;
    }

    public Date getTime() {
        return time;
    }

    public Type getType() {
        return type;
    }

    public Document toDocument() {
        return new Document("ticketId", ticketId).append("time", time).append("type", type.toString());
    }

    public enum Type {
        ENTER, EXIT
    }
}
