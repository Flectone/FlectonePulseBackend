package net.flectone.pulse.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class ServerMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serverCore;
    private String serverVersion;

    private String osName;
    private String osVersion;
    private String osArchitecture;

    private String javaVersion;
    private int cpuCores;

    @Column(name = "total_ram")
    private long totalRAM;

    private String location;
    private String projectVersion;
    private String projectLanguage;
    private String onlineMode;
    private String proxyMode;
    private String databaseMode;
    private int playerCount;

    @Column(columnDefinition = "TEXT")
    private String modules;

//    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}