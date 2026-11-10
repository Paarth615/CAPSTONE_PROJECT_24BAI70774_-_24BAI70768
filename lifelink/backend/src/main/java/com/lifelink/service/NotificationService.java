package com.lifelink.service;

import com.lifelink.dto.MiscDtos.NotificationDto;
import com.lifelink.dto.MiscDtos.NotificationsPage;
import com.lifelink.exception.ApiException;
import com.lifelink.model.Notification;
import com.lifelink.model.User;
import com.lifelink.repository.NotificationRepository;
import com.lifelink.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;

    public NotificationService(NotificationRepository notifications, CurrentUser currentUser, DtoMapper mapper) {
        this.notifications = notifications;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    /** Creates an in-app notification for {@code user}. */
    public void notify(User user, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        n.setLink(link);
        notifications.save(n);
    }

    @Transactional(readOnly = true)
    public NotificationsPage mine() {
        User user = currentUser.get();
        List<NotificationDto> items = notifications
                .findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 50))
                .stream().map(mapper::toNotificationDto).toList();
        return new NotificationsPage(notifications.countByUserAndSeenFalse(user), items);
    }

    public void markRead(Long id) {
        User user = currentUser.get();
        Notification n = notifications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("That notification belongs to someone else");
        }
        n.setSeen(true);
        notifications.save(n);
    }

    public void markAllRead() {
        User user = currentUser.get();
        List<Notification> unread = notifications.findByUserAndSeenFalse(user);
        unread.forEach(n -> n.setSeen(true));
        notifications.saveAll(unread);
    }
}
