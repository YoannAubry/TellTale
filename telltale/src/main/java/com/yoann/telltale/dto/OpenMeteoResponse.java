package com.yoann.telltale.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OpenMeteoResponse {
    private Hourly hourly;

    @Data
    public static class Hourly {
        private List<String> time;
        @JsonProperty("temperature_2m") private List<Double> temperature2m;
        @JsonProperty("weathercode") private List<Integer> weathercode;
        @JsonProperty("windspeed_10m") private List<Double> windspeed10m;
        @JsonProperty("winddirection_10m") private List<Integer> winddirection10m;
    }
}