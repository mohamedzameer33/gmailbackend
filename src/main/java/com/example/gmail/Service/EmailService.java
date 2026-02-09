package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
@Service
public class EmailService {

    @Autowired private JavaMailSender mailSender;
    @Autowired private TemplateRepository templateRepository;

    // --- SAVE NEW TEMPLATE ---
    public EmailTemplate saveTemplateWithFile(String name, String subject, String body, MultipartFile file) throws IOException {
        EmailTemplate template = new EmailTemplate();
        template.setTemplateName(name);
        template.setSubject(subject);
        template.setBody(body);

        if (file != null && !file.isEmpty()) {
            // Consistent with update logic: Store in DB
            template.setFileName(file.getOriginalFilename());
            template.setFileData(file.getBytes());
            template.setFileType(file.getContentType());
            // Optional: if you still want the disk path, keep your existing logic here
        }

        return templateRepository.save(template);
    }

    // --- UPDATE EXISTING TEMPLATE ---
    @Transactional
    public EmailTemplate updateTemplate(Long id, String name, String subject, String body, MultipartFile file) throws Exception {
        EmailTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new Exception("Template not found with ID: " + id));

        existingTemplate.setTemplateName(name);
        existingTemplate.setSubject(subject);
        existingTemplate.setBody(body);

        if (file != null && !file.isEmpty()) {
            existingTemplate.setFileName(file.getOriginalFilename());
            existingTemplate.setFileData(file.getBytes());
            existingTemplate.setFileType(file.getContentType());
        }

        return templateRepository.save(existingTemplate);
    }

    // --- DELETE TEMPLATE ---
    public void deleteTemplate(Long id) throws Exception {
        if (!templateRepository.existsById(id)) {
            throw new Exception("Cannot delete: Template " + id + " does not exist.");
        }
        templateRepository.deleteById(id);
    }

    // --- SEND EMAIL (Updated to support DB files) ---
    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile manualFile, Long templateId) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        // 1. Priority: Manual Upload from the UI
        if (manualFile != null && !manualFile.isEmpty()) {
            helper.addAttachment(manualFile.getOriginalFilename(), manualFile);
        }
        // 2. Secondary: Get file from DB Template
        else if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(temp -> {
                if (temp.getFileData() != null) {
                    try {
                        helper.addAttachment(temp.getFileName(), new org.springframework.core.io.ByteArrayResource(temp.getFileData()));
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        mailSender.send(message);
    }
}