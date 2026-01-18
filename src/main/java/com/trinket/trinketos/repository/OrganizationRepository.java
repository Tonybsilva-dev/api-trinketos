package com.trinket.trinketos.repository;

import com.trinket.trinketos.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
  Optional<Organization> findBySlug(String slug);
}
