package services;

import java.util.LinkedList;
import java.util.Queue;

import model.Event;

public class EventNotificationService {

    private Queue<Event> newEventsQueue;

    public EventNotificationService() {
        newEventsQueue = new LinkedList<>();
    }

    public void addEvent(Event event) {
        newEventsQueue.add(event);
    }

    public int getSize() {
        return newEventsQueue.size();
    }

    public Event pop() {
        return newEventsQueue.poll();
    }
}
