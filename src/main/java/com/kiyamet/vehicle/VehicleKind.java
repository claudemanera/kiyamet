package com.kiyamet.vehicle;

/** The 8 drivable vehicles. Seats are (x right-left, y up, z forward) in blocks. */
public enum VehicleKind {
    //          id                 title              hp   speed  turn  w     h     trunk fuel   armor ram  armored lab
    SEDAN        ("car_sedan",       "Sedan",           60,  0.40, 5.0f, 2.4f, 1.4f, 9,   6000,  4,  1.0f, false, 40,
            new float[][]{{0.4f, 0.3f, 0.3f}, {-0.4f, 0.3f, 0.3f}, {0.4f, 0.3f, -0.7f}, {-0.4f, 0.3f, -0.7f}}),
    PICKUP       ("car_pickup",      "Pickup Truck",    80,  0.37, 4.5f, 2.4f, 1.6f, 27,  7000,  6,  1.2f, false, 100,
            new float[][]{{0.4f, 0.45f, 0.6f}, {-0.4f, 0.45f, 0.6f}, {0.45f, 0.55f, -0.9f}, {-0.45f, 0.55f, -0.9f}}),
    POLICE       ("car_police",      "Police Cruiser",  70,  0.44, 5.5f, 2.4f, 1.5f, 9,   6000,  6,  1.2f, false, 150,
            new float[][]{{0.4f, 0.3f, 0.3f}, {-0.4f, 0.3f, 0.3f}, {0.4f, 0.3f, -0.7f}, {-0.4f, 0.3f, -0.7f}}),
    AMBULANCE    ("car_ambulance",   "Ambulance",       90,  0.34, 4.0f, 2.4f, 2.0f, 18,  7000,  6,  1.0f, false, 200,
            new float[][]{{0.4f, 0.45f, 1.0f}, {-0.4f, 0.45f, 1.0f}, {0.45f, 0.5f, -0.8f}, {-0.45f, 0.5f, -0.8f}}),
    ARMORED_SUV  ("car_armored_suv", "Armored SUV",     140, 0.36, 4.2f, 2.5f, 2.0f, 18,  8000,  14, 1.6f, true,  300,
            new float[][]{{0.45f, 0.5f, 0.3f}, {-0.45f, 0.5f, 0.3f}, {0.45f, 0.5f, -0.8f}, {-0.45f, 0.5f, -0.8f}}),
    ATV          ("atv",             "ATV",             40,  0.38, 7.0f, 1.4f, 1.0f, 9,   4000,  2,  0.6f, false, 30,
            new float[][]{{0f, 0.45f, 0.05f}, {0f, 0.55f, -0.45f}}),
    MOTORCYCLE   ("motorcycle",      "Motorcycle",      30,  0.50, 7.0f, 1.0f, 1.1f, 0,   3500,  1,  0.5f, false, 60,
            new float[][]{{0f, 0.45f, 0.0f}, {0f, 0.5f, -0.45f}}),
    ARMORED_TRUCK("armored_truck",   "Armored Truck",   260, 0.30, 3.2f, 2.6f, 2.2f, 27,  10000, 20, 2.2f, true,  450,
            new float[][]{{0.45f, 0.75f, 1.1f}, {-0.45f, 0.75f, 1.1f}, {0.5f, 0.8f, -0.3f}, {-0.5f, 0.8f, -0.3f}, {0.5f, 0.8f, -1.1f}, {-0.5f, 0.8f, -1.1f}});

    public final String id, title;
    public final double health, speed;
    public final float turn, width, height, ram;
    public final int trunk, fuel, armor, lab;
    public final boolean armored;
    public final float[][] seats;

    VehicleKind(String id, String title, double health, double speed, float turn, float width, float height, int trunk, int fuel, int armor, float ram, boolean armored, int lab, float[][] seats) {
        this.id = id; this.title = title; this.health = health; this.speed = speed; this.turn = turn; this.width = width; this.height = height;
        this.trunk = trunk; this.fuel = fuel; this.armor = armor; this.ram = ram; this.armored = armored; this.lab = lab; this.seats = seats;
    }

    public String kitId() { return id + "_keys"; }
}
