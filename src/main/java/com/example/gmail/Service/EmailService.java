package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import com.sendgrid.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private TemplateRepository templateRepository;

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${SENDGRID_SENDER_EMAIL}") // Verified sender in SendGrid
    private String senderEmail;

    // --- SAVE NEW TEMPLATE ---
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

    // --- SEND EMAIL WITH ATTACHMENTS ---
    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile manualFile, Long templateId) throws IOException {
        Mail mail = new Mail();
        Email from = new Email(senderEmail);
        mail.setFrom(from);
        mail.setSubject(subject);

        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        mail.addContent(content);
        Personalization personalization = new Personalization();
        personalization.addTo(toEmail);
        mail.addPersonalization(personalization);

        // 1️⃣ Manual file attachment
        if (manualFile != null && !manualFile.isEmpty()) {
            Attachments attachment = new Attachments();
            attachment.setContent(java.util.Base64.getEncoder().encodeToString(manualFile.getBytes()));
            attachment.setType(manualFile.getContentType());
            attachment.setFilename(manualFile.getOriginalFilename());
            mail.addAttachments(attachment);
        }

        // 2️⃣ File from DB template
        else if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(temp -> {
                if (temp.getFileData() != null) {
                    Attachments attachment = new Attachments();
                    attachment.setContent(java.util.Base64.getEncoder().encodeToString(temp.getFileData()));
                    attachment.setType(temp.getFileType());
                    attachment.setFilename(temp.getFileName());
                    mail.addAttachments(attachment);
                }
            });
        }

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println("SendGrid Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());
        } catch (IOException ex) {
            throw ex;
        }
    }
}
