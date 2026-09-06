package com.solidv.chronos.service;

import com.solidv.chronos.entity.DelayedTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTaskExecutor implements TaskExecutor {

    private final JavaMailSender mailSender;

    @Override
    public boolean execute(DelayedTask task) {
        try {
            String payload = task.getPayload();
            if (payload == null || payload.isBlank()) {
                log.error("Task {} payload is empty or null", task.getId());
                return false;
            }

            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) {
                log.error("Task {} payload format invalid. Expected 'recipient|subject|body', got: '{}'", task.getId(), payload);
                return false;
            }

            String recipient = parts[0].trim();
            String subject = parts[1].trim();
            String body = parts[2].trim();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Successfully sent email task ID {} to {}", task.getId(), recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute email task ID {}: {}", task.getId(), e.getMessage(), e);
            return false;
        }
    }
}
