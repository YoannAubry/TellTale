package com.yoann.telltale.controller;

import com.yoann.telltale.model.Waypoint;
import com.yoann.telltale.repository.WaypointRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waypoints")
public class WaypointController {

    private final WaypointRepository repository;

    public WaypointController(WaypointRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Waypoint> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public Waypoint create(@RequestBody Waypoint waypoint) {
        return repository.save(waypoint);
    }
}