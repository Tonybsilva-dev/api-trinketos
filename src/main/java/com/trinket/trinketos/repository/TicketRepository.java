package com.trinket.trinketos.repository;

import com.trinket.trinketos.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
  List<Ticket> findByOrganizationId(UUID organizationId);

  List<Ticket> findByCustomerId(UUID customerId);

  List<Ticket> findByAgentId(UUID agentId);
}
