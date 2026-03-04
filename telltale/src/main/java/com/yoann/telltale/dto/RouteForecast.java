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
    
    private List<RoutePoint> points;

    @Data
    public static class RoutePoint {
        private Double latitude;
        private Double longitude;
        private LocalDateTime estimatedTime;
        
        private Integer windSpeedKnots;
        private String windDirection;
        private Integer windDirectionDeg;
        private String weatherSummary;
        
        private Double currentSpeedKnots;
        private String currentDirection;
        private Integer currentDirectionDeg;
        
        private Double waveHeight;
        private String waveDirection;
        private Integer waveDirectionDeg;
        
        private Double boatSpeedKnots;
        private Double heading;
    }
}