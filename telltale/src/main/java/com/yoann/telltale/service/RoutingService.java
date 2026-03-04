package com.yoann.telltale.service;

import com.yoann.telltale.dto.RouteForecast;
import com.yoann.telltale.model.Waypoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {

    private final WeatherService weatherService; // On réutilise ton service existant !

    public RoutingService(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    public RouteForecast calculateRoute(Waypoint start, Waypoint end, LocalDateTime departureTime, double avgSpeedKnots) {
        RouteForecast forecast = new RouteForecast();
        forecast.setDepartureName(start.getName());
        forecast.setArrivalName(end.getName());
        forecast.setStartDateTime(departureTime);

        // 1. Calcul de la distance (Formule de Haversine simplifiée ou Pythagore locale pour les petites distances)
        // Pour faire simple ici, on va supposer une ligne droite (Orthodromie)
        double distance = calculateDistance(start, end);
        forecast.setTotalDistanceNm(distance);

        // 2. Durée estimée
        double duration = distance / avgSpeedKnots;
        forecast.setEstimatedDurationHours(duration);

        // 3. Générer les points intermédiaires (tous les X milles ou toutes les heures)
        // Disons un point toutes les heures pour avoir la météo évolutive
        List<RouteForecast.RoutePoint> points = new ArrayList<>();
        
        int steps = (int) Math.ceil(duration); // Nombre d'heures (arrondi sup)
        if (steps < 2) steps = 2; // Au moins départ et arrivée

         for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            
            // Interpolation linéaire
            double lat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * ratio;
            double lon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * ratio;
            
            // Heure estimée de passage
            LocalDateTime timeAtPoint = departureTime.plusMinutes((long) (duration * 60 * ratio));

            RouteForecast.RoutePoint point = new RouteForecast.RoutePoint();
            point.setLatitude(lat);
            point.setLongitude(lon);
            point.setEstimatedTime(timeAtPoint);

            // --- APPEL MÉTÉO ---
            WeatherService.WeatherData weather = weatherService.getPointWeather(lat, lon, timeAtPoint);
            
            // On remplit le point avec les données reçues
            if (weather != null) {
                point.setWindSpeedKnots(weather.windKnots());
                point.setWindDirection(weather.windDir());
                point.setWeatherSummary(weather.summary());
                // On pourrait ajouter les vagues aussi si on modifie le DTO RoutePoint
            }
            
            points.add(point);
        }
        
        forecast.setPoints(points);
        return forecast;
    }

    // Formule rapide pour la distance (Haversine est mieux, mais ça suffit pour commencer)
    private double calculateDistance(Waypoint p1, Waypoint p2) {
        double R = 3440.1; // Rayon Terre en Milles Nautiques
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}