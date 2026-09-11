package com.solidv.chronos.service;

import com.solidv.chronos.dto.EmailPayload;
import com.solidv.chronos.entity.DelayedTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTaskExecutor implements TaskExecutor {

    private final JavaMailSender mailSender;
    private final JsonMapper objectMapper;

    @Override
    public boolean execute(DelayedTask task) {
        try {
            String payload = task.getPayload();
            if (payload == null || payload.isBlank()) {
                log.error("Task {} payload is empty or null", task.getId());
                return false;
            }

            EmailPayload emailPayload = objectMapper.readValue(payload, EmailPayload.class);
            if (emailPayload == null
                    || emailPayload.to() == null || emailPayload.to().isBlank()
                    || emailPayload.subject() == null || emailPayload.subject().isBlank()
                    || emailPayload.body() == null || emailPayload.body().isBlank()) {
                log.error("Task {} email payload must contain non-blank to, subject, and body", task.getId());
                return false;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailPayload.to().trim());
            message.setSubject(emailPayload.subject().trim());
            message.setText(emailPayload.body().trim());

            mailSender.send(message);
            log.info("Successfully sent email task ID {} to {}", task.getId(), emailPayload.to());
            return true;
        } catch (JacksonException e) {
            log.error("Failed to parse JSON email payload for task ID {}", task.getId(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to execute email task ID {}: {}", task.getId(), e.getMessage(), e);
            return false;
        }
    }
}
