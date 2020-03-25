import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.rx.client.Success;
import db.MongoDriver;
import manager.ManagerServer;
import model.Ticket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import services.EventNotificationService;

public class ManagerServerTest {

    private static final String DATABASE = "manager-test";

    MongoDriver mongoDriver;
    ManagerServer server;

    @Before
    public void before() {
        TestSubscriber<Success> subscriber = new TestSubscriber<>();
        MongoDriver.MONGO_CLIENT.getDatabase(DATABASE).drop().subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        mongoDriver = new MongoDriver(MongoDriver.MONGO_CLIENT, DATABASE, new EventNotificationService());
        server = new ManagerServer(mongoDriver);
    }

    @Test
    public void testNecessaryParams() {
        Map<String, List<String>> params = new HashMap<>();
        Assert.assertEquals("Please add params: ticket-id, date", server.handleTicketOperation(params, new Date()).toBlocking().first());
    }

    @Test
    public void testAddTicketVersions() throws Throwable {
        Map<String, List<String>> params = new HashMap<>();
        params.put("ticket-id", Collections.singletonList("1"));
        params.put("date", Collections.singletonList("24-03-2020"));

        Date date = new Date();
        server.handleTicketOperation(params, date);
        List<Ticket> tickets = mongoDriver.getTicketVersions(1);
        Assert.assertEquals(1, tickets.size());
        Ticket ticket = tickets.get(0);
        Assert.assertEquals(1, ticket.getId());
        Assert.assertEquals(date, ticket.getCreationDate());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ticket.getExpiryDate());
        Assert.assertEquals("24-03-2020",
                calendar.get(Calendar.DAY_OF_MONTH) + "-0" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR));
    }

    @Test
    public void testAddTicketLatestVersion() throws Throwable {
        Map<String, List<String>> params = new HashMap<>();
        params.put("ticket-id", Collections.singletonList("1"));
        params.put("date", Collections.singletonList("24-03-2020"));

        Date date = new Date();
        server.handleTicketOperation(params, date);
        Ticket ticket = mongoDriver.getLatestTicket(1);
        Assert.assertEquals(1, ticket.getId());
        Assert.assertEquals(date, ticket.getCreationDate());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ticket.getExpiryDate());
        Assert.assertEquals("24-03-2020",
                calendar.get(Calendar.DAY_OF_MONTH) + "-0" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR));
    }
}
