package net.flectone.pulse.backend.controller;

import lombok.RequiredArgsConstructor;
import net.flectone.pulse.backend.aspect.CachedHourlySvg;
import net.flectone.pulse.backend.aspect.SpamProtect;
import net.flectone.pulse.backend.dto.MetricsDTO;
import net.flectone.pulse.backend.generator.*;
import net.flectone.pulse.backend.service.MetricsService;
import net.flectone.pulse.backend.util.HttpUtils;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pulse/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;
    private final HttpUtils httpRequestUtils;

    @SpamProtect
    @PostMapping
    public ResponseEntity<String> saveMetrics(@RequestBody MetricsDTO metricsDTO) {
        metricsDTO.setLocation(httpRequestUtils.getClientLocationFromIp());
        metricsService.saveMetrics(metricsDTO);

        return ResponseEntity.ok("Saved");
    }

    @CachedHourlySvg
    @GetMapping("/svg")
    public ResponseEntity<String> getMainSvg() throws SVGGraphics2DIOException {

        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<MetricsDTO> metrics = metricsService.getMetrics(7, ChronoUnit.DAYS).stream()
                .filter(m -> m.getCreatedAt().isBefore(todayStart))
                .toList();

        Map<Instant, Long> serversByHour = metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> truncateToChronoUnit(m.getCreatedAt(), ChronoUnit.HOURS),
                        Collectors.counting()
                ));

        Map<Instant, Long> playersByHour = metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> truncateToChronoUnit(m.getCreatedAt(), ChronoUnit.HOURS),
                        Collectors.summingLong(MetricsDTO::getPlayerCount)
                ));

        Map<Instant, Map<Integer, Long>> serversDayHour = new TreeMap<>();
        Map<Instant, Map<Integer, Long>> playersDayHour = new TreeMap<>();

        serversByHour.forEach((instant, cnt) -> {
            Instant dayInstant = truncateToChronoUnit(instant, ChronoUnit.DAYS);
            int hour = getHourFromInstant(instant);
            serversDayHour.computeIfAbsent(dayInstant, i -> new HashMap<>())
                    .put(hour, cnt);
        });

        playersByHour.forEach((instant, cnt) -> {
            Instant dayInstant = truncateToChronoUnit(instant, ChronoUnit.DAYS);
            int hour = getHourFromInstant(instant);
            playersDayHour.computeIfAbsent(dayInstant, d -> new HashMap<>())
                    .put(hour, cnt);
        });

        return svgResponse(new TimeSeriesSvg(playersDayHour, serversDayHour, "Players", "Servers"));
    }

    @CachedHourlySvg
    @GetMapping("/svg/server-versions")
    public ResponseEntity<String> getVersionsDistributionSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getServerVersion,
                        Collectors.counting()
                ));

        return svgResponse(new CircleDistributionSvg(data, "", true));
    }

    @CachedHourlySvg
    @GetMapping("/svg/ram-usage")
    public ResponseEntity<String> getRamUsageSvg() throws SVGGraphics2DIOException {
        List<MetricsDTO> metrics = metricsService.getMetrics(1, ChronoUnit.HOURS);

        TreeMap<String, Long> distribution = new TreeMap<>();
        metrics.stream()
                .map(m -> String.valueOf((int) Math.ceil(m.getTotalRAM() / (1024.0 * 1024.0) )))
                .forEach(ram -> distribution.put(ram, distribution.getOrDefault(ram, 0L) + 1));

        return svgResponse(new CircleDistributionSvg(distribution, " MB", "", true));
    }

    @CachedHourlySvg
    @GetMapping("/svg/modules-status")
    public ResponseEntity<String> getModulesStatusSvg() throws SVGGraphics2DIOException {
        List<MetricsDTO> metricsDTOS = metricsService.getMetrics(1, ChronoUnit.HOURS);

        Map<String, Long> modulesStats = metricsDTOS.stream()
                .flatMap(m -> m.getModules().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingLong(e -> "true".equals(e.getValue()) ? 1 : 0)
                ));

        long total = metricsDTOS.size();
        return svgResponse(new StatusItemsSvg(modulesStats, total, "Enabled", "Disabled"));
    }

    @CachedHourlySvg
    @GetMapping("/svg/server-types")
    public ResponseEntity<String> getServerTypesSvg() throws SVGGraphics2DIOException {
        Map<String, Pair<Long, Long>> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getServerCore,
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.summingLong(MetricsDTO::getPlayerCount),
                                Pair::of
                        )
                ));

        return svgResponse(new ComparisonSvg(data, "Servers", "Players"));
    }

    @CachedHourlySvg
    @GetMapping("/svg/online-mode")
    public ResponseEntity<String> getOnlineModeSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getOnlineMode,
                        Collectors.counting()
                ));
        return svgResponse(new BarDistributionSvg(data, ""));
    }


    @CachedHourlySvg
    @GetMapping("/svg/project-versions")
    public ResponseEntity<String> getPluginVersionsSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getProjectVersion,
                        Collectors.counting()
                ));

        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/project-languages")
    public ResponseEntity<String> getPluginLanguagesSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getProjectLanguage,
                        Collectors.counting()
                ));
        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/proxy-modes")
    public ResponseEntity<String> getProxyModesSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getProxyMode,
                        Collectors.counting()
                ));

        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/database-modes")
    public ResponseEntity<String> getDatabaseModesSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getDatabaseMode,
                        Collectors.counting()
                ));

        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/server-locations")
    public ResponseEntity<String> getServerLocationsSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getLocation,
                        Collectors.counting()
                ));

        return svgResponse(new CircleDistributionSvg(data, "", true));
    }

    @CachedHourlySvg
    @GetMapping("/svg/java-versions")
    public ResponseEntity<String> getJavaVersionsSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getJavaVersion,
                        Collectors.counting()
                ));

        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/core-counts")
    public ResponseEntity<String> getCoreCountsSvg() throws SVGGraphics2DIOException {
        TreeMap<String, Long> data = new TreeMap<>();
        metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .map(m -> String.valueOf(m.getCpuCores()))
                .forEach(cores -> data.put(cores, data.getOrDefault(cores, 0L) + 1));
        return svgResponse(new BarDistributionSvg(data, " cores"));
    }

    @CachedHourlySvg
    @GetMapping("/svg/system-archs")
    public ResponseEntity<String> getSystemArchsSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        MetricsDTO::getOsArchitecture,
                        Collectors.counting()
                ));
        return svgResponse(new BarDistributionSvg(data, ""));
    }

    @CachedHourlySvg
    @GetMapping("/svg/operation-systems")
    public ResponseEntity<String> getOperationSystemsSvg() throws SVGGraphics2DIOException {
        Map<String, Long> data = metricsService.getMetrics(1, ChronoUnit.HOURS).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getOsName() + (m.getOsVersion() == null ? "" : " " + m.getOsVersion()),
                        Collectors.counting()
                ));

        return svgResponse(new CircleDistributionSvg(data, "", true));
    }

    private Instant truncateToChronoUnit(Instant instant, ChronoUnit chronoUnit) {
        return instant.truncatedTo(chronoUnit);
    }

    private int getHourFromInstant(Instant instant) {
        return LocalDateTime.ofInstant(truncateToChronoUnit(instant, ChronoUnit.HOURS), ZoneOffset.UTC).getHour();
    }

    private ResponseEntity<String> svgResponse(SvgGenerator generator) throws SVGGraphics2DIOException {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(generator.generate());
    }
}