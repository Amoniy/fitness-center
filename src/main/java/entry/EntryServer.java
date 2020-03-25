package entry;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mongodb.rx.client.Success;
import db.MongoDriver;
import io.reactivex.netty.protocol.http.server.HttpServer;
import model.Event;
import model.Ticket;
import rx.Observable;

public class EntryServer {

    private final MongoDriver mongoDriver;

    public EntryServer(MongoDriver mongoDriver) {
        this.mongoDriver = mongoDriver;
    }

    public void run() {
        HttpServer.newServer(8080).start((request, response) -> {
            String method = request.getDecodedPath().substring(1);
            Map<String, List<String>> params = request.getQueryParameters();
            if ("enter".equals(method)) {
                return response.writeString(addEnter(params, new Date()));
            }
            if ("exit".equals(method)) {
                return response.writeString(addExit(params, new Date()));
            }
            return response.writeString(Observable.just("Unknown command"));
        }).awaitShutdown();
    }

    public Observable<String> addEnter(Map<String, List<String>> params, Date date) {
        String validation = validate(params);
        if (validation.length() > 0) {
            return Observable.just(validation);
        }

        int id = Integer.parseInt(params.get("ticket-id").get(0));
        Ticket ticket;
        try {
            ticket = mongoDriver.getLatestTicket(id);
        } catch (Throwable throwable) {
            return Observable.just("Exception encountered: " + throwable.getMessage());
        }
        if (ticket == null) {
            return Observable.just("No tickets found");
        }
        Date endDate = ticket.getExpiryDate();
        if (date.after(endDate)) {
            return Observable.just("Ticket expired");
        }

        Event event = new Event(id, date, Event.Type.ENTER);
        if (mongoDriver.addEvent(event) == Success.SUCCESS) {
            return Observable.just("New enter");
        } else {
            return Observable.just("Error");
        }
    }

    public Observable<String> addExit(Map<String, List<String>> params, Date date) {
        String validation = validate(params);
        if (validation.length() > 0) {
            return Observable.just(validation);
        }

        int id = Integer.parseInt(params.get("ticket-id").get(0));
        Event event = new Event(id, date, Event.Type.EXIT);
        if (mongoDriver.addEvent(event) == Success.SUCCESS) {
            return Observable.just("New exit");
        } else {
            return Observable.just("Error");
        }
    }

    private String validate(Map<String, List<String>> params) {
        if (params.containsKey("ticket-id")) {
            return "";
        }
        return "No ticket-id";
    }
}
