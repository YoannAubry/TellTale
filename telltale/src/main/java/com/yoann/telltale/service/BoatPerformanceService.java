package com.yoann.telltale.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BoatPerformanceService {

    private record PolarPoint(int twa, int tws, double speed) {}

    private final List<PolarPoint> polarData = new ArrayList<>();

    @PostConstruct
    public void loadPolars() {
        try {
            ClassPathResource resource = new ClassPathResource("polars.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int twa = Integer.parseInt(parts[0].trim());
                    int tws = Integer.parseInt(parts[1].trim());
                    double speed = Double.parseDouble(parts[2].trim());
                    polarData.add(new PolarPoint(twa, tws, speed));
                }
            }
            System.out.println("Polaires chargées : " + polarData.size() + " points.");
        } catch (Exception e) {
            System.err.println("Erreur chargement polaires : " + e.getMessage());
        }
    }

    public double calculateSpeed(Integer windKnots, Integer windDirDeg, double boatHeading, Double waveHeight, Integer waveDirDeg) {
        if (windKnots == null || windDirDeg == null) return 5.0;
        if (windKnots < 2) return 1.0;

        // Calcul TWA
        double twa = Math.abs(windDirDeg - boatHeading);
        if (twa > 180) twa = 360 - twa;

        // Vitesse Polaire
        double baseSpeed = getPolarSpeed(twa, windKnots);

        // Impact Vagues (Modèle "Doux")
        if (waveHeight != null && waveHeight > 0.5 && waveDirDeg != null) {
            double waveAngle = Math.abs(waveDirDeg - boatHeading);
            if (waveAngle > 180) waveAngle = 360 - waveAngle;

            double penalty = 0.0;

            if (waveAngle < 60) {
                penalty = (waveHeight - 0.5) * 0.08;
            } else if (waveAngle < 120) {
                if (waveHeight > 1.0) penalty = (waveHeight - 1.0) * 0.04;
            } else {
                if (waveHeight > 2.0) penalty = 0.05;
            }

            if (penalty > 0.4) penalty = 0.4;
            if (penalty < 0.0) penalty = 0.0;

            baseSpeed *= (1.0 - penalty);
        }

        return Math.round(baseSpeed * 10.0) / 10.0;
    }

    private double getPolarSpeed(double targetTwa, int targetTws) {
        return polarData.stream()
                .min(Comparator.comparingDouble(p -> 
                    Math.pow(p.twa - targetTwa, 2) + Math.pow(p.tws - targetTws, 2)
                ))
                .map(PolarPoint::speed)
                .orElse(5.0);
    }
}