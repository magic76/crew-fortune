package com.crewpocket.fortune;

import android.graphics.Color;

/**
 * Warm light visual system for the primary Crew Fortune surfaces.
 * Legacy renderers continue to consume MainActivity aliases so the migration
 * can stay visual-only without touching calculation or interpretation logic.
 */
final class FortuneTheme {
    private FortuneTheme() {}

    static final int BG = Color.rgb(247, 247, 244);
    static final int SURFACE = Color.WHITE;
    static final int SURFACE_ALT = Color.rgb(244, 243, 251);
    static final int TEXT = Color.rgb(32, 33, 36);
    static final int MUTED = Color.rgb(118, 118, 118);
    static final int LINE = Color.rgb(232, 231, 226);

    static final int BRAND = Color.rgb(106, 90, 224);
    static final int BRAND_SOFT = Color.rgb(238, 236, 255);
    static final int BRAND_LINE = Color.rgb(210, 205, 247);

    static final int GOLD = Color.rgb(184, 132, 43);
    static final int GOLD_SOFT = Color.rgb(255, 247, 228);

    static final int BAZI = Color.rgb(199, 119, 37);
    static final int BAZI_SOFT = Color.rgb(255, 243, 232);

    static final int TAROT = Color.rgb(43, 164, 113);
    static final int TAROT_SOFT = Color.rgb(234, 247, 240);

    static final int VEDIC = Color.rgb(63, 120, 215);
    static final int VEDIC_SOFT = Color.rgb(234, 242, 255);

    static final int ROSE = Color.rgb(197, 92, 130);
    static final int ROSE_SOFT = Color.rgb(253, 239, 245);
    static final int SKY = Color.rgb(63, 120, 215);
    static final int SKY_SOFT = Color.rgb(234, 242, 255);
    static final int TEAL = Color.rgb(43, 164, 113);
    static final int TEAL_SOFT = Color.rgb(234, 247, 240);
}
