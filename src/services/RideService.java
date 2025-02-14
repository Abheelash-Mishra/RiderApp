package services;

import database.InMemoryDB;
import models.Driver;
import models.Ride;
import models.Rider;
import utilities.DistanceUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class RideService {
    private final InMemoryDB db;
    private final PaymentService paymentService;

    public RideService(InMemoryDB db) {
        this.db = db;

        this.paymentService = new PaymentService(db);
    }

    public void addRider(String riderID, int x_coordinate, int y_coordinate) {
        db.riderDetails.put(riderID, new Rider(riderID, x_coordinate, y_coordinate));
    }

    public void matchRider(String riderID) {
        final double LIMIT = 5.0;
        int[] riderCoordinates = db.riderDetails.get(riderID).coordinates;

        HashMap<String, Driver> allDrivers = db.driverDetails;
        PriorityQueue<DriverDistancePair> nearestDrivers = new PriorityQueue<>((pair1, pair2) -> {
            if (pair1.distance < pair2.distance) {
                return -1;
            } else if (pair1.distance > pair2.distance) {
                return 1;
            }

            return pair1.ID.compareTo(pair2.ID);
        });

        for (String driverID : allDrivers.keySet()) {
            Driver driver = allDrivers.get(driverID);

            if (driver.available) {
                double distance = DistanceUtility.calculate(riderCoordinates, driver.coordinates);

                if (distance <= LIMIT) {
                    DriverDistancePair pair = new DriverDistancePair(driverID, distance);
                    nearestDrivers.add(pair);
                }
            }
        }

        try {
            driversMatched(riderID, nearestDrivers);
        } catch (NoDriversException e) {
            System.out.println(e.getMessage());
        }
    }

    private void driversMatched(String riderID, PriorityQueue<DriverDistancePair> nearestDrivers) throws NoDriversException {
        if (nearestDrivers.isEmpty()) {
            throw new NoDriversException();
        }

        System.out.print("DRIVERS_MATCHED");
        db.riderDriverMapping.putIfAbsent(riderID, new ArrayList<>());
        int size = Math.min(nearestDrivers.size(), 5);

        for (int i = 0; i < size; i++) {
            DriverDistancePair driver = nearestDrivers.poll();
            if (driver != null) {
                db.riderDriverMapping.get(riderID).add(driver.ID);
                System.out.print(" " + driver.ID);
            }
        }
        System.out.println();
    }

    public void startRide(String rideID, int N, String riderID) throws InvalidRideException {
        List<String> matchedDrivers = db.riderDriverMapping.get(riderID);

        if (matchedDrivers.size() < N) {
            throw new InvalidRideException();
        }

        String driverID = matchedDrivers.get(N - 1);
        boolean driverAvailable = db.driverDetails.get(driverID).available;

        if (!driverAvailable || db.rideDetails.containsKey(rideID)) {
            throw new InvalidRideException();
        }

        db.rideDetails.put(rideID, new Ride(riderID, driverID));
        db.driverDetails.get(driverID).updateAvailability();

        System.out.println("RIDE_STARTED " + rideID);
    }

    public void stopRide(String rideID, int dest_x_coordinate, int dest_y_coordinate, int timeTakenInMins) throws InvalidRideException {
        Ride currentRide = db.rideDetails.get(rideID);
        if (currentRide == null || currentRide.hasFinished) {
            throw new InvalidRideException();
        }

        System.out.println("RIDE_STOPPED " + rideID);
        db.driverDetails.get(currentRide.driverID).updateAvailability();
        currentRide.finishRide(dest_x_coordinate, dest_y_coordinate, timeTakenInMins);
    }

    public void billRide(String rideID) {
        Ride currentRide = db.rideDetails.get(rideID);

        final double BASE_FARE = 50.0;
        final double PER_KM = 6.5;
        final double PER_MIN = 2.0;
        final double SERVICE_TAX = 1.2;      // 20 percent

        double finalBill = BASE_FARE;

        int[] startCoordinates = db.riderDetails.get(currentRide.riderID).coordinates;
        int[] destCoordinates = currentRide.destinationCoordinates;
        double distanceTravelled = DistanceUtility.calculate(startCoordinates, destCoordinates);
        finalBill += (distanceTravelled * PER_KM);

        int timeTakenInMins = currentRide.timeTakenInMins;
        finalBill += (timeTakenInMins * PER_MIN);

        finalBill *= SERVICE_TAX;

        currentRide.bill = (float) (Math.round(finalBill * 10.0) / 10.0);
        System.out.println("BILL " + rideID + " " + currentRide.driverID + " " + String.format("%.1f", finalBill));
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }


    public record DriverDistancePair(String ID, double distance) {
    }

    public static class InvalidRideException extends Exception {
        public InvalidRideException() {
            super("INVALID_RIDE");
        }
    }

    public static class NoDriversException extends Exception {
        public NoDriversException() {
            super("NO_DRIVERS_AVAILABLE");
        }
    }
}
