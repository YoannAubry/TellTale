package com.yoann.telltale.repository;

import com.yoann.telltale.model.Waypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaypointRepository extends JpaRepository<Waypoint, Long> {
}