package com.trinket.trinketos.repository;

import com.trinket.trinketos.model.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmail(String email);

  List<User> findByOrganizationId(UUID organizationId);

  boolean existsByEmail(String email);
}
