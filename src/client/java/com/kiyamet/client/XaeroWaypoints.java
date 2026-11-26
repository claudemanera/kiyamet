package com.kiyamet.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;

/** Reads the newest waypoint from Xaero's Minimap / World Map save files (xaero/minimap/...). */
public final class XaeroWaypoints {
    /** {x, z} and name of the newest waypoint in the current dimension, or null. */
    public static Object[] newest(Minecraft mc) {
        Path root = mc.gameDirectory.toPath().resolve("xaero").resolve("minimap");
        if (!Files.isDirectory(root)) return null;
        String dim = mc.level == null ? "overworld" : mc.level.dimension().identifier().getPath();
        String tag = dim.equals("overworld") ? "dim%0" : dim.equals("the_nether") ? "dim%-1" : dim.equals("the_end") ? "dim%1" : dim;
        try (Stream<Path> s = Files.walk(root, 4)) {
            List<Path> files = s.filter(f -> f.toString().endsWith(".txt") && f.toString().contains(tag)).toList();
            Path best = null; long t = -1;
            for (Path f : files) { long m = Files.getLastModifiedTime(f).toMillis(); if (m > t) { t = m; best = f; } }
            if (best == null) return null;
            Object[] out = null;
            for (String line : Files.readAllLines(best)) {
                if (!line.startsWith("waypoint:")) continue;
                String[] p = line.split(":");
                if (p.length < 8 || p[7].equals("true")) continue;    // disabled
                try { out = new Object[]{Integer.parseInt(p[3]), Integer.parseInt(p[5]), p[1].replace("§§", ":")}; } catch (NumberFormatException ignored) {}
                if (p.length > 13 && p[13].equals("true")) return out;  // marked as destination: take it
            }
            return out;
        } catch (IOException e) {
            return null;
        }
    }

    private XaeroWaypoints() {}
}
