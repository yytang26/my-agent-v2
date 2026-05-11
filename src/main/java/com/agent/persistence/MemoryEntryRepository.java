package com.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, String> {

    Optional<MemoryEntry> findByKey(String key);

    List<MemoryEntry> findByKeyContainingIgnoreCase(String keyFragment);

    @Query("SELECT m FROM MemoryEntry m WHERE LOWER(m.key) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(m.value) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<MemoryEntry> searchByQuery(@Param("query") String query);

    List<MemoryEntry> findAllByOrderByLastAccessedAtDesc();
}
