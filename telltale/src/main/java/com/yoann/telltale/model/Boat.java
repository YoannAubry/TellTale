package com.yoann.telltale.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor // Génère public Boat() {}
@AllArgsConstructor // Génère public Boat(Long id, String name, ..., Integer year, ...)
public class Boat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String model;
    private Double length;
    private Integer year; // Bien en Integer

    @ManyToOne
    @JoinColumn(name = "home_port_id")
    private Waypoint homePort;
}