package com.weatherapp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persists recently searched cities to history.json in the working directory.
 * Entries are kept unique (case-insensitive), ordered with most-recent first,
 * and capped at MAX_ENTRIES.
 */
public class SearchHistory {
    public static final int MAX_ENTRIES = 10;
    private static final Path HISTORY_FILE = Paths.get("history.json");

    private final List<HistoryEntry> entries;

    public SearchHistory() {
        this.entries = new ArrayList<>();
        load();
    }

    public List<HistoryEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public List<String> getCityLabels() {
        List<String> labels = new ArrayList<>();
        for (HistoryEntry e : entries) {
            labels.add(e.displayLabel());
        }
        return labels;
    }

    public void add(String city, String country) {
        String cityNormalized = city == null ? "" : city.trim();
        String countryNormalized = country == null ? "" : country.trim();
        if (cityNormalized.isEmpty()) return;

        HistoryEntry fresh = new HistoryEntry(cityNormalized, countryNormalized,
                Instant.now().toEpochMilli());

        Set<String> seenKeys = new LinkedHashSet<>();
        List<HistoryEntry> deduped = new ArrayList<>();
        deduped.add(fresh);
        seenKeys.add(fresh.key());

        for (HistoryEntry existing : entries) {
            if (seenKeys.add(existing.key())) {
                deduped.add(existing);
            }
        }

        entries.clear();
        int cap = Math.min(MAX_ENTRIES, deduped.size());
        for (int i = 0; i < cap; i++) {
            entries.add(deduped.get(i));
        }
        save();
    }

    public void clear() {
        entries.clear();
        save();
    }

    private void load() {
        if (!Files.exists(HISTORY_FILE)) return;
        try (Reader reader = Files.newBufferedReader(HISTORY_FILE, StandardCharsets.UTF_8)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject root = new JSONObject(tokener);
            JSONArray arr = root.optJSONArray("entries");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                String c = obj.optString("city", "");
                String co = obj.optString("country", "");
                long t = obj.optLong("timestamp", 0L);
                if (!c.isEmpty()) entries.add(new HistoryEntry(c, co, t));
            }
        } catch (IOException | org.json.JSONException ex) {
            System.err.println("Warning: could not read history.json: " + ex.getMessage());
        }
    }

    private void save() {
        JSONObject root = new JSONObject();
        JSONArray arr = new JSONArray();
        for (HistoryEntry e : entries) {
            JSONObject obj = new JSONObject();
            obj.put("city", e.city);
            obj.put("country", e.country);
            obj.put("timestamp", e.timestamp);
            arr.put(obj);
        }
        root.put("entries", arr);
        try {
            Files.createDirectories(HISTORY_FILE.getParent() == null ? Paths.get(".") : HISTORY_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(HISTORY_FILE, StandardCharsets.UTF_8)) {
                root.write(writer, 2, 0);
            }
        } catch (IOException ex) {
            System.err.println("Warning: could not write history.json: " + ex.getMessage());
        }
    }

    public static final class HistoryEntry {
        public final String city;
        public final String country;
        public final long timestamp;

        public HistoryEntry(String city, String country, long timestamp) {
            this.city = city;
            this.country = country;
            this.timestamp = timestamp;
        }

        public String key() {
            return (city + "," + country).toLowerCase();
        }

        public String displayLabel() {
            if (country == null || country.isEmpty() || "XX".equals(country)) {
                return city;
            }
            return city + ", " + country;
        }
    }
}
