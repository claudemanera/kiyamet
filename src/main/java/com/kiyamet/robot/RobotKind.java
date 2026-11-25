package com.kiyamet.robot;

public enum RobotKind {
    ROBOT_TROOPER("robot_trooper", 40, 6, 8, 0.27, 1.0f, 0.8f, 1.94f, false, 5f, 40),
    ROBOT_HEAVY("robot_heavy", 110, 13, 15, 0.2, 1.3f, 1.0f, 1.94f, false, 9f, 70),
    SCOUT_DRONE("scout_drone", 18, 3, 2, 0.3, 1.0f, 0.88f, 0.5f, true, 3f, 30);

    public final String id;
    public final double health, damage, armor, speed;
    public final float scale, width, height, laser;
    public final boolean flying;
    public final int laserCooldown;

    RobotKind(String id, double health, double damage, double armor, double speed, float scale, float w, float h, boolean flying, float laser, int laserCooldown) {
        this.id = id; this.health = health; this.damage = damage; this.armor = armor; this.speed = speed;
        this.scale = scale; this.width = w * scale; this.height = h * scale; this.flying = flying; this.laser = laser; this.laserCooldown = laserCooldown;
    }
}
