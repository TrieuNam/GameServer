package com.SouthMillion.activity_service.repository;

import java.util.Optional;

public interface RoleIdLookupRepository<T, ID> {
    Optional<T> findByRoleId(Long roleId);
}