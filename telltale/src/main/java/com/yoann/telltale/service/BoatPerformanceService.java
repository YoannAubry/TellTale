package com.yoann.telltale.service;

import org.springframework.stereotype.Service;

@Service
public class BoatPerformanceService {

    /**
     * Calcule la vitesse estimée du Kelt 8.50
     * @param windKnots Vitesse du vent (nœuds)
     * @param windDirectionDirection du vent (0-360°)
     * @param boatHeading Cap du bateau (0-360°)
     * @param waveHeight Hauteur des vagues (mètres)
     * @return Vitesse bateau en Nœuds
     */
    public double calculateSpeed(Integer windKnots, String windDirectionStr, double boatHeading, Double waveHeight, String waveDirectionStr) {
        // 1. Sécurité : Si pas de météo, on rend une vitesse moyenne par défaut (moteur)
        if (windKnots == null || windDirectionStr == null) return 5.0;

        double windDir = convertCardinalToDegrees(windDirectionStr);
        
        // 2. Calcul de l'Angle au Vent Réel (TWA - True Wind Angle)
        double angleDiff = Math.abs(windDir - boatHeading);
        if (angleDiff > 180) angleDiff = 360 - angleDiff; // On ramène entre 0 et 180 (Babord/Tribord indifférent)

        // 3. Vitesse théorique (Modèle simplifié Kelt 8.50)
        double baseSpeed = 0.0;

        // --- Zone "No Go" (Face au vent < 45°) ---
        if (angleDiff < 45) {
            // On suppose qu'on tire des bords (Vmg) -> On avance moins vite vers la cible
            // Ou on met le moteur
            baseSpeed = 3.0 + (windKnots * 0.1); 
        } 
        // --- Près Bon Plein à Travers (45° - 90°) ---
        else if (angleDiff <= 90) {
            // C'est là que le bateau accélère le plus
            double ratio = (angleDiff - 45) / 45.0; // 0 à 1
            baseSpeed = (windKnots * 0.4) + (ratio * 1.5); 
        }
        // --- Portant (90° - 180°) ---
        else {
            // Au portant, on va vite mais attention au vent arrière pur
            baseSpeed = (windKnots * 0.5); 
        }

        // 4. Bornes de vitesse (Le Kelt ne plane pas à 20 noeuds !)
        double maxHullSpeed = 7.5; // Vitesse de coque max environ
        if (baseSpeed > maxHullSpeed) baseSpeed = maxHullSpeed;
        if (baseSpeed < 1.0) baseSpeed = 1.0; // Pétole -> Moteur ou dérive

        // 5. Impact Vagues Directionnel (Modèle "Doux")
        if (waveHeight != null && waveHeight > 0.5 && waveDirectionStr != null) {
            double waveDir = convertCardinalToDegrees(waveDirectionStr);
            double waveAngle = Math.abs(waveDir - boatHeading);
            if (waveAngle > 180) waveAngle = 360 - waveAngle; // Angle 0-180 (Symétrie bâbord/tribord)

            double penalty = 0.0;

            // --- Mer de Face (0° - 60°) ---
            if (waveAngle < 60) {
                // Formule linéaire douce : -8% par mètre au-delà de 0.5m
                // Ex: 1.5m -> (1.5 - 0.5) * 0.08 = 0.08 (8% de perte)
                penalty = (waveHeight - 0.5) * 0.08;
            }
            // --- Mer de Travers (60° - 120°) ---
            else if (waveAngle < 120) {
                // Roulis modéré : -4% par mètre au-delà de 1m
                if (waveHeight > 1.0) {
                    penalty = (waveHeight - 1.0) * 0.04;
                }
            }
            // --- Mer de l'Arrière (120° - 180°) ---
            else {
                // Neutre jusqu'à 2m.
                // Au-delà, prudence (-5% fixe) pour éviter le lof.
                if (waveHeight > 2.0) {
                    penalty = 0.05;
                } else {
                    penalty = 0.0;
                }
            }

            // Plafond de sécurité (on ne s'arrête jamais complètement à cause des vagues dans ce modèle)
            if (penalty > 0.4) penalty = 0.4; // Max 40% de perte
            if (penalty < 0.0) penalty = 0.0; // Pas de bonus

            baseSpeed *= (1.0 - penalty);
        }

        return Math.round(baseSpeed * 10.0) / 10.0; // Arrondi 1 décimale
    }

    // Convertit "NW" en 315°
    private double convertCardinalToDegrees(String dir) {
        switch (dir.toUpperCase()) {
            case "N": return 0;
            case "NE": return 45;
            case "E": return 90;
            case "SE": return 135;
            case "S": return 180;
            case "SW": return 225;
            case "W": return 270;
            case "NW": return 315;
            default: return 0;
        }
    }
}