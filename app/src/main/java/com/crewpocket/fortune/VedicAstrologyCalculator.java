package com.crewpocket.fortune;

import io.github.cosinekitty.astronomy.Aberration;
import io.github.cosinekitty.astronomy.Astronomy;
import io.github.cosinekitty.astronomy.Body;
import io.github.cosinekitty.astronomy.Ecliptic;
import io.github.cosinekitty.astronomy.Spherical;
import io.github.cosinekitty.astronomy.Time;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VedicAstrologyCalculator {
    public static final String METHOD_VERSION = "vedic-lahiri-whole-sign-v1";
    public static final String EPHEMERIS = "Astronomy Engine JVM 2.1.19";
    public static final String AYANAMSA = "Lahiri / Chitrapaksha";
    public static final String NODE_TYPE = "Mean Node";
    public static final double VIMSHOTTARI_YEAR_DAYS = 365.2425;

    private static final double NAKSHATRA_SIZE = 360.0 / 27.0;
    private static final double PADA_SIZE = NAKSHATRA_SIZE / 4.0;

    private static final String[] SIGNS = {
            "Aries 牡羊", "Taurus 金牛", "Gemini 雙子", "Cancer 巨蟹",
            "Leo 獅子", "Virgo 處女", "Libra 天秤", "Scorpio 天蠍",
            "Sagittarius 射手", "Capricorn 摩羯", "Aquarius 水瓶", "Pisces 雙魚"
    };

    private static final String[] SIGN_LORDS = {
            "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
            "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    };

    private static final String[] NAKSHATRAS = {
            "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
            "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni",
            "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha",
            "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha",
            "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
            "Uttara Bhadrapada", "Revati"
    };

    private static final String[] DASHA_ORDER = {
            "Ketu", "Venus", "Sun", "Moon", "Mars",
            "Rahu", "Jupiter", "Saturn", "Mercury"
    };

    private static final double[] DASHA_YEARS = {
            7.0, 20.0, 6.0, 10.0, 7.0, 18.0, 16.0, 19.0, 17.0
    };

    public Map<String, Object> calculate(
            String birthDate,
            String birthTime,
            BirthPlace birthPlace,
            Date now) {
        if (birthPlace == null) {
            throw new IllegalArgumentException("印度星盤需要出生地座標與時區");
        }

        LocalDate date = parseDate(birthDate);
        LocalTime time = parseTime(birthTime);
        ZoneId zone = birthPlace.zoneId();
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        ZonedDateTime birthLocal = resolveBirthTime(localDateTime, zone);
        Instant birthInstant = birthLocal.toInstant();
        Instant nowInstant = now == null ? Instant.now() : now.toInstant();
        Time astroTime = astronomyTime(birthInstant);

        double centuries = astroTime.getTt() / 36525.0;
        double ayanamsa = lahiriAyanamsaDegrees(centuries);
        double tropicalAscendant = tropicalAscendant(
                astroTime, birthPlace.latitude, birthPlace.longitude, centuries);
        double siderealAscendant = normalize(tropicalAscendant - ayanamsa);
        Position lagna = positionOf(siderealAscendant);

        List<PlanetSeed> seeds = new ArrayList<PlanetSeed>();
        seeds.add(new PlanetSeed("Sun", Body.Sun, false));
        seeds.add(new PlanetSeed("Moon", Body.Moon, false));
        seeds.add(new PlanetSeed("Mercury", Body.Mercury, true));
        seeds.add(new PlanetSeed("Venus", Body.Venus, true));
        seeds.add(new PlanetSeed("Mars", Body.Mars, true));
        seeds.add(new PlanetSeed("Jupiter", Body.Jupiter, true));
        seeds.add(new PlanetSeed("Saturn", Body.Saturn, true));

        double rahuTropical = meanLunarNodeDegrees(centuries);
        double rahuSidereal = normalize(rahuTropical - ayanamsa);
        double ketuSidereal = normalize(rahuSidereal + 180.0);

        List<Map<String, Object>> planets = new ArrayList<Map<String, Object>>();
        Map<String, Integer> planetHouses = new LinkedHashMap<String, Integer>();
        Map<String, String> signPlacements = new LinkedHashMap<String, String>();

        for (PlanetSeed seed : seeds) {
            double tropical = tropicalLongitude(seed.body, astroTime);
            double sidereal = normalize(tropical - ayanamsa);
            Position p = positionOf(sidereal);
            int house = wholeSignHouse(lagna.signIndex, p.signIndex);
            boolean retrograde = seed.checkRetrograde && isRetrograde(seed.body, astroTime);
            Map<String, Object> item = planetMap(
                    seed.name, tropical, sidereal, p, house, retrograde, dignity(seed.name, p.signIndex));
            planets.add(item);
            planetHouses.put(seed.name, house);
            signPlacements.put(seed.name, p.sign);
        }

        Position rahuPosition = positionOf(rahuSidereal);
        int rahuHouse = wholeSignHouse(lagna.signIndex, rahuPosition.signIndex);
        Map<String, Object> rahu = planetMap(
                "Rahu", rahuTropical, rahuSidereal, rahuPosition,
                rahuHouse, true, "not_assigned");
        planets.add(rahu);
        planetHouses.put("Rahu", rahuHouse);
        signPlacements.put("Rahu", rahuPosition.sign);

        Position ketuPosition = positionOf(ketuSidereal);
        int ketuHouse = wholeSignHouse(lagna.signIndex, ketuPosition.signIndex);
        Map<String, Object> ketu = planetMap(
                "Ketu", normalize(rahuTropical + 180.0), ketuSidereal, ketuPosition,
                ketuHouse, true, "not_assigned");
        planets.add(ketu);
        planetHouses.put("Ketu", ketuHouse);
        signPlacements.put("Ketu", ketuPosition.sign);

        List<Map<String, Object>> houses = buildHouses(lagna.signIndex, planets);
        List<Map<String, Object>> houseLords = buildHouseLords(lagna.signIndex);
        List<Map<String, Object>> aspects = buildAspects(planets);
        List<Map<String, Object>> conjunctions = buildConjunctions(planets);

        Map<String, Object> moon = findPlanet(planets, "Moon");
        int moonNakshatraIndex = intValue(moon.get("nakshatraIndex"));
        double moonLongitude = doubleValue(moon.get("siderealLongitude"));
        DashaData dasha = buildVimshottari(
                birthInstant,
                nowInstant,
                zone,
                moonNakshatraIndex,
                moonLongitude);

        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("methodVersion", METHOD_VERSION);
        details.put("ephemeris", EPHEMERIS);
        details.put("zodiac", "Sidereal");
        details.put("ayanamsa", AYANAMSA);
        details.put("ayanamsaDegrees", round(ayanamsa, 6));
        details.put("houseSystem", "Whole Sign");
        details.put("nodeType", NODE_TYPE);
        details.put("birthPlaceDisplay", birthPlace.displayName);
        details.put("birthLatitude", round(birthPlace.latitude, 6));
        details.put("birthLongitude", round(birthPlace.longitude, 6));
        details.put("birthTimeZone", birthPlace.timeZoneId);
        details.put("birthLocal", birthLocal.toString());
        details.put("birthUtc", birthInstant.toString());

        details.put("lagnaLongitude", round(siderealAscendant, 6));
        details.put("lagnaSign", lagna.sign);
        details.put("lagnaSignIndex", lagna.signIndex);
        details.put("lagnaDegreeInSign", round(lagna.degreeInSign, 6));
        details.put("lagnaNakshatra", lagna.nakshatra);
        details.put("lagnaNakshatraIndex", lagna.nakshatraIndex);
        details.put("lagnaPada", lagna.pada);

        details.put("moonSign", text(moon.get("sign")));
        details.put("moonNakshatra", text(moon.get("nakshatra")));
        details.put("moonPada", moon.get("pada"));
        details.put("sunSign", text(findPlanet(planets, "Sun").get("sign")));

        details.put("planets", planets);
        details.put("planetaryHousePlacements", planetHouses);
        details.put("signPlacements", signPlacements);
        details.put("houses", houses);
        details.put("houseLords", houseLords);
        details.put("aspects", aspects);
        details.put("conjunctions", conjunctions);

        details.put("mahadashaTimeline", dasha.mahadashaTimeline);
        details.put("currentMahadasha", dasha.currentMahadasha);
        details.put("currentAntardasha", dasha.currentAntardasha);
        details.put("importantPeriods", dasha.importantPeriods);
        details.put("vimshottariConvention",
                "120-year cycle · Moon Nakshatra lord at birth · "
                        + VIMSHOTTARI_YEAR_DAYS + " days per dasha year");

        Map<String, Object> convention = new LinkedHashMap<String, Object>();
        convention.put("planetSource", EPHEMERIS + " geocentric apparent ecliptic-of-date");
        convention.put("lahiri",
                "J2000 23.85305556° + 1.39722222°×T + 0.00018°×T²; T=Julian centuries from J2000 TT");
        convention.put("ascendant",
                "Meeus horizon/ecliptic intersection from GAST + longitude, latitude and mean obliquity; converted with same Lahiri ayanamsa");
        convention.put("rahuKetu", "mean lunar node; Ketu exactly 180° opposite Rahu");
        convention.put("houses", "Whole Sign; Lagna sign is house 1");
        convention.put("aspects",
                "classical graha drishti only: all seven classical planets 7th; Mars 4th/8th; Jupiter 5th/9th; Saturn 3rd/10th; no special node aspects");
        convention.put("conjunction", "same sidereal sign and <= 8° angular separation");
        convention.put("dignity",
                "sign-level exaltation/debilitation/own-sign for seven classical planets; nodes intentionally unassigned");
        details.put("calculationConvention", convention);

        VedicInsightBuilder.enrich(details);
        return details;
    }

    static double lahiriAyanamsaDegrees(double julianCenturiesFromJ2000) {
        double t = julianCenturiesFromJ2000;
        return normalize(23.8530555556
                + 1.3972222222 * t
                + 0.00018 * t * t);
    }

    static double meanLunarNodeDegrees(double julianCenturiesFromJ2000) {
        double t = julianCenturiesFromJ2000;
        double omega = 125.04452
                - 1934.136261 * t
                + 0.0020708 * t * t
                + (t * t * t) / 450000.0;
        return normalize(omega);
    }

    static double tropicalAscendant(
            Time time,
            double latitude,
            double longitude,
            double julianCenturiesFromJ2000) {
        double ramc = normalize(Astronomy.siderealTime(time) * 15.0 + longitude);
        double eps = meanObliquityDegrees(julianCenturiesFromJ2000);
        double theta = Math.toRadians(ramc);
        double phi = Math.toRadians(latitude);
        double e = Math.toRadians(eps);

        double y = Math.cos(theta);
        double x = -(Math.sin(theta) * Math.cos(e)
                + Math.tan(phi) * Math.sin(e));
        return normalize(Math.toDegrees(Math.atan2(y, x)));
    }

    static double meanObliquityDegrees(double julianCenturiesFromJ2000) {
        double t = julianCenturiesFromJ2000;
        double arcsec = 84381.448
                - 46.8150 * t
                - 0.00059 * t * t
                + 0.001813 * t * t * t;
        return arcsec / 3600.0;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(clean(value), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception error) {
            throw new IllegalArgumentException("生日格式請使用 yyyy-MM-dd", error);
        }
    }

    private static LocalTime parseTime(String value) {
        String clean = clean(value);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("印度星盤需要精確出生時間");
        }
        try {
            return LocalTime.parse(clean, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (Exception error) {
            throw new IllegalArgumentException("出生時間格式請使用 HH:mm", error);
        }
    }

    private static ZonedDateTime resolveBirthTime(LocalDateTime local, ZoneId zone) {
        ZoneRules rules = zone.getRules();
        List<ZoneOffset> offsets = rules.getValidOffsets(local);
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException(
                    "出生時間落在夏令時間跳過區段，請確認時間或改用明確 UTC offset");
        }
        if (offsets.size() > 1 && !(zone instanceof ZoneOffset)) {
            throw new IllegalArgumentException(
                    "出生時間落在夏令時間重複區段，請把時區改成當時明確 UTC offset，例如 -04:00");
        }
        try {
            return ZonedDateTime.ofStrict(local, offsets.get(0), zone);
        } catch (DateTimeException error) {
            throw new IllegalArgumentException("出生地時區與時間無法對應", error);
        }
    }

    private static Time astronomyTime(Instant instant) {
        LocalDateTime utc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        double second = utc.getSecond() + utc.getNano() / 1_000_000_000.0;
        return new Time(
                utc.getYear(),
                utc.getMonthValue(),
                utc.getDayOfMonth(),
                utc.getHour(),
                utc.getMinute(),
                second);
    }

    private static double tropicalLongitude(Body body, Time time) {
        if (body == Body.Sun) {
            Ecliptic sun = Astronomy.sunPosition(time);
            return normalize(sun.getElon());
        }
        if (body == Body.Moon) {
            Spherical moon = Astronomy.eclipticGeoMoon(time);
            return normalize(moon.getLon());
        }
        Ecliptic ecliptic = Astronomy.ecliptic(
                Astronomy.geoVector(body, time, Aberration.Corrected));
        return normalize(ecliptic.getElon());
    }

    private static boolean isRetrograde(Body body, Time time) {
        double before = tropicalLongitude(body, time.addDays(-0.5));
        double after = tropicalLongitude(body, time.addDays(0.5));
        return signedAngularDelta(after, before) < 0.0;
    }

    private static Position positionOf(double longitude) {
        double value = normalize(longitude);
        int signIndex = Math.min(11, (int) Math.floor(value / 30.0));
        double degreeInSign = value - signIndex * 30.0;
        int nakshatraIndex = Math.min(26, (int) Math.floor(value / NAKSHATRA_SIZE));
        double withinNakshatra = value - nakshatraIndex * NAKSHATRA_SIZE;
        int pada = Math.min(4, (int) Math.floor(withinNakshatra / PADA_SIZE) + 1);
        return new Position(
                value,
                signIndex,
                SIGNS[signIndex],
                degreeInSign,
                nakshatraIndex,
                NAKSHATRAS[nakshatraIndex],
                pada);
    }

    private static int wholeSignHouse(int lagnaSignIndex, int planetSignIndex) {
        return ((planetSignIndex - lagnaSignIndex + 12) % 12) + 1;
    }

    private static Map<String, Object> planetMap(
            String name,
            double tropical,
            double sidereal,
            Position p,
            int house,
            boolean retrograde,
            String dignity) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("name", name);
        out.put("tropicalLongitude", round(normalize(tropical), 6));
        out.put("siderealLongitude", round(normalize(sidereal), 6));
        out.put("signIndex", p.signIndex);
        out.put("sign", p.sign);
        out.put("degreeInSign", round(p.degreeInSign, 6));
        out.put("house", house);
        out.put("nakshatraIndex", p.nakshatraIndex);
        out.put("nakshatra", p.nakshatra);
        out.put("pada", p.pada);
        out.put("retrograde", retrograde);
        out.put("dignity", dignity);
        return out;
    }

    private static List<Map<String, Object>> buildHouses(
            int lagnaSignIndex,
            List<Map<String, Object>> planets) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int house = 1; house <= 12; house++) {
            int signIndex = (lagnaSignIndex + house - 1) % 12;
            List<String> occupants = new ArrayList<String>();
            for (Map<String, Object> planet : planets) {
                if (intValue(planet.get("house")) == house) {
                    occupants.add(text(planet.get("name")));
                }
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("house", house);
            item.put("signIndex", signIndex);
            item.put("sign", SIGNS[signIndex]);
            item.put("lord", SIGN_LORDS[signIndex]);
            item.put("planets", occupants);
            out.add(item);
        }
        return out;
    }

    private static List<Map<String, Object>> buildHouseLords(int lagnaSignIndex) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int house = 1; house <= 12; house++) {
            int signIndex = (lagnaSignIndex + house - 1) % 12;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("house", house);
            item.put("sign", SIGNS[signIndex]);
            item.put("lord", SIGN_LORDS[signIndex]);
            out.add(item);
        }
        return out;
    }

    private static List<Map<String, Object>> buildAspects(List<Map<String, Object>> planets) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> planet : planets) {
            String name = text(planet.get("name"));
            if ("Rahu".equals(name) || "Ketu".equals(name)) continue;
            int house = intValue(planet.get("house"));
            List<Integer> offsets = new ArrayList<Integer>();
            offsets.add(6);
            if ("Mars".equals(name)) {
                offsets.add(3);
                offsets.add(7);
            } else if ("Jupiter".equals(name)) {
                offsets.add(4);
                offsets.add(8);
            } else if ("Saturn".equals(name)) {
                offsets.add(2);
                offsets.add(9);
            }
            Collections.sort(offsets);
            for (Integer offset : offsets) {
                int targetHouse = ((house - 1 + offset) % 12) + 1;
                List<String> targets = new ArrayList<String>();
                for (Map<String, Object> other : planets) {
                    if (intValue(other.get("house")) == targetHouse) {
                        targets.add(text(other.get("name")));
                    }
                }
                Map<String, Object> aspect = new LinkedHashMap<String, Object>();
                aspect.put("from", name);
                aspect.put("fromHouse", house);
                aspect.put("toHouse", targetHouse);
                aspect.put("distance", offset + 1);
                aspect.put("targetPlanets", targets);
                out.add(aspect);
            }
        }
        return out;
    }

    private static List<Map<String, Object>> buildConjunctions(List<Map<String, Object>> planets) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < planets.size(); i++) {
            Map<String, Object> a = planets.get(i);
            for (int j = i + 1; j < planets.size(); j++) {
                Map<String, Object> b = planets.get(j);
                if (intValue(a.get("signIndex")) != intValue(b.get("signIndex"))) continue;
                double sep = angularSeparation(
                        doubleValue(a.get("siderealLongitude")),
                        doubleValue(b.get("siderealLongitude")));
                if (sep > 8.0) continue;
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("a", text(a.get("name")));
                item.put("b", text(b.get("name")));
                item.put("sign", text(a.get("sign")));
                item.put("house", a.get("house"));
                item.put("separationDegrees", round(sep, 4));
                out.add(item);
            }
        }
        return out;
    }

    private static String dignity(String planet, int signIndex) {
        if ("Sun".equals(planet)) return dignityBySign(signIndex, 0, 6, 4);
        if ("Moon".equals(planet)) return dignityBySign(signIndex, 1, 7, 3);
        if ("Mars".equals(planet)) return dignityBySign(signIndex, 9, 3, 0, 7);
        if ("Mercury".equals(planet)) return dignityBySign(signIndex, 5, 11, 2, 5);
        if ("Jupiter".equals(planet)) return dignityBySign(signIndex, 3, 9, 8, 11);
        if ("Venus".equals(planet)) return dignityBySign(signIndex, 11, 5, 1, 6);
        if ("Saturn".equals(planet)) return dignityBySign(signIndex, 6, 0, 9, 10);
        return "neutral";
    }

    private static String dignityBySign(
            int signIndex,
            int exalted,
            int debilitated,
            int... own) {
        if (signIndex == exalted) return "exalted";
        if (signIndex == debilitated) return "debilitated";
        for (int value : own) if (signIndex == value) return "own_sign";
        return "neutral";
    }

    private static DashaData buildVimshottari(
            Instant birth,
            Instant now,
            ZoneId zone,
            int moonNakshatraIndex,
            double moonLongitude) {
        int startLordIndex = moonNakshatraIndex % DASHA_ORDER.length;
        double withinNakshatra =
                normalize(moonLongitude) - moonNakshatraIndex * NAKSHATRA_SIZE;
        double elapsedFraction = Math.max(0.0,
                Math.min(1.0, withinNakshatra / NAKSHATRA_SIZE));
        double elapsedYears = DASHA_YEARS[startLordIndex] * elapsedFraction;
        Instant cursor = addYears(birth, -elapsedYears);

        List<Map<String, Object>> maha = new ArrayList<Map<String, Object>>();
        Map<String, Object> currentMd = null;
        Map<String, Object> currentAd = null;
        List<Map<String, Object>> futureAntars = new ArrayList<Map<String, Object>>();

        for (int step = 0; step < DASHA_ORDER.length; step++) {
            int lordIndex = (startLordIndex + step) % DASHA_ORDER.length;
            String lord = DASHA_ORDER[lordIndex];
            double years = DASHA_YEARS[lordIndex];
            Instant end = addYears(cursor, years);

            List<Map<String, Object>> antars = new ArrayList<Map<String, Object>>();
            Instant adCursor = cursor;
            for (int adStep = 0; adStep < DASHA_ORDER.length; adStep++) {
                int adIndex = (lordIndex + adStep) % DASHA_ORDER.length;
                String adLord = DASHA_ORDER[adIndex];
                double adYears = years * DASHA_YEARS[adIndex] / 120.0;
                Instant adEnd = adStep == DASHA_ORDER.length - 1
                        ? end : addYears(adCursor, adYears);
                Map<String, Object> ad = periodMap(
                        adLord, adCursor, adEnd, birth, zone);
                ad.put("mahadashaLord", lord);
                antars.add(ad);

                if (!now.isBefore(adCursor) && now.isBefore(adEnd)) {
                    currentAd = ad;
                }
                if (adEnd.isAfter(now)) {
                    futureAntars.add(ad);
                }
                adCursor = adEnd;
            }

            Map<String, Object> md = periodMap(lord, cursor, end, birth, zone);
            md.put("durationYears", years);
            md.put("antardashas", antars);
            maha.add(md);
            if (!now.isBefore(cursor) && now.isBefore(end)) {
                currentMd = md;
            }
            cursor = end;
        }

        List<Map<String, Object>> important = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> ad : futureAntars) {
            important.add(compactPeriod(ad));
            if (important.size() >= 4) break;
        }

        DashaData data = new DashaData();
        data.mahadashaTimeline = maha;
        data.currentMahadasha = currentMd == null
                ? Collections.<String, Object>emptyMap()
                : compactMahadasha(currentMd);
        data.currentAntardasha = currentAd == null
                ? Collections.<String, Object>emptyMap()
                : compactPeriod(currentAd);
        data.importantPeriods = important;
        return data;
    }

    private static Map<String, Object> periodMap(
            String lord,
            Instant start,
            Instant end,
            Instant birth,
            ZoneId zone) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lord", lord);
        out.put("startDate", localDate(start, zone));
        out.put("endDate", localDate(end, zone));
        out.put("startAge", round(ageYears(birth, start), 2));
        out.put("endAge", round(ageYears(birth, end), 2));
        return out;
    }

    private static Map<String, Object> compactMahadasha(Map<String, Object> source) {
        Map<String, Object> out = compactPeriod(source);
        out.put("durationYears", source.get("durationYears"));
        return out;
    }

    private static Map<String, Object> compactPeriod(Map<String, Object> source) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lord", source.get("lord"));
        if (source.containsKey("mahadashaLord")) {
            out.put("mahadashaLord", source.get("mahadashaLord"));
        }
        out.put("startDate", source.get("startDate"));
        out.put("endDate", source.get("endDate"));
        out.put("startAge", source.get("startAge"));
        out.put("endAge", source.get("endAge"));
        return out;
    }

    private static Instant addYears(Instant base, double years) {
        double seconds = years * VIMSHOTTARI_YEAR_DAYS * 86400.0;
        return base.plusMillis(Math.round(seconds * 1000.0));
    }

    private static double ageYears(Instant birth, Instant value) {
        double seconds = (value.toEpochMilli() - birth.toEpochMilli()) / 1000.0;
        return seconds / (VIMSHOTTARI_YEAR_DAYS * 86400.0);
    }

    private static String localDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate().toString();
    }

    private static Map<String, Object> findPlanet(
            List<Map<String, Object>> planets,
            String name) {
        for (Map<String, Object> planet : planets) {
            if (name.equals(text(planet.get("name")))) return planet;
        }
        throw new IllegalStateException("Missing planet " + name);
    }

    private static double angularSeparation(double a, double b) {
        double diff = Math.abs(normalize(a) - normalize(b));
        return diff > 180.0 ? 360.0 - diff : diff;
    }

    private static double signedAngularDelta(double after, double before) {
        double diff = normalize(after - before);
        return diff > 180.0 ? diff - 360.0 : diff;
    }

    private static double normalize(double value) {
        double out = value % 360.0;
        if (out < 0.0) out += 360.0;
        return out;
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10.0, scale);
        return Math.round(value * factor) / factor;
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(text(value)); }
        catch (Exception ignored) { return 0; }
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(text(value)); }
        catch (Exception ignored) { return 0.0; }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class PlanetSeed {
        final String name;
        final Body body;
        final boolean checkRetrograde;

        PlanetSeed(String name, Body body, boolean checkRetrograde) {
            this.name = name;
            this.body = body;
            this.checkRetrograde = checkRetrograde;
        }
    }

    private static final class Position {
        final double longitude;
        final int signIndex;
        final String sign;
        final double degreeInSign;
        final int nakshatraIndex;
        final String nakshatra;
        final int pada;

        Position(
                double longitude,
                int signIndex,
                String sign,
                double degreeInSign,
                int nakshatraIndex,
                String nakshatra,
                int pada) {
            this.longitude = longitude;
            this.signIndex = signIndex;
            this.sign = sign;
            this.degreeInSign = degreeInSign;
            this.nakshatraIndex = nakshatraIndex;
            this.nakshatra = nakshatra;
            this.pada = pada;
        }
    }

    private static final class DashaData {
        List<Map<String, Object>> mahadashaTimeline;
        Map<String, Object> currentMahadasha;
        Map<String, Object> currentAntardasha;
        List<Map<String, Object>> importantPeriods;
    }
}
