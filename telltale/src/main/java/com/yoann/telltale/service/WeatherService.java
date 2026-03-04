package com.yoann.telltale.service;

import com.yoann.telltale.dto.MarineWeatherResponse;
import com.yoann.telltale.dto.OpenMeteoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
public class WeatherService {

    private final RestClient weatherClient;
    private final RestClient marineClient;

    public WeatherService(RestClient.Builder builder) {
        // Client Météo (Vent, Ciel) - Gère Forecast et Archive
        this.weatherClient = builder.baseUrl("https://api.open-meteo.com/v1").build();
        // Client Marine (Vagues, Courant) - Gère Forecast et Archive
        this.marineClient = builder.baseUrl("https://marine-api.open-meteo.com/v1").build();
    }

    /**
     * Récupère TOUTE la météo (Vent, Ciel, Vagues, Courant) pour un point et une heure.
     */
    public WeatherData getPointWeather(double lat, double lon, LocalDateTime dateTime) {
        LocalDate today = LocalDate.now();
        String dateStr = dateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        int hourIndex = dateTime.getHour();

        // Si c'est dans le passé, on devrait changer l'URL de base pour "/archive"
        // (Pour simplifier ici, on assume que c'est du Forecast/Futur pour le routage).
        // Si tu veux gérer l'historique aussi, il faudra la logique de switch d'URL ici.
        String weatherPath = "/forecast";
        String marinePath = "/marine";

        // 1. Appel Météo (Vent + Ciel)
        CompletableFuture<WeatherData> weatherFuture = CompletableFuture.supplyAsync(() -> {
            try {
                OpenMeteoResponse response = weatherClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path(weatherPath)
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("hourly", "temperature_2m,weathercode,windspeed_10m,winddirection_10m")
                        .queryParam("start_date", dateStr)
                        .queryParam("end_date", dateStr)
                        .build())
                    .retrieve()
                    .body(OpenMeteoResponse.class);

                if (response != null && response.getHourly() != null) {
                    double windKmh = response.getHourly().getWindspeed10m().get(hourIndex);
                    int knots = (int) Math.round(windKmh * 0.54);
                    String dir = convertDegreesToCardinal(response.getHourly().getWinddirection10m().get(hourIndex));
                    String summary = decodeWeatherCode(response.getHourly().getWeathercode().get(hourIndex));
                    int windDeg = response.getHourly().getWinddirection10m().get(hourIndex);
                    String dirStr = convertDegreesToCardinal(windDeg);
                    // On retourne juste la partie Météo remplie
                    return new WeatherData(knots, dirStr, windDeg, summary, null, null, null, null, null, null);
                }
            } catch (Exception e) {
                System.err.println("Erreur Météo : " + e.getMessage());
            }
            return new WeatherData(null, null, null, null, null, null, null, null, null, null);
        });

        // 2. Appel Marine (Vagues + Courant)
        CompletableFuture<MarineData> marineFuture = CompletableFuture.supplyAsync(() -> {
            try {
                MarineWeatherResponse response = marineClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path(marinePath)
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("hourly", "wave_height,wave_direction,ocean_current_velocity,ocean_current_direction")
                        .queryParam("start_date", dateStr)
                        .queryParam("end_date", dateStr)
                        .build())
                    .retrieve()
                    .body(MarineWeatherResponse.class);

                if (response != null && response.getHourly() != null) {
                    // Vagues
                    Double waveHeight = response.getHourly().getWaveHeight().get(hourIndex);
                    String waveDir = convertDegreesToCardinal(response.getHourly().getWaveDirection().get(hourIndex).intValue());
                    Double waveDirDouble = response.getHourly().getWaveDirection().get(hourIndex);
                    int waveDeg = waveDirDouble.intValue();
                    String waveStr = convertDegreesToCardinal(waveDeg);
                    
                    // Courant (km/h -> nds)
                    Double currentKmh = response.getHourly().getCurrentSpeed().get(hourIndex);
                    Double currentKnots = currentKmh * 0.54;
                    String currentDir = convertDegreesToCardinal(response.getHourly().getCurrentDirection().get(hourIndex).intValue());
                    Double curDirDouble = response.getHourly().getCurrentDirection().get(hourIndex);
                    int curDeg = curDirDouble.intValue();
                    String curStr = convertDegreesToCardinal(curDeg);

                    return new MarineData(waveHeight, waveStr, waveDeg, currentKnots, curStr, curDeg);
                }
            } catch (Exception e) {
                System.err.println("Erreur Marine : " + e.getMessage());
            }
            return new MarineData(null, null, null, null, null, null);
        });

        // 3. Fusion des résultats
        try {
            WeatherData w = weatherFuture.get();
            MarineData m = marineFuture.get();
            
            return new WeatherData(
                w.windKnots, w.windDirStr, w.windDirDeg, w.summary,
                m.waveHeight, m.waveDirStr, m.waveDirDeg,
                m.currentKnots, m.currentDirStr, m.currentDirDeg
            );
        } catch (Exception e) {
            return new WeatherData(null, null, null, null, null, null, null, null, null, null);
        }
    }

    // DTO Interne pour tout transporter
    public record WeatherData(
        Integer windKnots, String windDirStr, Integer windDirDeg, // Ajout Deg
        String summary,
        Double waveHeight, String waveDirStr, Integer waveDirDeg, // Ajout Deg
        Double currentKnots, String currentDirStr, Integer currentDirDeg // Ajout Deg
    ) {}

    private record MarineData(
        Double waveHeight, String waveDirStr, Integer waveDirDeg,
        Double currentKnots, String currentDirStr, Integer currentDirDeg
    ) {}

    // --- Utilitaires ---
    private String convertDegreesToCardinal(Integer degrees) {
        if (degrees == null) return null;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int)Math.round(((double)degrees % 360) / 45)];
    }

    private String decodeWeatherCode(Integer code) {
        if (code == null) return "Inconnu";
        if (code == 0) return "Soleil";
        if (code <= 3) return "Nuageux";
        if (code <= 45) return "Brume";
        if (code <= 67) return "Pluie";
        if (code >= 95) return "Orage";
        return "Mauvais temps";
    }
}