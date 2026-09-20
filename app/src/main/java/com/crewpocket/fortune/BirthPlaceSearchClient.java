package com.crewpocket.fortune;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class BirthPlaceSearchClient {
    private static final String ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
    private final OkHttpClient httpClient;

    public BirthPlaceSearchClient() {
        this(new OkHttpClient());
    }

    BirthPlaceSearchClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<Result> search(String query) throws IOException {
        String clean = query == null ? "" : query.trim();
        if (clean.length() < 2) return Collections.emptyList();

        String language = Locale.getDefault().getLanguage();
        if (language == null || language.trim().isEmpty()) language = "en";

        String url = ENDPOINT
                + "?name=" + URLEncoder.encode(clean, "UTF-8")
                + "&count=8"
                + "&language=" + URLEncoder.encode(language.toLowerCase(Locale.US), "UTF-8")
                + "&format=json";

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("出生地搜尋服務暫時無法使用");
            }
            JSONObject root = new JSONObject(response.body().string());
            JSONArray rows = root.optJSONArray("results");
            if (rows == null || rows.length() == 0) return Collections.emptyList();

            List<Result> results = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                if (!row.has("latitude") || !row.has("longitude")) continue;

                String timezone = row.optString("timezone", "").trim();
                if (timezone.isEmpty()) continue;

                results.add(new Result(
                        row.optString("name", ""),
                        row.optString("admin1", ""),
                        row.optString("country", ""),
                        row.optString("country_code", ""),
                        row.optDouble("latitude"),
                        row.optDouble("longitude"),
                        timezone));
            }
            return results;
        } catch (org.json.JSONException error) {
            throw new IOException("出生地搜尋資料格式不正確", error);
        }
    }

    public static final class Result {
        public final String name;
        public final String admin1;
        public final String country;
        public final String countryCode;
        public final double latitude;
        public final double longitude;
        public final String timezone;

        Result(
                String name,
                String admin1,
                String country,
                String countryCode,
                double latitude,
                double longitude,
                String timezone) {
            this.name = clean(name);
            this.admin1 = clean(admin1);
            this.country = clean(country);
            this.countryCode = clean(countryCode);
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = clean(timezone);
        }

        public String displayName() {
            StringBuilder out = new StringBuilder();
            appendDistinct(out, name);
            appendDistinct(out, admin1);
            appendDistinct(out, country);
            return out.length() == 0 ? countryCode : out.toString();
        }

        public String detail() {
            return String.format(
                    Locale.US,
                    "%.4f, %.4f · %s",
                    latitude,
                    longitude,
                    timezone);
        }

        private static void appendDistinct(StringBuilder out, String value) {
            String clean = clean(value);
            if (clean.isEmpty()) return;
            String current = out.toString();
            if (current.equals(clean) || current.endsWith(", " + clean)) return;
            if (out.length() > 0) out.append(", ");
            out.append(clean);
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
