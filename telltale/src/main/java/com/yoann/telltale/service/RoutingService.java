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

    /**
     * Calcule la route complète avec étapes intermédiaires
     */
    public RouteForecast calculateRoute(Waypoint start, Waypoint end, List<Waypoint> steps, LocalDateTime departureTime, double avgSpeedKnots) {
        RouteForecast forecast = new RouteForecast();
        forecast.setDepartureName(start.getName());
        forecast.setArrivalName(end.getName());
        forecast.setStartDateTime(departureTime);

        // 1. Construire la liste ordonnée de tous les points de passage
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

        // 2. Boucler sur chaque segment (Point A -> Point B)
        for (int i = 0; i < fullPath.size() - 1; i++) {
            Waypoint p1 = fullPath.get(i);
            Waypoint p2 = fullPath.get(i + 1);

            // Calcul du segment
            SegmentResult segment = calculateSegment(p1, p2, currentSegmentStartTime, avgSpeedKnots);

            // Ajouter les résultats
            totalDistance += segment.distance;
            totalDuration += segment.duration;
            
            // On ajoute les points du segment à la liste globale
            if (i > 0 && !segment.points.isEmpty()) {
                // Si ce n'est pas le premier segment, on ignore le premier point 
                // (car c'est le même que le dernier du segment d'avant)
                allPoints.addAll(segment.points.subList(1, segment.points.size()));
            } else {
                // Premier segment : on prend tout
                allPoints.addAll(segment.points);
            }

            // Mise à jour de l'heure pour le prochain segment
            currentSegmentStartTime = segment.endTime;
        }

        forecast.setTotalDistanceNm(totalDistance);
        forecast.setEstimatedDurationHours(totalDuration);
        forecast.setPoints(allPoints);

        return forecast;
    }

    private SegmentResult calculateSegment(Waypoint start, Waypoint end, LocalDateTime startTime, double defaultSpeed) {
        double totalDist = calculateDistance(start, end);
        double heading = calculateBearing(start, end); // Cap géographique (Route fond)

        List<RouteForecast.RoutePoint> points = new ArrayList<>();
        
        double remainingDist = totalDist;
        double currentLat = start.getLatitude();
        double currentLon = start.getLongitude();
        LocalDateTime currentTime = startTime;
        
        // 1. Initialisation : Météo au point de départ
        var weather = weatherService.getPointWeather(currentLat, currentLon, currentTime);
        
        // Ajout du point de départ (Vitesse 0.0 car on est à l'arrêt)
        points.add(createRoutePoint(currentLat, currentLon, currentTime, weather, 0.0));

        // Pas de temps de simulation : 30 minutes
        double timeStepHours = 0.5; 

        while (remainingDist > 0.1) { // Tant qu'il reste plus de 0.1 mille
            // --- A. Récupération des conditions ---
            // On utilise la météo du dernier point connu pour calculer ce tronçon
            Integer wind = (weather != null && weather.windKnots() != null) ? weather.windKnots() : 5;
            String windDir = (weather != null) ? weather.windDirStr() : "N";
            
            Double wave = (weather != null) ? weather.waveHeight() : 0.0;
            String waveDir = (weather != null) ? weather.waveDirStr() : null;
            
            Double currentKnots = (weather != null) ? weather.currentKnots() : 0.0;
            String currentDirStr = (weather != null) ? weather.currentDirStr() : null;

            // --- B. Calcul Vitesse Surface (STW) ---
            // Utilise les Polaires + Impact des vagues
            double stw = performanceService.calculateSpeed(wind, windDir, heading, wave, waveDir);

            // --- C. Calcul Vitesse Fond (SOG) avec Courant ---
            double sog = stw;
            
            if (currentKnots != null && currentKnots > 0 && currentDirStr != null) {
                double currentDir = convertCardinalToDegrees(currentDirStr);
                
                // Angle entre la route du bateau et le courant
                double angleDiff = Math.toRadians(currentDir - heading);
                
                // Projection : Si le courant pousse (angle < 90), on accélère. Sinon on freine.
                double currentImpact = currentKnots * Math.cos(angleDiff);
                
                sog = stw + currentImpact;
            }

            // Sécurité : Vitesse minimale (0.5 nds) pour ne pas reculer ou faire une division par zéro
            if (sog < 0.5) sog = 0.5;

            // --- D. Avancement ---
            double distStep = sog * timeStepHours; // Distance faite en 30 min

            // Si on dépasse l'arrivée, on ajuste le temps pour tomber pile dessus
            if (distStep > remainingDist) {
                distStep = remainingDist;
                timeStepHours = distStep / sog; // Temps exact restant
            }

            // Mise à jour de la position (Interpolation linéaire sur le segment)
            double totalRatio = 1.0 - ((remainingDist - distStep) / totalDist);
            double newLat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * totalRatio;
            double newLon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * totalRatio;
            
            // Mise à jour du temps
            currentTime = currentTime.plusMinutes((long)(timeStepHours * 60));
            remainingDist -= distStep;

            // --- E. Enregistrement du nouveau point ---
            // On récupère la météo pour ce NOUVEAU point (pour l'affichage et le prochain tour)
            weather = weatherService.getPointWeather(newLat, newLon, currentTime);
            
            // On ajoute le point avec la vitesse (SOG) qu'on vient de tenir
            points.add(createRoutePoint(newLat, newLon, currentTime, weather, sog));
        }

        // Calcul durée totale du segment
        java.time.Duration d = java.time.Duration.between(startTime, currentTime);
        double totalDurationHours = d.toMinutes() / 60.0;

        return new SegmentResult(points, totalDist, totalDurationHours, currentTime);
    }

    // --- Utilitaires ---

    private RouteForecast.RoutePoint createRoutePoint(double lat, double lon, LocalDateTime time, 
                                                      WeatherService.WeatherData weather, 
                                                      Double boatSpeed) { // Nouveau param
        RouteForecast.RoutePoint p = new RouteForecast.RoutePoint();
        p.setLatitude(lat);
        p.setLongitude(lon);
        p.setEstimatedTime(time);
        p.setBoatSpeedKnots(boatSpeed); // <-- On stocke la vitesse !
        
        if (weather != null) {
            p.setWindSpeedKnots(weather.windKnots());
            p.setWindDirection(weather.windDirStr());
            p.setWindDirectionDeg(weather.windDirDeg());
            p.setWeatherSummary(weather.summary());
            p.setCurrentSpeedKnots(weather.currentKnots());
            p.setCurrentDirection(weather.currentDirStr());
            p.setCurrentDirectionDeg(weather.currentDirDeg());
            
            // --- VAGUES ---
            p.setWaveHeight(weather.waveHeight());
            p.setWaveDirection(weather.waveDirStr());
            p.setWaveDirectionDeg(weather.waveDirDeg());
        }
        return p;
    }

    // Duplication de la méthode de conversion (car l'autre est privée dans un autre service)
    // Idéalement à mettre dans une classe 'NavigationUtils' partagée
    private double convertCardinalToDegrees(String dir) {
        if (dir == null) return 0;
        switch (dir.toUpperCase()) {
            case "N": return 0; case "NE": return 45; case "E": return 90;
            case "SE": return 135; case "S": return 180; case "SW": return 225;
            case "W": return 270; case "NW": return 315; default: return 0;
        }
    }

     // Calcul du Cap (Bearing) entre 2 points
    private double calculateBearing(Waypoint p1, Waypoint p2) {
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());

        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360) % 360; // Normaliser 0-360
    }

    // Petit DTO interne pour passer les infos d'un segment
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
}