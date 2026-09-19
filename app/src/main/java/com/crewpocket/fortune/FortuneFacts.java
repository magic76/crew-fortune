package com.crewpocket.fortune;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FortuneFacts {
    public final FortuneMode mode;
    public final int score;
    public final String basis;
    public final int lifeNumber;
    public final String zodiac;
    public final int nameNumber;
    public final int secondaryNameNumber;
    public final int momentum;
    public final int stability;
    public final int social;
    public final int impulse;
    public final String dayKey;
    public final Map<String, Object> details;

    public FortuneFacts(FortuneMode mode,
                        int score,
                        String basis,
                        int lifeNumber,
                        String zodiac,
                        int nameNumber,
                        int secondaryNameNumber,
                        int momentum,
                        int stability,
                        int social,
                        int impulse,
                        String dayKey) {
        this(mode, score, basis, lifeNumber, zodiac, nameNumber, secondaryNameNumber,
                momentum, stability, social, impulse, dayKey, Collections.<String, Object>emptyMap());
    }

    public FortuneFacts(FortuneMode mode,
                        int score,
                        String basis,
                        int lifeNumber,
                        String zodiac,
                        int nameNumber,
                        int secondaryNameNumber,
                        int momentum,
                        int stability,
                        int social,
                        int impulse,
                        String dayKey,
                        Map<String, Object> details) {
        this.mode = mode;
        this.score = score;
        this.basis = basis;
        this.lifeNumber = lifeNumber;
        this.zodiac = zodiac;
        this.nameNumber = nameNumber;
        this.secondaryNameNumber = secondaryNameNumber;
        this.momentum = momentum;
        this.stability = stability;
        this.social = social;
        this.impulse = impulse;
        this.dayKey = dayKey;
        this.details = Collections.unmodifiableMap(
                details == null ? new LinkedHashMap<String, Object>()
                        : new LinkedHashMap<String, Object>(details));
    }

    public Object detail(String key) {
        return details.get(key);
    }

    public String detailText(String key) {
        Object value = details.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
