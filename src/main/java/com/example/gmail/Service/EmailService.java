package com.example.gmail.Service;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import com.sendgrid.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Service
public class EmailService {

    @Autowired
    private TemplateRepository templateRepository;

    // --- SEND EMAIL USING SENDGRID ---
    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile manualFile, Long templateId) throws IOException {
        SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));

        Mail mail = new Mail();
        Email fromEmail = new Email("mohamedzameermpm123@gmail.com"); // must be verified in SendGrid
        mail.setFrom(fromEmail);
        mail.setSubject(subject);

        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        mail.addPersonalization(personalization);

        Content content = new Content("text/plain", body);
        mail.addContent(content);

        // Attach manual file if uploaded
        if (manualFile != null && !manualFile.isEmpty()) {
            Attachments attachments = new Attachments();
            attachments.setContent(Base64.getEncoder().encodeToString(manualFile.getBytes()));
            attachments.setType(manualFile.getContentType());
            attachments.setFilename(manualFile.getOriginalFilename());
            attachments.setDisposition("attachment");
            mail.addAttachments(attachments);
        } 
        // Attach file from template if templateId provided
        else if (templateId != null) {
            Optional<EmailTemplate> templateOpt = templateRepository.findById(templateId);
            if (templateOpt.isPresent()) {
                EmailTemplate temp = templateOpt.get();
                if (temp.getFileData() != null) {
                    Attachments attachments = new Attachments();
                    attachments.setContent(Base64.getEncoder().encodeToString(temp.getFileData()));
                    attachments.setType(temp.getFileType());
                    attachments.setFilename(temp.getFileName());
                    attachments.setDisposition("attachment");
                    mail.addAttachments(attachments);
                }
            }
        }

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                throw new IOException("SendGrid error: " + response.getBody());
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}
