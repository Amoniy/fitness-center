import java.util.Calendar;
import java.util.Date;

import com.mongodb.rx.client.Success;
import db.MongoDriver;
import model.Event;
import model.Ticket;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import report.ReportServer;
import rx.observers.TestSubscriber;
import services.EventNotificationService;

import static model.Event.Type.ENTER;
import static model.Event.Type.EXIT;

public class ReportServerTest {

    private static final String DATABASE = "report-test";

    MongoDriver mongoDriver;
    ReportServer server;

    @Before
    public void before() throws Throwable {
        TestSubscriber<Success> subscriber = new TestSubscriber<>();
        MongoDriver.MONGO_CLIENT.getDatabase(DATABASE).drop().subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        EventNotificationService eventNotificationService = new EventNotificationService();
        mongoDriver = new MongoDriver(MongoDriver.MONGO_CLIENT, DATABASE, eventNotificationService);
        server = new ReportServer(mongoDriver, eventNotificationService);
    }

    @Test
    public void testStats() {
        fillEnters();
        Assert.assertEquals("1 3 2020: 2\n" +
                "2 3 2020: 1\n", server.stats());
    }

    @Test
    public void testMedianLength() {
        fillEnters();
        Assert.assertEquals("Median length is 2 hours and 6 minutes", server.medianLength());
    }

    private void fillEnters() {
        Date expiry = new Date();
        expiry.setDate(1);
        expiry.setMonth(Calendar.MARCH);
        expiry.setYear(2020 - 1900);
        expiry.setHours(5);
        Date creation = new Date();
        mongoDriver.addTicket(new Ticket(1, expiry, creation));

        Date enter = new Date();
        enter.setTime(expiry.getTime() - 5000);
        mongoDriver.addEvent(new Event(1, enter, ENTER));

        long oneHour = 60 * 60 * 1000;
        long oneMinute = 60 * 1000;
        Date exit = new Date();
        exit.setTime(enter.getTime() + oneHour + 5 * oneMinute);
        mongoDriver.addEvent(new Event(1, exit, EXIT));

        Date enter2 = new Date();
        enter2.setTime(exit.getTime() + 5000);
        mongoDriver.addEvent(new Event(1, enter2, ENTER));

        Date exit2 = new Date();
        exit2.setTime(enter2.getTime() + oneHour * 2 + 6 * oneMinute);
        mongoDriver.addEvent(new Event(1, exit2, EXIT));

        Date enter3 = new Date();
        enter3.setTime(exit2.getTime() + 24 * oneHour); // skip one day
        mongoDriver.addEvent(new Event(2, enter3, ENTER));

        Date exit3 = new Date();
        exit3.setTime(enter3.getTime() + oneHour * 3 + 7 * oneMinute);
        mongoDriver.addEvent(new Event(2, exit3, EXIT));
    }
}
