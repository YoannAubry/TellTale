package com.yoann.telltale.controller;

import com.yoann.telltale.dto.RouteForecast;
import com.yoann.telltale.model.Waypoint;
import com.yoann.telltale.repository.WaypointRepository;
import com.yoann.telltale.service.RoutingService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;
    private final WaypointRepository waypointRepository;

    public RoutingController(RoutingService routingService, WaypointRepository waypointRepository) {
        this.routingService = routingService;
        this.waypointRepository = waypointRepository;
    }

    // GET /api/routing?startId=1&endId=2&departureTime=2026-03-05T10:00
    @GetMapping
    public RouteForecast getRoute(
            @RequestParam Long startId, 
            @RequestParam Long endId,
            @RequestParam(required = false) List<Long> stepIds,
            @RequestParam(defaultValue = "5.0") Double speed, // (Sera ignoré si les polaires marchent, mais gardons-le en fallback)
            @RequestParam(required = false) String departureTime // <-- NOUVEAU
    ) {
        
        Waypoint start = waypointRepository.findById(startId)
                .orElseThrow(() -> new RuntimeException("Départ introuvable"));
        Waypoint end = waypointRepository.findById(endId)
                .orElseThrow(() -> new RuntimeException("Arrivée introuvable"));

        // ...
        List<Waypoint> steps = new ArrayList<>();
        if (stepIds != null && !stepIds.isEmpty()) {
            // 1. On récupère tout en vrac
            List<Waypoint> unorderedSteps = waypointRepository.findAllById(stepIds);
            
            // 2. On recrée la liste dans le bon ordre
            for (Long id : stepIds) {
                unorderedSteps.stream()
                    .filter(wp -> wp.getId().equals(id))
                    .findFirst()
                    .ifPresent(steps::add);
            }
        }

        // Gestion de la date de départ
        LocalDateTime startDateTime;
        if (departureTime != null) {
            // On parse la date reçue (format ISO 8601 : YYYY-MM-DDTHH:MM)
            startDateTime = LocalDateTime.parse(departureTime);
        } else {
            // Par défaut : maintenant
            startDateTime = LocalDateTime.now();
        }

        return routingService.calculateRoute(start, end, steps, startDateTime, speed);
    }
}