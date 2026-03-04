package com.yoann.telltale.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteForecast {
    private String departureName;
    private String arrivalName;
    private LocalDateTime startDateTime;
    private Double totalDistanceNm;
    private Double estimatedDurationHours;
    
    private List<RoutePoint> points; // Les étapes du trajet

    @Data
    public static class RoutePoint {
        private Double latitude;
        private Double longitude;
        private LocalDateTime estimatedTime; // Heure de passage prévue
        
        // Météo prévue à ce point et à cette heure
        private Integer windSpeedKnots;
        private String windDirection; // N, NE...
        private String weatherSummary; // "Pluie", "Soleil"
    }
}