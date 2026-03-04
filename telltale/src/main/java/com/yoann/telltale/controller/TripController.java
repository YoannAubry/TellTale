package com.yoann.telltale.controller;

import com.yoann.telltale.model.Trip;
import com.yoann.telltale.model.Boat;
import com.yoann.telltale.model.Waypoint;
import com.yoann.telltale.repository.TripRepository;
import com.yoann.telltale.repository.BoatRepository;
import com.yoann.telltale.repository.WaypointRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;
    private final BoatRepository boatRepository;
    private final WaypointRepository waypointRepository;

    public TripController(TripRepository tripRepository, 
                          BoatRepository boatRepository, 
                          WaypointRepository waypointRepository) {
        this.tripRepository = tripRepository;
        this.boatRepository = boatRepository;
        this.waypointRepository = waypointRepository;
    }

    @GetMapping
    public List<Trip> getAll() {
        return tripRepository.findAll();
    }

    @PostMapping
    public Trip create(@RequestBody Trip trip) {
        // On vérifie que le bateau existe vraiment
        if (trip.getBoat() != null && trip.getBoat().getId() != null) {
            Boat boat = boatRepository.findById(trip.getBoat().getId())
                .orElseThrow(() -> new RuntimeException("Bateau introuvable"));
            trip.setBoat(boat);
        }

        // On vérifie le départ
        if (trip.getDeparture() != null && trip.getDeparture().getId() != null) {
            Waypoint dep = waypointRepository.findById(trip.getDeparture().getId()).orElse(null);
            trip.setDeparture(dep);
        }

        // On vérifie l'arrivée
        if (trip.getArrival() != null && trip.getArrival().getId() != null) {
            Waypoint arr = waypointRepository.findById(trip.getArrival().getId()).orElse(null);
            trip.setArrival(arr);
        }

        // Si pas de date de début, on met "maintenant"
        if (trip.getStartDateTime() == null) {
            trip.setStartDateTime(LocalDateTime.now());
        }

        return tripRepository.save(trip);
    }
}