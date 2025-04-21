package net.flectone.pulse.backend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import net.flectone.pulse.backend.dto.MetricsDTO;
import net.flectone.pulse.backend.model.ServerMetrics;
import net.flectone.pulse.backend.repository.MetricsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MetricsRepository metricsRepository;
    private final Gson gson;

    @Transactional
    public void saveMetrics(MetricsDTO requestDTO) {
        ServerMetrics serverMetrics = new ServerMetrics();
        serverMetrics.setServerCore(requestDTO.getServerCore());
        serverMetrics.setServerVersion(requestDTO.getServerVersion());
        serverMetrics.setOsName(requestDTO.getOsName());
        serverMetrics.setOsVersion(requestDTO.getOsVersion());
        serverMetrics.setOsArchitecture(requestDTO.getOsArchitecture());
        serverMetrics.setJavaVersion(requestDTO.getJavaVersion());
        serverMetrics.setCpuCores(requestDTO.getCpuCores());
        serverMetrics.setTotalRAM(requestDTO.getTotalRAM());
        serverMetrics.setLocation(requestDTO.getLocation());
        serverMetrics.setProjectVersion(requestDTO.getProjectVersion());
        serverMetrics.setProjectLanguage(requestDTO.getProjectLanguage());
        serverMetrics.setOnlineMode(requestDTO.getOnlineMode());
        serverMetrics.setProxyMode(requestDTO.getProxyMode());
        serverMetrics.setDatabaseMode(requestDTO.getDatabaseMode());
        serverMetrics.setPlayerCount(requestDTO.getPlayerCount());
        serverMetrics.setModules(gson.toJson(requestDTO.getModules()));
        serverMetrics.setCreatedAt(requestDTO.getCreatedAt());

        metricsRepository.save(serverMetrics);
    }

    public List<MetricsDTO> getMetrics(int amount, ChronoUnit chronoUnit) {
        Instant timestampFrom = Instant.now().minus(amount, chronoUnit);
        return metricsRepository.findByCreatedAtAfter(timestampFrom).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MetricsDTO convertToResponse(ServerMetrics serverMetrics) {
        return new MetricsDTO(
                serverMetrics.getServerCore(),
                serverMetrics.getServerVersion(),
                serverMetrics.getOsName(),
                serverMetrics.getOsVersion(),
                serverMetrics.getOsArchitecture(),
                serverMetrics.getJavaVersion(),
                serverMetrics.getCpuCores(),
                serverMetrics.getTotalRAM(),
                serverMetrics.getLocation(),
                serverMetrics.getProjectVersion(),
                serverMetrics.getProjectLanguage(),
                serverMetrics.getOnlineMode(),
                serverMetrics.getProxyMode(),
                serverMetrics.getDatabaseMode(),
                serverMetrics.getPlayerCount(),
                gson.fromJson(serverMetrics.getModules(), new TypeToken<Map<String, String>>() {}.getType()),
                serverMetrics.getCreatedAt()
        );
    }

    public void generateAndSaveTestData() {
        List<ServerMetrics> metrics = new ArrayList<>();
        Instant startTime = Instant.now().minus(10, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS);

        for (int serverId = 0; serverId < 5; serverId++) {
            System.out.println(serverId);
            for (int day = 0; day < 10; day++) {
                for (int hour = 0; hour < 24; hour++) {
                    Instant timestamp = startTime
                            .plus(day, ChronoUnit.DAYS)
                            .plus(hour, ChronoUnit.HOURS);

                    metrics.add(generateRandomMetrics(serverId, timestamp));
                }
            }
        }

        metricsRepository.saveAll(metrics);
    }

    private ServerMetrics generateRandomMetrics(int serverId, Instant timestamp) {
        ServerMetrics metrics = new ServerMetrics();

        metrics.setServerCore(getRandomServerCore(serverId));
        metrics.setServerVersion(getRandomVersion(serverId));
        metrics.setOsName(getRandomOS(serverId));
        metrics.setOsArchitecture(getRandomArch(serverId));
        metrics.setLocation(getRandomLocation(serverId));
        metrics.setJavaVersion(getRandomJavaVersion());
        metrics.setCpuCores(ThreadLocalRandom.current().nextInt(1, 17));
        metrics.setTotalRAM(ThreadLocalRandom.current().nextLong(1, 100) * 1024L);
        metrics.setPlayerCount(ThreadLocalRandom.current().nextInt(0, 11));
        metrics.setOnlineMode(String.valueOf(ThreadLocalRandom.current().nextBoolean()));
        metrics.setProxyMode(getRandomProxyMode());
        metrics.setDatabaseMode(getRandomDatabaseMode());
        metrics.setProjectLanguage(getRandomProjectLanguage());
        metrics.setProjectVersion("1." + ThreadLocalRandom.current().nextInt(0, 5) + ".0");
        metrics.setOsVersion(getRandomOSVersion());
        metrics.setModules(generateRandomModules());
        metrics.setCreatedAt(timestamp);

        return metrics;
    }

    private String getRandomServerCore(int serverId) {
        String[] cores = {"Paper", "Spigot", "Purpur", "Fabric", "Forge", "Folia", "Bukkit", "Sponge", "Random", "Leaves", "Leaf", "1", "2", "3", "4"};
        return cores[Math.abs(ThreadLocalRandom.current().nextInt() % cores.length)];
    }

    private String getRandomVersion(int serverId) {
        return "1." + (20 - serverId) + "." + ThreadLocalRandom.current().nextInt(1, 5);
    }

    private String getRandomOS(int serverId) {
        String[] os = {"Linux", "Windows", "macOS"};
        return os[serverId % os.length];
    }

    private String getRandomArch(int serverId) {
        return serverId % 2 == 0 ? "amd64" : "arm64";
    }

    private String getRandomLocation(int serverId) {
        String[] locations = {"Russia", "USA", "Germany", "Japan", "Brazil"};
        return locations[serverId % locations.length];
    }

    private String getRandomJavaVersion() {
        String[] versions = {"8", "11", "17"};
        return versions[ThreadLocalRandom.current().nextInt(versions.length)];
    }

    private String getRandomProxyMode() {
        String[] proxies = {"BungeeCord", "Velocity", "Waterfall", "None"};
        return proxies[ThreadLocalRandom.current().nextInt(proxies.length)];
    }

    private String getRandomDatabaseMode() {
        String[] modes = {"remote", "embedded", "cloud"};
        return modes[ThreadLocalRandom.current().nextInt(modes.length)];
    }

    private String getRandomProjectLanguage() {
        String[] langs = {"Java", "Kotlin", "Groovy"};
        return langs[ThreadLocalRandom.current().nextInt(langs.length)];
    }

    private String getRandomOSVersion() {
        String[] versions = {"10", "11", "22.04", "2022"};
        return versions[ThreadLocalRandom.current().nextInt(versions.length)];
    }

    private String generateRandomModules() {
        Map<String, String> modules = new HashMap<>();
        modules.put("core", "enabled");
        modules.put("spit", "disabled");
        modules.put("chat", ThreadLocalRandom.current().nextBoolean() ? "enabled" : "disabled");
        modules.put("anti-cheat", ThreadLocalRandom.current().nextBoolean() ? "enabled" : "disabled");
        return modules.toString();
    }
}