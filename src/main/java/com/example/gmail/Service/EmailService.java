package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.sendgrid.helpers.mail.objects.Attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class EmailService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private SendGrid sendGrid;

    // Save template with optional file
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

    // Update template
    public EmailTemplate updateTemplate(Long id, String name, String subject, String body, MultipartFile file) throws Exception {
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new Exception("Template not found with ID: " + id));

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

    // Delete template
    public void deleteTemplate(Long id) throws Exception {
        if (!templateRepository.existsById(id)) throw new Exception("Template not found");
        templateRepository.deleteById(id);
    }

    // Convert plain text body to HTML (preserves line breaks)
    private String convertToHtml(String body) {
        if (body == null) return "";
        return body.replace("\n", "<br>");
    }

    // Send email using templateId and optional uploaded file
    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile manualFile, Long templateId) throws IOException {

        Email from = new Email("verified_sendgrid_email@yourdomain.com"); // must be verified
        Email toEmail = new Email(to);

        // Get body from template if templateId provided, otherwise use body parameter
        String emailBody;
        if (templateId != null) {
            EmailTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IOException("Template not found"));
            emailBody = template.getBody();
        } else {
            emailBody = body;
        }

        // Convert to HTML
        Content content = new Content("text/html", convertToHtml(emailBody));
        Mail mail = new Mail(from, subject, toEmail, content);

        // Attachment: uploaded file takes priority
        if (manualFile != null && !manualFile.isEmpty()) {
            Attachments attachment = new Attachments();
            attachment.setFilename(manualFile.getOriginalFilename());
            attachment.setType(manualFile.getContentType());
            attachment.setDisposition("attachment");
            attachment.setContent(Base64.getEncoder().encodeToString(manualFile.getBytes()));
            mail.addAttachments(attachment);
        }
        // Attachment: from template if manual file not provided
        else if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(t -> {
                if (t.getFileData() != null) {
                    Attachments attachment = new Attachments();
                    attachment.setFilename(t.getFileName());
                    attachment.setType(t.getFileType());
                    attachment.setDisposition("attachment");
                    attachment.setContent(Base64.getEncoder().encodeToString(t.getFileData()));
                    mail.addAttachments(attachment);
                }
            });
        }

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                throw new IOException("SendGrid error: " + response.getBody());
            }
        } catch (IOException ex) {
            throw new IOException("Failed to send email: " + ex.getMessage());
        }
    }
}
