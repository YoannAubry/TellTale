package com.yoann.telltale.controller;

import com.yoann.telltale.model.Boat;
import com.yoann.telltale.model.Waypoint;
import com.yoann.telltale.repository.BoatRepository;
import com.yoann.telltale.repository.WaypointRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boats")
public class BoatController {

    private final BoatRepository repository;
    private final WaypointRepository waypointRepository;

    // Injection de dépendance : Spring nous donne le repository tout prêt
    public BoatController(BoatRepository repository, WaypointRepository waypointRepository) {
        this.repository = repository;
        this.waypointRepository = waypointRepository;
    }

    // GET /api/boats -> Renvoie la liste de tous les bateaux
    @GetMapping
    public List<Boat> getAllBoats() {
        return repository.findAll();
    }

    // POST /api/boats -> Crée un nouveau bateau
    @PostMapping
    public Boat createBoat(@RequestBody Boat boat) {
        return repository.save(boat);
    }

    // PUT /api/boats/{id} -> Met à jour un bateau existant
    @PutMapping("/{id}")
    public Boat updateBoat(@PathVariable Long id, @RequestBody Boat boatDetails) {
        return repository.findById(id)
                .map(boat -> {
                    boat.setName(boatDetails.getName());
                    boat.setModel(boatDetails.getModel());
                    boat.setLength(boatDetails.getLength());
                    boat.setYear(boatDetails.getYear());
                    if (boatDetails.getHomePort() != null && boatDetails.getHomePort().getId() != null) {
                        Waypoint realPort = waypointRepository.findById(boatDetails.getHomePort().getId())
                            .orElse(null);
                        boat.setHomePort(realPort);
                    }
                    return repository.save(boat);
                })
                .orElseThrow(() -> new RuntimeException("Bateau non trouvé avec l'id " + id));
    }
}
