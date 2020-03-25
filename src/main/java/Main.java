import db.MongoDriver;
import entry.EntryServer;
import manager.ManagerServer;
import report.ReportServer;
import services.EventNotificationService;

public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        main.run();
    }

    public void run() {
        EventNotificationService notificationService = new EventNotificationService();
        String database = "fitness-center";
        EntryServer entryServer = new EntryServer(new MongoDriver(MongoDriver.MONGO_CLIENT, database, notificationService));
        ManagerServer managerServer = new ManagerServer(new MongoDriver(MongoDriver.MONGO_CLIENT, database, notificationService));
        ReportServer reportServer;
        try {
            reportServer = new ReportServer(new MongoDriver(MongoDriver.MONGO_CLIENT, database, notificationService), notificationService);
        } catch (Throwable throwable) {
            System.out.println("Could not start report server");
            throwable.printStackTrace();
            return;
        }
        new Thread(new ManagerServerRunner(managerServer)).start();
        new Thread(new EntryServerRunner(entryServer)).start();
        new Thread(new ReportServerRunner(reportServer)).start();
    }

    public class EntryServerRunner implements Runnable {

        private final EntryServer entryServer;

        public EntryServerRunner(EntryServer entryServer) {
            this.entryServer = entryServer;
        }

        @Override
        public void run() {
            entryServer.run();
        }
    }

    public class ManagerServerRunner implements Runnable {

        private final ManagerServer managerServer;

        public ManagerServerRunner(ManagerServer managerServer) {
            this.managerServer = managerServer;
        }

        @Override
        public void run() {
            managerServer.run();
        }
    }

    public class ReportServerRunner implements Runnable {

        private final ReportServer reportServer;

        public ReportServerRunner(ReportServer reportServer) {
            this.reportServer = reportServer;
        }

        @Override
        public void run() {
            reportServer.run();
        }
    }
}
