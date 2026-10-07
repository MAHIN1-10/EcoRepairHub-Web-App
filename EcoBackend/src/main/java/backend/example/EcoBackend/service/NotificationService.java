package backend.example.EcoBackend.service;

import backend.example.EcoBackend.entity.Notification;
import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /** type is one of: pickup, certificate, reward, campaign, info (the UI picks the icon from it). */
    public void notify(UserData user, String type, String title, String message) {
        if (user == null) return;
        notificationRepository.save(new Notification(user, type, title, message));
    }
}
