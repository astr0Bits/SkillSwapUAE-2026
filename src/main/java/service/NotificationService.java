// service/NotificationService.java
package service;

import dto.NotificationDTO;
import model.Notification;
import model.User;
import repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public List<NotificationDTO> getUserNotifications(User user) {
        return repository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addNotification(User user, String message) {
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setMessage(message);
        notif.setRead(false);
        repository.save(notif);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notif = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notif.getUser().getId().equals(user.getId()))
            throw new SecurityException("Not your notification");
        notif.setRead(true);
        repository.save(notif);
    }

    @Transactional
    public void clearAll(User user) {
        repository.deleteByUser(user);
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setRead(n.isRead());
        return dto;
    }
}