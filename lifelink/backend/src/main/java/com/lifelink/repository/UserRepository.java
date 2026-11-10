package com.lifelink.repository;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleAndEnabledTrueAndAvailableTrue(Role role);

    long countByRole(Role role);

    long countByRoleAndEnabledTrueAndAvailableTrue(Role role);

    @Query("select u.bloodGroup, count(u) from User u " +
            "where u.role = :role and u.enabled = true and u.available = true " +
            "and u.bloodGroup is not null group by u.bloodGroup")
    List<Object[]> countAvailableDonorsByGroup(@Param("role") Role role);

    @Query("select count(distinct lower(u.city)) from User u where u.city is not null and u.enabled = true")
    long countDistinctCities();

    List<User> findAllByOrderByCreatedAtDesc();

    long countByBloodGroup(BloodGroup bloodGroup);
}
