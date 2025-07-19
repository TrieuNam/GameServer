package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.model.Role;
import com.SouthMillion.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // Trả về tất cả Role thuộc về một User
    List<Role> findByUser(User user);
}
