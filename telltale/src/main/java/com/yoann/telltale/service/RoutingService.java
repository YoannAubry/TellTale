package com.yoann.telltale.service;

import com.yoann.telltale.dto.RouteForecast;
import com.yoann.telltale.model.Waypoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {

    private final WeatherService weatherService;
    private final BoatPerformanceService performanceService;

    public RoutingService(WeatherService weatherService, BoatPerformanceService performanceService) {
        this.weatherService = weatherService;
        this.performanceService = performanceService;
    }

    public RouteForecast calculateRoute(Waypoint start, Waypoint end, List<Waypoint> steps, LocalDateTime departureTime, double avgSpeedKnots) {
        RouteForecast forecast = new RouteForecast();
        forecast.setDepartureName(start.getName());
        forecast.setArrivalName(end.getName());
        forecast.setStartDateTime(departureTime);

        // 1. Construire la liste ordonnée
        List<Waypoint> fullPath = new ArrayList<>();
        fullPath.add(start);
        if (steps != null && !steps.isEmpty()) {
            fullPath.addAll(steps);
        }
        fullPath.add(end);

        List<RouteForecast.RoutePoint> allPoints = new ArrayList<>();
        double totalDistance = 0;
        double totalDuration = 0;
        LocalDateTime currentSegmentStartTime = departureTime;

        // 2. Boucler sur chaque segment
        for (int i = 0; i < fullPath.size() - 1; i++) {
            Waypoint p1 = fullPath.get(i);
            Waypoint p2 = fullPath.get(i + 1);

            // Calcul du tronçon
            SegmentResult segment = calculateSegment(p1, p2, currentSegmentStartTime);

            totalDistance += segment.distance;
            totalDuration += segment.duration;
            
            // FUSION INTELLIGENTE DES LISTES
            // Si ce n'est pas le premier segment, on supprime le point de "fin" du segment précédent 
            // (qui avait une vitesse 0.0 car considéré comme arrivée)
            // pour le remplacer par le point de "début" du nouveau segment (qui a une vitesse de départ).
            if (i > 0 && !allPoints.isEmpty() && !segment.points.isEmpty()) {
                allPoints.remove(allPoints.size() - 1);
            }
            
            allPoints.addAll(segment.points);

            // Préparer l'heure pour la suite
            currentSegmentStartTime = segment.endTime;
        }

        forecast.setTotalDistanceNm(totalDistance);
        forecast.setEstimatedDurationHours(totalDuration);
        forecast.setPoints(allPoints);

        return forecast;
    }

    private SegmentResult calculateSegment(Waypoint start, Waypoint end, LocalDateTime startTime) {
        double totalDist = calculateDistance(start, end);
        double heading = calculateBearing(start, end);

        List<RouteForecast.RoutePoint> points = new ArrayList<>();
        
        double remainingDist = totalDist;
        double currentLat = start.getLatitude();
        double currentLon = start.getLongitude();
        LocalDateTime currentTime = startTime;
        
        // Initialisation Météo
        var weather = weatherService.getPointWeather(currentLat, currentLon, currentTime);
        
        double timeStepHours = 0.5;

        // Boucle de simulation
        while (remainingDist > 0.05) { // Tant qu'il reste un peu de route (seuil 0.05 milles)
            
            // 1. Calcul de la vitesse PREVUE pour ce tronçon (SOG)
            // On utilise la météo actuelle pour savoir à quelle vitesse on va partir d'ici
            double sog = calculateSog(weather, heading);

            // 2. Enregistrement du point AVEC sa vitesse future
            // L'utilisateur verra : "A 09:00, Vent 15nds, Je file à 6.5nds"
            points.add(createRoutePoint(currentLat, currentLon, currentTime, weather, sog, heading));

            // 3. Avancement
            double distStep = sog * timeStepHours;

            // Ajustement dernier pas
            if (distStep > remainingDist) {
                distStep = remainingDist;
                timeStepHours = distStep / sog;
            }

            // Interpolation position
            double totalRatio = 1.0 - ((remainingDist - distStep) / totalDist);
            double newLat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * totalRatio;
            double newLon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * totalRatio;
            
            // Mise à jour temps & espace
            currentTime = currentTime.plusMinutes((long)(timeStepHours * 60));
            remainingDist -= distStep;
            currentLat = newLat;
            currentLon = newLon;

            // 4. Météo pour le prochain tour
            weather = weatherService.getPointWeather(currentLat, currentLon, currentTime);
        }

        // Ajout du point d'arrivée (Vitesse 0.0 car stop)
        points.add(createRoutePoint(currentLat, currentLon, currentTime, weather, 0.0, 0.0));

        java.time.Duration d = java.time.Duration.between(startTime, currentTime);
        double totalDurationHours = d.toMinutes() / 60.0;

        return new SegmentResult(points, totalDist, totalDurationHours, currentTime);
    }

    // Méthode helper pour calculer la vitesse fond (SOG)
    private double calculateSog(WeatherService.WeatherData weather, double heading) {
        // Valeurs par défaut si météo manquante
        Integer wind = (weather != null && weather.windKnots() != null) ? weather.windKnots() : 5;
        Integer windDirDeg = (weather != null && weather.windDirDeg() != null) ? weather.windDirDeg() : 0;
        
        Double wave = (weather != null) ? weather.waveHeight() : 0.0;
        Integer waveDirDeg = (weather != null && weather.waveDirDeg() != null) ? weather.waveDirDeg() : 0;
        
        Double currentKnots = (weather != null) ? weather.currentKnots() : 0.0;
        Integer currentDirDeg = (weather != null && weather.currentDirDeg() != null) ? weather.currentDirDeg() : 0;

        // 1. Vitesse Surface (STW)
        double stw = performanceService.calculateSpeed(wind, windDirDeg, heading, wave, waveDirDeg);

        // 2. Impact Courant
        double sog = stw;
        if (currentKnots > 0) {
            double currentDir = currentDirDeg.doubleValue();
            double angleDiff = Math.toRadians(currentDir - heading);
            double currentImpact = currentKnots * Math.cos(angleDiff);
            sog = stw + currentImpact;
        }

        // Sécurité
        return Math.max(0.5, sog);
    }

    private RouteForecast.RoutePoint createRoutePoint(double lat, double lon, LocalDateTime time, 
                                                      WeatherService.WeatherData weather, 
                                                      Double boatSpeed, Double heading) {
        RouteForecast.RoutePoint p = new RouteForecast.RoutePoint();
        p.setLatitude(lat);
        p.setLongitude(lon);
        p.setEstimatedTime(time);
        p.setBoatSpeedKnots(boatSpeed);
        p.setHeading(heading);
        
        if (weather != null) {
            p.setWindSpeedKnots(weather.windKnots());
            p.setWindDirection(weather.windDirStr());
            p.setWindDirectionDeg(weather.windDirDeg());
            p.setWeatherSummary(weather.summary());
            
            p.setCurrentSpeedKnots(weather.currentKnots());
            p.setCurrentDirection(weather.currentDirStr());
            p.setCurrentDirectionDeg(weather.currentDirDeg());
            
            p.setWaveHeight(weather.waveHeight());
            p.setWaveDirection(weather.waveDirStr());
            p.setWaveDirectionDeg(weather.waveDirDeg());
        }
        return p;
    }

    private record SegmentResult(List<RouteForecast.RoutePoint> points, double distance, double duration, LocalDateTime endTime) {}

    private double calculateDistance(Waypoint p1, Waypoint p2) {
        double R = 3440.1; 
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private double calculateBearing(Waypoint p1, Waypoint p2) {
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());
        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360) % 360;
    }
}