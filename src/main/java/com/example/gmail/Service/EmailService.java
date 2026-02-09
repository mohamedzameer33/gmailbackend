package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateRepository templateRepository;

    // --- SAVE TEMPLATE ---
    public EmailTemplate saveTemplateWithFile(String name, String subject, String body, MultipartFile file) throws IOException {
        EmailTemplate template = new EmailTemplate();
        template.setTemplateName(name);
        template.setSubject(subject);
        template.setBody(body);

        if (file != null && !file.isEmpty()) {
            template.setFileName(file.getOriginalFilename());
            template.setFileData(file.getBytes());
            template.setFileType(file.getContentType());
        }

        return templateRepository.save(template);
    }

    // --- UPDATE TEMPLATE ---
    @Transactional
    public EmailTemplate updateTemplate(Long id, String name, String subject, String body, MultipartFile file) throws Exception {
        EmailTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new Exception("Template not found: " + id));

        existing.setTemplateName(name);
        existing.setSubject(subject);
        existing.setBody(body);

        if (file != null && !file.isEmpty()) {
            existing.setFileName(file.getOriginalFilename());
            existing.setFileData(file.getBytes());
            existing.setFileType(file.getContentType());
        }

        return templateRepository.save(existing);
    }

    // --- DELETE TEMPLATE ---
    public void deleteTemplate(Long id) throws Exception {
        if (!templateRepository.existsById(id)) throw new Exception("Template does not exist: " + id);
        templateRepository.deleteById(id);
    }

    // --- SEND EMAIL ---
    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile manualFile, Long templateId) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML enabled

            // Attachment priority: manual file > DB template file
            if (manualFile != null && !manualFile.isEmpty()) {
                helper.addAttachment(manualFile.getOriginalFilename(), manualFile);
            } else if (templateId != null) {
                templateRepository.findById(templateId).ifPresent(temp -> {
                    if (temp.getFileData() != null) {
                        try {
                            helper.addAttachment(temp.getFileName(), new ByteArrayResource(temp.getFileData()));
                        } catch (MessagingException e) {
                            throw new RuntimeException("Attachment failed: " + e.getMessage());
                        }
                    }
                });
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MessagingException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
