package com.lifelink.util;

public final class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtil() {
    }

    /** Great-circle distance in km (haversine). */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
