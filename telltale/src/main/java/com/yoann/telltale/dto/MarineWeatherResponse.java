package com.yoann.telltale.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class MarineWeatherResponse {

    private Hourly hourly;

    @Data
    public static class Hourly {
        private List<String> time;
        
        // Vagues (Houle + Mer du vent)
        @JsonProperty("wave_height")
        private List<Double> waveHeight;
        
        @JsonProperty("wave_direction")
        private List<Double> waveDirection;
        
        // --- COURANTS ---
        @JsonProperty("ocean_current_velocity")
        private List<Double> currentSpeed; // En km/h par défaut
        
        @JsonProperty("ocean_current_direction")
        private List<Double> currentDirection; // Direction vers laquelle ça porte
    }
}