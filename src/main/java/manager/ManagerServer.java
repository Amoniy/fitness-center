package manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mongodb.rx.client.Success;
import db.MongoDriver;
import io.reactivex.netty.protocol.http.server.HttpServer;
import model.Ticket;
import rx.Observable;

public class ManagerServer {

    private final static List<String> GET_TICKET_PARAMS = Collections.singletonList("ticket-id");
    private final static List<String> UPDATE_TICKET_PARAMS = Arrays.asList("ticket-id", "date");

    private final MongoDriver mongoDriver;

    public ManagerServer(MongoDriver mongoDriver) {
        this.mongoDriver = mongoDriver;
    }

    public void run() {
        HttpServer.newServer(8081).start((request, response) -> {
            String method = request.getDecodedPath().substring(1);
            Map<String, List<String>> params = request.getQueryParameters();
            if ("getTicketInfo".equals(method)) {
                return response.writeString(getTicket(params));
            }
            if ("addTicketInfo".equals(method)) {
                return response.writeString(handleTicketOperation(params, new Date()));
            }
            return response.writeString(Observable.just("Unknown command"));
        }).awaitShutdown();
    }

    public Observable<String> getTicket(Map<String, List<String>> params) {
        String validation = validate(params, GET_TICKET_PARAMS);
        if (validation.length() > 0) {
            return Observable.just(validation);
        }

        int id = Integer.parseInt(params.get("ticket-id").get(0));
        try {
            Ticket ticket = mongoDriver.getLatestTicket(id);
            return Observable.just(ticket == null ? "No tickets found" : ticket.toString());
        } catch (Throwable throwable) {
            return Observable.just("Exception encountered: " + throwable.getMessage());
        }
    }

    public Observable<String> handleTicketOperation(Map<String, List<String>> params, Date creationDate) {
        String validation = validate(params, UPDATE_TICKET_PARAMS);
        if (validation.length() > 0) {
            return Observable.just(validation);
        }

        int id = Integer.parseInt(params.get("ticket-id").get(0));
        Date date;
        try {
            date = new SimpleDateFormat("dd-MM-yyyy").parse(params.get("date").get(0));
        } catch (ParseException e) {
            return Observable.just("Incorrect date format");
        }
        return addTicket(id, date, creationDate);
    }

    public Observable<String> addTicket(int id, Date expiryDate, Date creationDate) {
        if (mongoDriver.addTicket(new Ticket(id, expiryDate, creationDate)) == Success.SUCCESS) {
            return Observable.just("Created or updated ticket");
        } else {
            return Observable.just("Exception updating expiry date");
        }
    }

    private String validate(Map<String, List<String>> params, List<String> expectedParams) {
        List<String> missingParams = expectedParams.stream().filter(param -> !params.containsKey(param)).collect(Collectors.toList());
        if (missingParams.isEmpty()) {
            return "";
        }
        return "Please add params: " + String.join(", ", missingParams);
    }
}
