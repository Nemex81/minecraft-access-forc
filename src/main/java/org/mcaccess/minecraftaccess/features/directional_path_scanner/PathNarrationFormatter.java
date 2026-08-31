package org.mcaccess.minecraftaccess.features.directional_path_scanner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.language.I18n;

import org.mcaccess.minecraftaccess.Config;

/**
 * Formats a PathScanReport into localized, linear speech strings for NVDA / Screen Reader.
 */
public final class PathNarrationFormatter {

    private PathNarrationFormatter() {
    }

    public static String formatReport(PathScanReport report, Config.DirectionalPathScanner.VerbosityMode verbosity) {
        if (report == null) return "";

        String dirName = getLocalizedDirectionName(report.directionKey());
        String groundName = report.primaryGroundName();
        boolean hasGround = groundName != null && !groundName.isBlank() && !groundName.equalsIgnoreCase("Aria") && !groundName.equalsIgnoreCase("Air");

        // 1. SUMMARY_ONLY Mode
        if (verbosity == Config.DirectionalPathScanner.VerbosityMode.SUMMARY_ONLY) {
            PathScanReport.PathScanEvent terminalObstacle = report.events().stream()
                    .filter(e -> e.type() == PathScanReport.EventType.OBSTACLE_BLOCK || e.type() == PathScanReport.EventType.DROP_HAZARD)
                    .findFirst()
                    .orElse(null);

            if (terminalObstacle != null) {
                String obstacleName = terminalObstacle.type() == PathScanReport.EventType.DROP_HAZARD
                        ? I18n.get("minecraft_access.path_scanner.drop_hazard_short", terminalObstacle.dropDepth())
                        : terminalObstacle.name();
                if (hasGround) {
                    return I18n.get("minecraft_access.path_scanner.report_blocked_summary_on_ground", dirName, terminalObstacle.distance(), obstacleName, groundName);
                }
                return I18n.get("minecraft_access.path_scanner.report_blocked_summary", dirName, terminalObstacle.distance(), obstacleName);
            }

            if (hasGround) {
                return I18n.get("minecraft_access.path_scanner.report_clear_on_ground", dirName, report.freeDistance(), groundName);
            }
            return I18n.get("minecraft_access.path_scanner.report_clear", dirName, report.freeDistance());
        }

        // 2. DETAILED Mode (Full-range narrative)
        if (verbosity == Config.DirectionalPathScanner.VerbosityMode.DETAILED) {
            StringBuilder sb = new StringBuilder();
            if (report.freeDistance() > 0) {
                if (hasGround) {
                    sb.append(I18n.get("minecraft_access.path_scanner.detailed_prefix_on_ground", dirName, report.freeDistance(), groundName));
                } else {
                    sb.append(I18n.get("minecraft_access.path_scanner.detailed_prefix", dirName, report.freeDistance()));
                }
            } else {
                sb.append(dirName).append(": ");
            }

            List<String> eventFragments = new ArrayList<>();
            for (PathScanReport.PathScanEvent event : report.events()) {
                switch (event.type()) {
                    case ELEVATION_CHANGE -> {
                        String elevationLabel = event.dropDepth() > 0
                                ? I18n.get("minecraft_access.path_scanner.elevation_up", event.distance(), event.y(), event.name())
                                : I18n.get("minecraft_access.path_scanner.elevation_down", event.distance(), event.y(), event.name());
                        eventFragments.add(elevationLabel);
                    }
                    case ITEM_RESOURCE -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_item_detailed", event.name(), event.distance(), event.y()));
                    case PASSIVE_MOB -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_passive_detailed", event.name(), event.distance(), event.y()));
                    case HOSTILE_MOB -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_hostile_detailed", event.name(), event.distance(), event.y()));
                    case FLUID -> {
                        String fluidLabel = event.name().equalsIgnoreCase("lava")
                                ? I18n.get("minecraft_access.path_scanner.fluid_lava")
                                : I18n.get("minecraft_access.path_scanner.fluid_water");
                        eventFragments.add(I18n.get("minecraft_access.path_scanner.event_fluid_detailed", fluidLabel, event.distance(), event.y()));
                    }
                    case DROP_HAZARD -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_drop_detailed", event.dropDepth(), event.distance(), event.y()));
                    case OBSTACLE_BLOCK -> {
                        if (event.name().equals("pinch_gap")) {
                            eventFragments.add(I18n.get("minecraft_access.path_scanner.event_pinch", event.distance()));
                        } else {
                            eventFragments.add(I18n.get("minecraft_access.path_scanner.event_obstacle_detailed", event.name(), event.distance(), event.y()));
                        }
                    }
                    default -> {
                    }
                }
            }

            if (report.freeDistance() == 0 && !eventFragments.isEmpty()) {
                sb.append(String.join(", ", eventFragments));
            } else if (!eventFragments.isEmpty()) {
                sb.append(", ").append(String.join(", ", eventFragments));
            }

            if (report.freeDistance() == report.totalRange() && eventFragments.isEmpty()) {
                if (hasGround) {
                    return I18n.get("minecraft_access.path_scanner.report_clear_on_ground", dirName, report.totalRange(), groundName);
                }
                return I18n.get("minecraft_access.path_scanner.report_clear", dirName, report.totalRange());
            }

            return sb.toString();
        }

        // 3. COMPACT Mode (First safe segment + intermediate events + first obstacle)
        if (report.events().isEmpty()) {
            if (hasGround) {
                return I18n.get("minecraft_access.path_scanner.report_clear_on_ground", dirName, report.freeDistance(), groundName);
            }
            return I18n.get("minecraft_access.path_scanner.report_clear", dirName, report.freeDistance());
        }

        StringBuilder sb = new StringBuilder();
        if (report.freeDistance() > 0) {
            if (hasGround) {
                sb.append(I18n.get("minecraft_access.path_scanner.free_prefix_on_ground", dirName, report.freeDistance(), groundName));
            } else {
                sb.append(I18n.get("minecraft_access.path_scanner.free_prefix", dirName, report.freeDistance()));
            }
        } else {
            sb.append(dirName).append(": ");
        }

        List<String> eventFragments = new ArrayList<>();
        for (PathScanReport.PathScanEvent event : report.events()) {
            switch (event.type()) {
                case ITEM_RESOURCE -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_item", event.name(), event.distance()));
                case PASSIVE_MOB -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_passive", event.name(), event.distance()));
                case HOSTILE_MOB -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_hostile", event.name(), event.distance()));
                case FLUID -> {
                    String fluidLabel = event.name().equalsIgnoreCase("lava")
                            ? I18n.get("minecraft_access.path_scanner.fluid_lava")
                            : I18n.get("minecraft_access.path_scanner.fluid_water");
                    eventFragments.add(I18n.get("minecraft_access.path_scanner.event_fluid", fluidLabel, event.distance()));
                }
                case DROP_HAZARD -> eventFragments.add(I18n.get("minecraft_access.path_scanner.event_drop", event.dropDepth(), event.distance()));
                case OBSTACLE_BLOCK -> {
                    if (event.name().equals("pinch_gap")) {
                        eventFragments.add(I18n.get("minecraft_access.path_scanner.event_pinch", event.distance()));
                    } else {
                        eventFragments.add(I18n.get("minecraft_access.path_scanner.event_obstacle", event.name(), event.distance()));
                    }
                }
                default -> {
                }
            }
        }

        if (report.freeDistance() == 0 && !eventFragments.isEmpty()) {
            sb.append(String.join(", ", eventFragments));
        } else if (!eventFragments.isEmpty()) {
            sb.append(", ").append(String.join(", ", eventFragments));
        }

        return sb.toString();
    }

    public static String getLocalizedDirectionName(String key) {
        if (key == null) return "";
        return I18n.get("minecraft_access.direction." + key.toLowerCase());
    }
}
