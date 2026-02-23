package com.trinket.trinketos.repository;

import com.trinket.trinketos.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID>, JpaSpecificationExecutor<Team> {
  List<Team> findByOrganizationId(UUID organizationId);

  boolean existsBySlugAndOrganizationId(String slug, UUID organizationId);
}
