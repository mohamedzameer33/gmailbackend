package com.example.gmail.Controller;

import com.example.gmail.Entity.EmailTemplate;
import com.example.gmail.Repository.TemplateRepository;
import com.example.gmail.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/mail")
@CrossOrigin(origins = "https://zam-gmailtemp.vercel.app")
public class EmailController {

    @Autowired private EmailService emailService;
    @Autowired private TemplateRepository templateRepository;

    @GetMapping("/templates")
    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @PutMapping("/template/{id}")
    public ResponseEntity<?> edit(@PathVariable Long id,
                                  @RequestParam("templateName") String name,
                                  @RequestParam("subject") String sub,
                                  @RequestParam("body") String body,
                                  @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            return ResponseEntity.ok(emailService.updateTemplate(id, name, sub, body, file));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Update failed: " + e.getMessage());
        }
    }

    @PostMapping("/template")
    public ResponseEntity<?> saveTemplate(
            @RequestParam("templateName") String templateName,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            EmailTemplate savedTemplate = emailService.saveTemplateWithFile(templateName, subject, body, file);
            return ResponseEntity.ok(savedTemplate);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to archive template: " + e.getMessage());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMail(
            @RequestParam("to") String to,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // We pass templateId directly. Let the Service Layer handle the database lookup.
            emailService.sendEmailWithAttachment(to, subject, body, file, templateId);
            return ResponseEntity.ok("Transmission Successful!");
        } catch (Exception e) {
            e.printStackTrace(); // Good for debugging server logs
            return ResponseEntity.status(500).body("Transmission Failed: " + e.getMessage());
        }
    }
    @DeleteMapping("/template/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            emailService.deleteTemplate(id);
            return ResponseEntity.ok("Template deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
