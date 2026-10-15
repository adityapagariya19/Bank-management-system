package com.meridian.service;

import com.meridian.model.Notification;
import com.meridian.model.User;
import com.meridian.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void push(User user, String title, String message, String type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message.length() > 300 ? message.substring(0, 300) : message);
        n.setType(type);
        repo.save(n);
    }
}
