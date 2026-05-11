package com.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    List<SessionEntity> findAllByOrderByUpdatedAtDesc();
}
