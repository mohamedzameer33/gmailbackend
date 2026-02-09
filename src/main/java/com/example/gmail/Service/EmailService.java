package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import com.sendgrid.*;
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

    private final String VERIFIED_SENDER = "mohamedzameermpm123@gmail.com"; // Must match verified sender
    private final String SENDER_NAME = "Mohamed Zameer S M";

    // SEND EMAIL WITH ATTACHMENTS
    public void sendEmailWithAttachment(String to, String subject, String body,
                                        MultipartFile manualFile, Long templateId) throws IOException {

        Email from = new Email(VERIFIED_SENDER, SENDER_NAME);
        Email toEmail = new Email(to);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setReplyTo(new Email(VERIFIED_SENDER)); // Reply-To same as sender
        mail.setSubject(subject);

        Content content = new Content("text/html", body);
        mail.addContent(content);
        mail.addPersonalization(new Personalization() {{
            addTo(toEmail);
        }});

        // Attachment from manual file
        if (manualFile != null && !manualFile.isEmpty()) {
            Attachments attachment = new Attachments();
            attachment.setFilename(manualFile.getOriginalFilename());
            attachment.setType(manualFile.getContentType());
            attachment.setDisposition("attachment");
            attachment.setContent(Base64.getEncoder().encodeToString(manualFile.getBytes()));
            mail.addAttachments(attachment);
        }

        // Attachment from template
        else if (templateId != null) {
            templateRepository.findById(templateId).ifPresent(t -> {
                try {
                    if (t.getFileData() != null) {
                        Attachments attachment = new Attachments();
                        attachment.setFilename(t.getFileName());
                        attachment.setType(t.getFileType());
                        attachment.setDisposition("attachment");
                        attachment.setContent(Base64.getEncoder().encodeToString(t.getFileData()));
                        mail.addAttachments(attachment);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Send via SendGrid
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

    // SAVE TEMPLATE
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

    // UPDATE TEMPLATE
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

    // DELETE TEMPLATE
    public void deleteTemplate(Long id) throws Exception {
        if (!templateRepository.existsById(id)) throw new Exception("Template not found");
        templateRepository.deleteById(id);
    }
}
