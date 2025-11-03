package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Klien minimal untuk Supabase REST (tabel 'moods').
 * - insertMood(...) melakukan POST
 * - fetchEntriesBetween(...) melakukan GET dan mengembalikan list MoodEntry
 *
 * Catatan: untuk parsing GET kita minta CSV (Accept: text/csv) sehingga tidak perlu dependency JSON.
 */
public class SupabaseClient {
    private final String baseUrl; // mis. https://<project>.supabase.co
    private final String apiKey;
    private final HttpClient http;

    public SupabaseClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newHttpClient();
    }

    public boolean insertMood(String mood, int score, LocalDateTime ts, String userId) {
        try {
            String iso = ts.atOffset(ZoneOffset.UTC).toString(); // termasuk Z offset
        String json;
        if (userId == null) {
        json = String.format("{\"mood\":\"%s\",\"score\":%d,\"timestamp\":\"%s\"}",
            escapeJson(mood), score, iso);
        } else {
        json = String.format("{\"mood\":\"%s\",\"score\":%d,\"timestamp\":\"%s\",\"user_id\":\"%s\"}",
            escapeJson(mood), score, iso, escapeJson(userId));
        }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/rest/v1/moods"))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.println("[SupabaseClient] insertMood failed: status=" + resp.statusCode());
                System.err.println("[SupabaseClient] response body: " + resp.body());
                return false;
            }
            return true;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<MoodEntry> fetchEntriesBetween(LocalDate startDate, LocalDate endDate) {
        try {
            // minta hanya kolom mood,score,timestamp sebagai CSV
            String startIso = startDate.atStartOfDay().atOffset(ZoneOffset.UTC).toString();
            String endIso = endDate.atTime(23,59,59).atOffset(ZoneOffset.UTC).toString();
            String select = URLEncoder.encode("mood,score,timestamp,user_id", StandardCharsets.UTF_8);
            String filter = String.format("timestamp=gte.%s&timestamp=lte.%s&order=timestamp.asc",
                    URLEncoder.encode(startIso, StandardCharsets.UTF_8),
                    URLEncoder.encode(endIso, StandardCharsets.UTF_8));
            String uri = String.format("%s/rest/v1/moods?select=%s&%s", baseUrl, select, filter);

            System.out.println("[SupabaseClient] Request URI: " + uri);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            System.out.println("[SupabaseClient] Response status: " + resp.statusCode());
            System.out.println("[SupabaseClient] Response body: " + resp.body());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.println("[SupabaseClient] fetchEntriesBetween failed: status=" + resp.statusCode());
                System.err.println("[SupabaseClient] response body: " + resp.body());
                return List.of();
            }
            return parseCsvToEntries(resp.body());
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    public List<MoodEntry> fetchAllEntries() {
        try {
            String select = URLEncoder.encode("mood,score,timestamp,user_id", StandardCharsets.UTF_8);
            String uri = String.format("%s/rest/v1/moods?select=%s&order=timestamp.asc", baseUrl, select);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.err.println("[SupabaseClient] fetchAllEntries failed: status=" + resp.statusCode());
                System.err.println("[SupabaseClient] response body: " + resp.body());
                return List.of();
            }
            return parseCsvToEntries(resp.body());
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    /**
     * Fetch all entries for a specific user id.
     */
    public List<MoodEntry> fetchAllEntriesForUser(String userId) {
        try {
            String select = URLEncoder.encode("mood,score,timestamp,user_id", StandardCharsets.UTF_8);
            String uri = String.format("%s/rest/v1/moods?select=%s&user_id=eq.%s&order=timestamp.asc",
                    baseUrl, select, URLEncoder.encode(userId, StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return List.of();
            return parseCsvToEntries(resp.body());
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    public List<MoodEntry> fetchEntriesBetweenForUser(LocalDate startDate, LocalDate endDate, String userId) {
        try {
            String startIso = startDate.atStartOfDay().atOffset(ZoneOffset.UTC).toString();
            String endIso = endDate.atTime(23,59,59).atOffset(ZoneOffset.UTC).toString();
            String select = URLEncoder.encode("mood,score,timestamp,user_id", StandardCharsets.UTF_8);
            String filter = String.format("timestamp=gte.%s&timestamp=lte.%s&user_id=eq.%s&order=timestamp.asc",
                    URLEncoder.encode(startIso, StandardCharsets.UTF_8),
                    URLEncoder.encode(endIso, StandardCharsets.UTF_8),
                    URLEncoder.encode(userId, StandardCharsets.UTF_8));
            String uri = String.format("%s/rest/v1/moods?select=%s&%s", baseUrl, select, filter);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return List.of();
            return parseCsvToEntries(resp.body());
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    private List<MoodEntry> parseCsvToEntries(String csv) {
        List<MoodEntry> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        String[] lines = csv.split("\\r?\\n");
        if (lines.length <= 1) return out; // tidak ada data (header saja)

        // Log header CSV
        String headerLine = lines[0].trim();

        if (headerLine.isEmpty()) return out;
        String[] headers = headerLine.split(",", -1);
        int idxMood = -1, idxScore = -1, idxTs = -1, idxUser = -1;
        for (int h = 0; h < headers.length; h++) {
            String hn = unquote(headers[h]).trim().toLowerCase();
            if (hn.equals("mood") || hn.equals("moods")) idxMood = h;
            else if (hn.equals("score") || hn.equals("skor")) idxScore = h;
            else if (hn.equals("timestamp") || hn.equals("time") || hn.equals("created_at")) idxTs = h;
            else if (hn.equals("user_id") || hn.equals("userid") || hn.equals("user")) idxUser = h;
        }
        if (idxMood == -1 || idxScore == -1 || idxTs == -1) {
            return out;
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) continue;
            // basic CSV split (handles quoted values roughly)
            String[] cols = line.split(",", -1);
            if (cols.length <= Math.max(idxMood, Math.max(idxScore, idxTs))) continue;
            String mood = unquote(cols[idxMood]);
            int score = 0;
            try { score = Integer.parseInt(unquote(cols[idxScore])); } catch (NumberFormatException e) {}
            String tsStr = unquote(cols[idxTs]);
            String userId = (idxUser >= 0 && cols.length > idxUser) ? unquote(cols[idxUser]) : null;
            LocalDateTime ts;
            try {
                // Normalize timestamp format to ISO-8601
                tsStr = tsStr.replace(" ", "T");
                if (tsStr.endsWith("+00")) {
                    tsStr = tsStr + ":00";
                }

                java.time.OffsetDateTime odt;
                try {
                    odt = java.time.OffsetDateTime.parse(tsStr);
                    ts = odt.toLocalDateTime();
                } catch (Exception e) {
                    try {
                        ts = LocalDateTime.parse(tsStr);
                    } catch (Exception ex) {
                        continue;
                    }
                }
            } catch (Exception e) {
                continue;
            }
            out.add(new MoodEntry(mood, score, ts, userId));
        }
        return out;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // -------------------- User management (minimal) --------------------
    /**
     * Create a new user row in 'users' table. Returns true on success.
     */
    public boolean createUser(String username, String passwordHash, LocalDateTime lastLogin) {
        try {
            String iso = lastLogin == null ? null : lastLogin.atOffset(ZoneOffset.UTC).toString();
            String json;
            if (iso == null) {
                json = String.format("{\"username\":\"%s\",\"password_hash\":\"%s\"}",
                        escapeJson(username), escapeJson(passwordHash));
            } else {
                json = String.format("{\"username\":\"%s\",\"password_hash\":\"%s\",\"last_login\":\"%s\"}",
                        escapeJson(username), escapeJson(passwordHash), iso);
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/rest/v1/users"))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Fetch a single user by username. Returns null if not found or on error.
     */
    public UserEntry fetchUserByUsername(String username) {
        try {
            String select = URLEncoder.encode("id,username,password_hash,last_login", StandardCharsets.UTF_8);
            String filter = String.format("username=eq.%s&limit=1", URLEncoder.encode(username, StandardCharsets.UTF_8));
            String uri = String.format("%s/rest/v1/users?select=%s&%s", baseUrl, select, filter);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "text/csv")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return null;
            List<UserEntry> users = parseCsvToUsers(resp.body());
            if (users.isEmpty()) return null;
            return users.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Update user's last_login by id (UUID string). Returns true on success.
     */
    public boolean updateUserLastLogin(String id, LocalDateTime lastLogin) {
        try {
            String iso = lastLogin.atOffset(ZoneOffset.UTC).toString();
            String json = String.format("{\"last_login\":\"%s\"}", iso);
            String uri = String.format("%s/rest/v1/users?id=eq.%s", baseUrl, URLEncoder.encode(id, StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private List<UserEntry> parseCsvToUsers(String csv) {
        List<UserEntry> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        String[] lines = csv.split("\\r?\\n");
        if (lines.length <= 1) return out; // header only
        // header: id,username,password_hash,last_login
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 4) continue;
            String id = unquote(cols[0]);
            String username = unquote(cols[1]);
            String pwHash = unquote(cols[2]);
            String lastLoginStr = unquote(cols[3]);
            LocalDateTime lastLogin = null;
            if (lastLoginStr != null && !lastLoginStr.isEmpty()) {
                try {
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(lastLoginStr);
                    lastLogin = odt.toLocalDateTime();
                } catch (Exception e) {
                    try { lastLogin = LocalDateTime.parse(lastLoginStr); } catch (Exception ex) { lastLogin = null; }
                }
            }
            out.add(new UserEntry(id, username, pwHash, lastLogin));
        }
        return out;
    }

    public static class UserEntry {
        public final String id;
        public final String username;
        public final String passwordHash;
        public final LocalDateTime lastLogin;
        public UserEntry(String id, String username, String passwordHash, LocalDateTime lastLogin) {
            this.id = id; this.username = username; this.passwordHash = passwordHash; this.lastLogin = lastLogin;
        }
    }

    public static class MoodEntry {
        public final String mood;
        public final int score;
        public final LocalDateTime timestamp;
        public final String userId;
        public MoodEntry(String mood, int score, LocalDateTime timestamp, String userId) {
            this.mood = mood; this.score = score; this.timestamp = timestamp; this.userId = userId;
        }
    }
}
