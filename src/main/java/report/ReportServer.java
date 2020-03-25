package report;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import db.MongoDriver;
import io.reactivex.netty.protocol.http.server.HttpServer;
import model.Event;
import rx.Observable;
import services.EventNotificationService;

public class ReportServer {

    private final MongoDriver mongoDriver;
    private final EventNotificationService notificationService;
    private List<Event> events;

    public ReportServer(MongoDriver mongoDriver, EventNotificationService notificationService) throws Throwable {
        this.mongoDriver = mongoDriver;
        this.notificationService = notificationService;
        events = mongoDriver.getEvents();
    }

    public void run() {
        HttpServer.newServer(8082).start((request, response) -> {
            String method = request.getDecodedPath().substring(1);
            Map<String, List<String>> params = request.getQueryParameters();
            if ("stats".equals(method)) {
                return response.writeString(Observable.just(stats()));
            }
            if ("median-length".equals(method)) {
                return response.writeString(Observable.just(medianLength()));
            }
            return response.writeString(Observable.just("Unknown command"));
        }).awaitShutdown();
    }

    private void updateEvents() {
        while (notificationService.getSize() > 0) {
            events.add(notificationService.pop());
        }
    }

    public String stats() {
        updateEvents();
        Map<String, List<Event>> eventsByDay = events.stream().filter(event -> event.getType() == Event.Type.ENTER)
                .collect(Collectors.groupingBy(event -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(event.getTime());
                    return calendar.get(Calendar.DAY_OF_MONTH) + " " + (calendar.get(Calendar.MONTH) + 1) + " " + calendar.get(Calendar.YEAR);
                }));
        StringBuilder stringBuilder = new StringBuilder();
        eventsByDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\n"));
        return stringBuilder.toString();
    }

    public String medianLength() {
        updateEvents();
        Map<Integer, List<Event>> eventsByTicketId = events.stream().collect(Collectors.groupingBy(Event::getTicketId));
        long minutesSum = 0;
        int recordedSessions = 0;
        for (List<Event> eventList : eventsByTicketId.values()) {
            eventList.sort(Comparator.comparingLong(event -> event.getTime().getTime()));
            Event previous = null;
            for (Event event : eventList) {
                if (event.getType() == Event.Type.ENTER) {
                    previous = event;
                } else if (previous != null && event.getType() == Event.Type.EXIT) {
                    minutesSum += (event.getTime().getTime() - previous.getTime().getTime()) / 1000 / 60;
                    recordedSessions++;
                    previous = null;
                }
            }
        }

        if (recordedSessions == 0) {
            return "No sessions found";
        }
        int medianMinutes = (int) (minutesSum / recordedSessions);
        return "Median length is " + medianMinutes / 60 + " hours and " + medianMinutes % 60 + " minutes";
    }
}
