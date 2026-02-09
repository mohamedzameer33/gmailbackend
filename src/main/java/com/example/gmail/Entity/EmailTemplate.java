package com.example.gmail.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "email_templates")
@Data
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String templateName;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    // Renamed for clarity and to match the Service Layer
    private String fileName;

    @Lob
    @Column(columnDefinition = "LONGBLOB") // Stores the actual file in the DB
    private byte[] fileData;

    private String fileType;
}