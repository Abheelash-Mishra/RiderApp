package org.example;

import org.example.database.Database;
import org.example.database.InMemoryDB;
import org.example.database.MockRealDB;
import org.example.services.admin.AdminService;
import org.example.services.admin.exceptions.InvalidDriverIDException;
import org.example.services.admin.impl.AdminServiceConsoleImpl;
import org.example.services.admin.impl.AdminServiceRestImpl;
import org.example.services.driver.DriverService;
import org.example.services.driver.impl.DriverServiceConsoleImpl;
import org.example.services.driver.impl.DriverServiceRestImpl;
import org.example.services.payment.PaymentMethodType;
import org.example.services.payment.PaymentService;
import org.example.services.payment.impl.WalletPayment;
import org.example.services.ride.RideService;
import org.example.services.ride.exceptions.InvalidRideException;
import org.example.services.ride.impl.RideServiceConsoleImpl;
import org.example.services.ride.impl.RideServiceRestImpl;

import java.util.Scanner;

public class RiderApp {
    private static Scanner scanner = new Scanner(System.in);

    private static Database db = InMemoryDB.getInstance();
    private static AdminService adminService = new AdminService(new AdminServiceConsoleImpl(db));
    private static RideService rideService = new RideService(new RideServiceConsoleImpl(db));
    private static DriverService driverService = new DriverService(new DriverServiceConsoleImpl(db));
    private static PaymentService paymentService = new PaymentService(PaymentMethodType.CASH, db);

    public static void reset() {
        InMemoryDB.reset();
        MockRealDB.reset();

        adminService = new AdminService(new AdminServiceConsoleImpl(db));
        rideService = new RideService(new RideServiceConsoleImpl(db));
        driverService = new DriverService(new DriverServiceConsoleImpl(db));
        paymentService = new PaymentService(PaymentMethodType.CASH, db);

        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        db = InMemoryDB.getInstance();

        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) break;

            processCommands(command);
        }

        scanner.close();
    }

    public static void processCommands(String command) {
        String[] parts = command.split(" ");
        int x_coordinate, y_coordinate, N;
        String riderID, rideID, driverID;

        try {
            switch (parts[0]) {
                case "CONNECT_MRD":
                    MockRealDB.reset();
                    db = MockRealDB.getInstance();
                    db.connect();
                    break;

                case "CONNECT_IMDB":
                    InMemoryDB.reset();
                    db = InMemoryDB.getInstance();
                    db.connect();
                    break;

                case "USE_REST_IMPL":
                    adminService = new AdminService(new AdminServiceRestImpl(db));
                    rideService = new RideService(new RideServiceRestImpl(db));
                    driverService = new DriverService(new DriverServiceRestImpl(db));

                    break;

                case "ADD_DRIVER":
                    driverID = parts[1];
                    x_coordinate = Integer.parseInt(parts[2]);
                    y_coordinate = Integer.parseInt(parts[3]);

                    driverService.addDriver(driverID, x_coordinate, y_coordinate);
                    break;

                case "ADD_RIDER":
                    riderID = parts[1];
                    x_coordinate = Integer.parseInt(parts[2]);
                    y_coordinate = Integer.parseInt(parts[3]);

                    rideService.addRider(riderID, x_coordinate, y_coordinate);
                    break;

                case "MATCH":
                    riderID = parts[1];

                    rideService.matchRider(riderID);
                    break;

                case "START_RIDE":
                    rideID = parts[1];
                    N = Integer.parseInt(parts[2]);
                    riderID = parts[3];

                    rideService.startRide(rideID, N, riderID);
                    break;

                case "STOP_RIDE":
                    rideID = parts[1];
                    int dest_x_coordinate = Integer.parseInt(parts[2]);
                    int dest_y_coordinate = Integer.parseInt(parts[3]);
                    int timeTakenInMins = Integer.parseInt(parts[4]);

                    rideService.stopRide(rideID, dest_x_coordinate, dest_y_coordinate, timeTakenInMins);
                    break;

                case "RATE_DRIVER":
                    driverID = parts[1];
                    float rating = Float.parseFloat(parts[2]);

                    driverService.rateDriver(driverID, rating);
                    break;

                case "BILL":
                    rideID = parts[1];

                    rideService.billRide(rideID);
                    break;

                case "PAY":
                    rideID = parts[1];
                    String method = parts[2];

                    PaymentMethodType paymentMethodType = PaymentMethodType.valueOf(method.toUpperCase());
                    paymentService.setPaymentMethod(paymentMethodType, db);

                    paymentService.processPayment(rideID);
                    break;

                case "ADD_MONEY":
                    riderID = parts[1];
                    float amount = Float.parseFloat(parts[2]);

                    paymentService.setPaymentMethod(PaymentMethodType.WALLET, db);
                    WalletPayment wallet = (WalletPayment) paymentService.getPaymentMethod();
                    wallet.addMoney(riderID, amount);
                    break;

                case "ADMIN_REMOVE_DRIVER":
                    driverID = parts[1];

                    adminService.removeDriver(driverID);
                    break;

                case "ADMIN_LIST_DRIVERS":
                    N = Integer.parseInt(parts[1]);

                    adminService.listNDriverDetails(N);
                    break;

                case "ADMIN_VIEW_DRIVER_EARNINGS":
                    driverID = parts[1];

                    adminService.getDriverEarnings(driverID);
                    break;

                default:
                    break;
            }
        } catch (InvalidRideException | InvalidDriverIDException e) {
            System.out.println(e.getMessage());
        }
    }
}