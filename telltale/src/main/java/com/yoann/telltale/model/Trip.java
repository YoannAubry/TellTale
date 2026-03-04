package com.yoann.telltale.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Qui et Quand ---
    
    @ManyToOne(optional = false) // Une sortie appartient forcément à un bateau
    @JoinColumn(name = "boat_id")
    private Boat boat;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // --- Le Trajet ---

    @ManyToOne
    @JoinColumn(name = "departure_id")
    private Waypoint departure;

    @ManyToOne
    @JoinColumn(name = "arrival_id")
    private Waypoint arrival;

    private Double distanceNm;   // Distance en milles nautiques
    private Double engineHours;  // Heures moteur (ex: 1.5 pour 1h30)

    // --- La Météo (Observée ou Auto-remplie) ---

    private Integer windSpeedKnots; // Force du vent
    private String windDirection;   // Direction (N, NW, SE...)
    private String seaState;        // État de la mer (Calme, Peu agitée...)
    private String weatherSummary;  // "Grand soleil", "Pluie fine"

    // --- Le Récit ---
    
    @Column(length = 2000)
    private String comments;      // "Vu des dauphins, super nav sous spi..."
}