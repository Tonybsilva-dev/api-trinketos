package com.trinket.trinketos.dto;

import com.trinket.trinketos.model.Role;
import java.util.UUID;

public record RegisterRequest(
    String name,
    String email,
    String password,
    Role role,
    UUID organizationId // In some flows admin might set this implicit from their own, but for super
                        // admin maybe explicit?
                        // User says: "Apenas o ADMIN de uma organização pode cadastrar... garantindo
                        // organization_id igual".
                        // So the Admin sends name, email, password, role. Id is implicit.
                        // I will keep it flexible or make it optional in DTO, but for the endpoint
                        // logic I will enforce matching context.
) {
}
