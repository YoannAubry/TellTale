package com.yoann.telltale.repository;

import com.yoann.telltale.model.Boat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoatRepository extends JpaRepository<Boat, Long> {
    // C'est tout ! Spring Data JPA va implémenter tout le reste pour toi.
}
