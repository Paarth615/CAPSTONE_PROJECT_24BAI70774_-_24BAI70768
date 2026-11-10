package com.lifelink.repository;

import com.lifelink.model.Notification;
import com.lifelink.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndSeenFalse(User user);

    long countByUserAndSeenFalse(User user);
}
