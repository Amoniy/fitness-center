import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.rx.client.Success;
import db.MongoDriver;
import entry.EntryServer;
import model.Event;
import model.Ticket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import services.EventNotificationService;

import static model.Event.Type.ENTER;
import static model.Event.Type.EXIT;

public class EntryServerTest {

    private static final String DATABASE = "entry-test";

    MongoDriver mongoDriver;
    EntryServer server;

    @Before
    public void before() {
        TestSubscriber<Success> subscriber = new TestSubscriber<>();
        MongoDriver.MONGO_CLIENT.getDatabase(DATABASE).drop().subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        mongoDriver = new MongoDriver(MongoDriver.MONGO_CLIENT, DATABASE, new EventNotificationService());
        server = new EntryServer(mongoDriver);
    }

    @Test
    public void testNoId() {
        Map<String, List<String>> params = new HashMap<>();
        Assert.assertEquals("No ticket-id", server.addEnter(params, new Date()).toBlocking().first());
    }

    @Test
    public void testAddEnter() throws Throwable {
        Map<String, List<String>> params = new HashMap<>();
        params.put("ticket-id", Collections.singletonList("1"));

        Date expiry = new Date();
        expiry.setTime(expiry.getTime() + 10000);
        Date creation = new Date();
        mongoDriver.addTicket(new Ticket(1, expiry, creation));

        Date enter = new Date();
        enter.setTime(expiry.getTime() - 5000);
        server.addEnter(params, enter);
        List<Event> events = mongoDriver.getEvents();
        Assert.assertEquals(1, events.size());
        Event event = events.get(0);
        Assert.assertEquals(enter, event.getTime());
        Assert.assertEquals(ENTER, event.getType());
        Assert.assertEquals(1, event.getTicketId());
    }

    @Test
    public void testAddExit() throws Throwable {
        Map<String, List<String>> params = new HashMap<>();
        params.put("ticket-id", Collections.singletonList("1"));

        Date expiry = new Date();
        expiry.setTime(expiry.getTime() + 10000);
        Date creation = new Date();
        mongoDriver.addTicket(new Ticket(1, expiry, creation));

        Date enter = new Date();
        enter.setTime(expiry.getTime() - 5000);
        server.addExit(params, enter);
        List<Event> events = mongoDriver.getEvents();
        Assert.assertEquals(1, events.size());
        Event event = events.get(0);
        Assert.assertEquals(enter, event.getTime());
        Assert.assertEquals(EXIT, event.getType());
        Assert.assertEquals(1, event.getTicketId());
    }
}
