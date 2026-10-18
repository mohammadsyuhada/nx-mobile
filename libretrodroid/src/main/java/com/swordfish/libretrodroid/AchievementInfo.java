package com.swordfish.libretrodroid;

/** One achievement from rc_client_create_achievement_list. State/type/category are rcheevos' RC_CLIENT_ACHIEVEMENT_* values. */
public final class AchievementInfo {
    public final int id;
    public final String title;
    public final String description;
    public final int points;
    public final String badgeName;
    public final int state;        // 0 inactive, 1 active, 2 unlocked, 3 disabled
    public final long unlockTime;  // epoch seconds, 0 when locked
    public final String measuredProgress;
    public final float measuredPercent;
    public final float rarity;     // softcore unlock rate, percent
    public final int type;         // 0 standard, 1 missable, 2 progression, 3 win
    public final int category;     // 1 core, 2 unofficial

    public AchievementInfo(int id, String title, String description, int points, String badgeName, int state, long unlockTime,
                           String measuredProgress, float measuredPercent, float rarity, int type, int category) {
        this.id = id; this.title = title; this.description = description; this.points = points; this.badgeName = badgeName;
        this.state = state; this.unlockTime = unlockTime; this.measuredProgress = measuredProgress; this.measuredPercent = measuredPercent;
        this.rarity = rarity; this.type = type; this.category = category;
    }
}
