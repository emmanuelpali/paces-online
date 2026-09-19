package com.pacesonline.runservice.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pacesonline.runservice.entity.Run;

public interface RunRepository extends JpaRepository<Run, UUID> {
    Optional<Run> findByIdAndUserId(UUID id, UUID userId);
}
