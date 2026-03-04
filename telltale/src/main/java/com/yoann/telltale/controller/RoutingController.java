package com.yoann.telltale.controller;

import com.yoann.telltale.dto.RouteForecast;
import com.yoann.telltale.model.Waypoint;
import com.yoann.telltale.repository.WaypointRepository;
import com.yoann.telltale.service.RoutingService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;
    private final WaypointRepository waypointRepository;

    public RoutingController(RoutingService routingService, WaypointRepository waypointRepository) {
        this.routingService = routingService;
        this.waypointRepository = waypointRepository;
    }

    // GET /api/routing?startId=1&endId=2&speed=5.0
    @GetMapping
    public RouteForecast getRoute(
            @RequestParam Long startId, 
            @RequestParam Long endId, 
            @RequestParam(defaultValue = "5.0") Double speed) {
        
        Waypoint start = waypointRepository.findById(startId)
                .orElseThrow(() -> new RuntimeException("Départ introuvable"));
        Waypoint end = waypointRepository.findById(endId)
                .orElseThrow(() -> new RuntimeException("Arrivée introuvable"));

        // On part "maintenant" pour le test (ou on pourrait passer une date en paramètre)
        LocalDateTime departureTime = LocalDateTime.now(); 

        return routingService.calculateRoute(start, end, departureTime, speed);
    }
}