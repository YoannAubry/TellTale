package com.yoann.telltale.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Génère Getters, Setters, ToString, etc.
@Entity
@NoArgsConstructor // Constructeur vide obligatoire pour JPA
@AllArgsConstructor // Constructeur avec tous les arguments (pratique)
public class Waypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // Nom obligatoire
    private String name;

    @Column(nullable = false) // Lat obligatoire
    private Double latitude;

    @Column(nullable = false) // Lon obligatoire
    private Double longitude;

    @Enumerated(EnumType.STRING) // Stocke "PORT" en toutes lettres en BDD
    @Column(nullable = false)
    private WaypointType type;

    // --- Champs Optionnels ---

    private Integer vhfChannel; // Nullable (Integer objet, pas int primitif)

    @Column(length = 1000) // Texte un peu long autorisé
    private String notes;

    // Stockera les secteurs protégés (ex: "N,NE")
    // On pourrait faire plus complexe (EnumSet), mais String simple suffit pour débuter
    private String protectedFrom; 
}