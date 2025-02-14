package services;

import database.InMemoryDB;
import models.Driver;

public class AdminService {
    private final InMemoryDB db;

    public AdminService(InMemoryDB db) {
        this.db = db;
    }

    public void removeDriver(String driverID) {
        if (db.driverDetails.get(driverID) == null) {
            System.out.println("INVALID_DRIVER_ID");
            return;
        }

        db.driverDetails.remove(driverID);
        System.out.println("REMOVED_DRIVER " + driverID);
    }

    public void listNDriverDetails(int N) {
        int size = Math.min(db.driverDetails.size(), N);
        int idx = 0;

        for (String driverID : db.driverDetails.keySet()) {
            if (idx == size) break;

            idx++;
            Driver driver = db.driverDetails.get(driverID);

            System.out.printf("DRIVER_%s (X=%d, Y=%d) RATING %.1f%n", driverID, driver.coordinates[0], driver.coordinates[1], driver.rating);
        }
    }
}
