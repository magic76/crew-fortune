package com.crewpocket.fortune;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FortuneEngine {
    public FortuneFacts calculateFacts(FortuneMode mode, FortuneProfile profile, Date now) {
        if (mode == null) throw new IllegalArgumentException("mode is required");
        if (profile == null || profile.name.isEmpty()) throw new IllegalArgumentException("name is required");

        ParsedBirth birth = parseBirth(profile.birthDate);
        int life = digitalRootDigits(profile.birthDate);
        int nameNumber = digitalRootCodePoints(profile.name);
        String zodiac = zodiac(birth.month, birth.day);
        String dayKey = mode == FortuneMode.TODAY
                ? new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now == null ? new Date() : now)
                : "stable";
        int seed = stableHash(mode.name() + "|" + profile.name + "|" + profile.birthDate + "|"
                + profile.birthTime + "|" + profile.secondaryName + "|" + dayKey);

        int secondaryNameNumber = 0;
        int score = 42 + Math.abs(seed % 55);
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        String basis;

        if (mode == FortuneMode.BA_ZI) {
            details.putAll(new BaZiCalculator().calculate(profile.birthDate, profile.birthTime));
            score = intValue(details.get("fiveElementBalance"), 50);
            basis = "四柱 " + details.get("fourPillars")
                    + " · 日主 " + details.get("dayMaster") + details.get("dayMasterElement");
        } else if (mode == FortuneMode.TAROT_NUMEROLOGY) {
            details.putAll(new TarotNumerologyCalculator().calculate(profile.birthDate));
            score = intValue(details.get("lifePathNumber"), life);
            basis = "生命靈數 " + details.get("lifePathNumber")
                    + " · 出生牌 " + details.get("birthCardNumber") + " " + details.get("birthCardName");
        } else {
            if (mode == FortuneMode.COMPATIBILITY) {
                if (profile.secondaryName.isEmpty()) {
                    throw new IllegalArgumentException("合盤需要第二個名字");
                }
                secondaryNameNumber = digitalRootCodePoints(profile.secondaryName);
                score = 55 + (9 - Math.abs(nameNumber - secondaryNameNumber)) * 5;
                score = Math.max(45, Math.min(96, score + (seed % 7)));
            }
            basis = "生命靈數 " + life + " · " + zodiac + " · 名字數 " + nameNumber;
            if (secondaryNameNumber > 0) basis += " · 對方名字數 " + secondaryNameNumber;
        }

        return new FortuneFacts(
                mode,
                score,
                basis,
                life,
                zodiac,
                nameNumber,
                secondaryNameNumber,
                dimension(seed, 11),
                dimension(seed, 23),
                dimension(seed, 37),
                dimension(seed, 53),
                dayKey,
                details);
    }

    public FortuneResult calculate(FortuneMode mode, FortuneProfile profile, Date now) {
        FortuneFacts facts = calculateFacts(mode, profile, now);
        int seed = stableHash(mode.name() + "|" + profile.name + "|" + profile.birthDate + "|"
                + profile.secondaryName + "|" + facts.dayKey);

        switch (mode) {
            case TODAY:
                return today(facts.score, seed, facts.basis);
            case BA_ZI:
                return baZiFallback(facts, seed);
            case TAROT_NUMEROLOGY:
                return tarotFallback(facts, seed);
            case PERSONALITY:
                return personality(facts.score, seed, facts.basis, facts.lifeNumber);
            case WEALTH:
                return wealth(facts.score, seed, facts.basis, facts.nameNumber);
            case LOVE_BUG:
                return love(facts.score, seed, facts.basis, facts.zodiac);
            case COMPATIBILITY:
                return compatibility(facts.score, seed, facts.basis, profile.secondaryName);
            default:
                throw new IllegalStateException("unsupported mode");
        }
    }

    private FortuneResult baZiFallback(FortuneFacts facts, int seed) {
        String dayMaster = facts.detailText("dayMaster");
        String element = facts.detailText("dayMasterElement");
        String strongest = facts.detailText("strongestVisibleElement");
        String weakest = facts.detailText("weakestVisibleElement");
        String pillars = facts.detailText("fourPillars");
        return new FortuneResult(
                FortuneMode.BA_ZI,
                facts.score,
                "你的日主是「" + dayMaster + element + "」",
                facts.basis,
                "四柱為 " + pillars + "。目前先以可驗證的四柱、可見五行、藏干與十神結構做基礎解讀，不把門派差異很大的喜用神硬算成唯一答案。",
                "表面上是在排八字，實際上宇宙只是把你的設定檔拆成四欄給你看。",
                "可見五行目前以「" + strongest + "」較多、「" + weakest + "」較少；這是分布描述，不等於吉凶判決。",
                "先把命盤當成觀察自己的另一種語言，不要拿它代替現實決策。");
    }

    private FortuneResult tarotFallback(FortuneFacts facts, int seed) {
        String card = facts.detailText("birthCardName");
        String number = facts.detailText("birthCardNumber");
        String keywords = facts.detailText("birthCardKeywords");
        String lifePath = facts.detailText("lifePathNumber");
        return new FortuneResult(
                FortuneMode.TAROT_NUMEROLOGY,
                facts.score,
                "你的出生牌：(" + number + ") " + card,
                facts.basis,
                "生命靈數為 " + lifePath + "，塔羅出生牌核心關鍵字是「" + keywords + "」。這是固定生日計算，不是每次隨機抽牌。",
                "你的人生不是被一張牌控制，只是這張牌很會搶著當你的年度形象顧問。",
                "同一個生日會得到同一張出生牌；今天抽到什麼心情，則是另一回事。",
                "把牌義當成反思提示，不要當成宇宙替你簽好的合約。");
    }

    private FortuneResult today(int score, int seed, String basis) {
        String[] titles = {"今天宜低調發光", "宇宙今天有回你訊息", "今日運勢：可衝但別全衝", "今天的你有主角光環"};
        String[] analysis = {
                "今天的節奏偏向先觀察再出手，臨時起意反而容易浪費力氣。",
                "今天適合處理卡很久的小事，完成感會比開新坑更有回報。",
                "人際運比單打獨鬥更順，適合問一句、借一手、少硬撐。",
                "判斷力在線，但衝動也在線；先做重要的，再做想做的。"
        };
        String[] trans = {
                "今天不是不能衝，是先確認你衝的方向不是牆。",
                "宇宙批准你努力，但沒有批准你一次開八個新坑。",
                "今天有人能幫你，前提是你願意開口，不要演獨立電影。",
                "你的直覺今天可以信八成，剩下兩成請交給常識。"
        };
        return result(FortuneMode.TODAY, score, seed, basis, titles, analysis, trans,
                new String[]{"真正的幸運物可能是充飽電的手機。","你最大的阻力目前看起來像是你自己。","今天少看一次購物車，財運會立刻改善。","命很好，待辦清單比較不好。"},
                new String[]{"先完成一件最重要的事。","重要訊息晚三秒再送出。","有人可問就別硬猜。","晚上不要突然決定改變人生。"});
    }

    private FortuneResult personality(int score, int seed, String basis, int life) {
        String[] titles = {"外冷內建聊天室型", "表面淡定，內心開會型", "理性外殼，戲很多核心", "可靠到忘了自己也會累"};
        String[] analysis = {
                "你傾向先理解局面再表態，不喜歡把不確定的情緒直接丟出去。",
                "你對細節的敏感度高，常常比別人更早發現氣氛改變。",
                "你習慣自己消化問題，因此別人常低估你腦內正在跑多少流程。",
                "你在熟人面前和陌生人面前可能像兩個版本，安全感會大幅改變輸出。"
        };
        String[] trans = {
                "別人以為你沒事，其實你腦內已經開到第 " + (life + 2) + " 次會議。",
                "你不是想太多，你只是把別人沒想的份也順便想完。",
                "你的沉默通常不是空白，是背景運算。",
                "熟了之後才知道，你的人設其實只是試用版。"
        };
        return result(FortuneMode.PERSONALITY, score, seed, basis, titles, analysis, trans,
                new String[]{"你的內心戲沒有觀眾，但製作費很高。","你不是難懂，只是說明書藏得比較深。","偶爾直接講，比讓別人通靈便宜。","你最大的反差通常發生在『熟了以後』。"},
                new String[]{"把一件心事講成人話。","今天少替別人預測一次反應。","不確定就問，不要自己寫劇本。","允許自己偶爾沒那麼可靠。"});
    }

    private FortuneResult wealth(int score, int seed, String basis, int nameNumber) {
        String[] titles = {"有進財命，也有手滑命", "財神有來，只是停不久", "適合累積，不適合暴衝", "錢跟你有緣，跟購物車也有"};
        String[] analysis = {
                "你的財運比較吃長期累積與紀律，不太適合靠一次性的情緒決策。",
                "你對『值得』的東西容易放寬預算，因此真正的關鍵不是收入，而是判斷值不值得。",
                "你在看得到進度時更容易持續，因此分段目標會比模糊的存錢更有效。",
                "你的財務優勢在選擇能力，弱點則是偶爾把想要說服成需要。"
        };
        String[] trans = {
                "你不是存不到錢，是錢常來你帳戶觀光。",
                "你很會研究 CP 值，研究完通常還是買比較貴的那個。",
                "財神對你有期待，購物平台也對你有期待。",
                "你的財運數字是 " + nameNumber + "，但信用卡不接受命理辯護。"
        };
        return result(FortuneMode.WEALTH, score, seed, basis, titles, analysis, trans,
                new String[]{"宇宙目前沒有找到『買了就等於省』的證據。","投資自己可以，但購物車不一定是自己。","真正的招財物是沒有忘記取消的訂閱。","錢不是消失，只是換成你家裡的東西。"},
                new String[]{"今天想買的東西先放 24 小時。","先存再花，不要反過來。","刪掉一個沒在用的訂閱。","大額決策不要在半夜做。"});
    }

    private FortuneResult love(int score, int seed, String basis, String zodiac) {
        String[] titles = {"慢熱型自我攻略王", "嘴上隨緣，心裡逐格分析", "訊息已讀研究員", "戀愛系統：敏感度過高"};
        String[] analysis = {
                "你需要安全感才會真正投入，因此前期容易觀察過久。",
                "你對互動細節很敏感，小變化很容易被你讀成大訊號。",
                "你不喜歡太失控的感情節奏，確認感通常比刺激感更重要。",
                "你在意對方是否穩定回應，而不是單次很浪漫。"
        };
        String[] trans = {
                "對方少打一個愛心，你已經把 " + zodiac + " 本週運勢查完。",
                "嘴巴說順其自然，腦袋正在跑關係風險模型。",
                "人家可能只是忙，你已經推演到三年後。",
                "你不是沒桃花，你只是審核流程比較像上市公司。"
        };
        return result(FortuneMode.LOVE_BUG, score, seed, basis, titles, analysis, trans,
                new String[]{"有些訊號不是訊號，只是對方手機剩 3%。","感情不是 API，不是每次都會回 200。","少腦補一點，伺服器壓力會比較小。","不是每個句點都代表感情句點。"},
                new String[]{"有疑問就問一次，不要腦補十次。","看長期行為，不看單次訊息。","別用沉默測試別人的讀心術。","今天先把手機放下十分鐘。"});
    }

    private FortuneResult compatibility(int score, int seed, String basis, String other) {
        String[] titles = {"互補型共犯組合", "一個踩油門，一個找煞車", "嘴上互嫌，實際很搭", "奇怪但有效的組合"};
        String[] analysis = {
                "你們的差異會製造摩擦，也會補上彼此容易漏掉的地方。",
                "這段關係的優勢不是完全相同，而是遇到事情時容易形成分工。",
                "熟悉之後你們會建立自己的溝通捷徑，外人不一定看得懂。",
                "真正的相容點在於能不能容忍彼此節奏，而不是興趣是否完全一致。"
        };
        String[] trans = {
                "你跟 " + other + " 的默契像藍牙：偶爾斷線，但通常會自己連回來。",
                "一個負責說『走啊』，另一個負責問『等等，去哪？』。",
                "你們不是沒有摩擦，只是吵完通常還是一起吃飯。",
                "別人看不懂你們為什麼合，你們自己有時候也看不懂。"
        };
        return result(FortuneMode.COMPATIBILITY, score, seed, basis, titles, analysis, trans,
                new String[]{"關係能維持，不代表誰比較正常。","真正的默契是知道對方哪句話可以不用理。","你們最大的共同點，可能是都覺得自己比較有道理。","宇宙判定：可以相處，請定期更新韌體。"},
                new String[]{"有事講清楚，不要考對方。","保留差異，不用硬變一樣。","一起做一件沒有目的的小事。","今天少爭一次誰記得比較清楚。"});
    }

    private FortuneResult result(FortuneMode mode, int score, int seed, String basis,
                                 String[] titles, String[] analysis, String[] translation,
                                 String[] punchline, String[] advice) {
        int i = seed & Integer.MAX_VALUE;
        return new FortuneResult(mode, score,
                titles[i % titles.length],
                basis,
                analysis[(i / 3) % analysis.length],
                translation[(i / 5) % translation.length],
                punchline[(i / 7) % punchline.length],
                advice[(i / 11) % advice.length]);
    }

    private ParsedBirth parseBirth(String raw) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date date = format.parse(raw);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new ParsedBirth(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        } catch (Exception error) {
            throw new IllegalArgumentException("生日格式請用 yyyy-MM-dd");
        }
    }

    private int digitalRootDigits(String value) {
        int sum = 0;
        for (int i = 0; i < value.length(); i++) if (Character.isDigit(value.charAt(i))) sum += value.charAt(i) - '0';
        return digitalRoot(sum);
    }

    private int digitalRootCodePoints(String value) {
        int sum = 0;
        for (int i = 0; i < value.length(); i++) sum += value.charAt(i);
        return digitalRoot(sum);
    }

    private int digitalRoot(int number) {
        number = Math.abs(number);
        while (number > 9) {
            int next = 0;
            while (number > 0) { next += number % 10; number /= 10; }
            number = next;
        }
        return number == 0 ? 9 : number;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    private int dimension(int seed, int salt) {
        return 30 + ((stableHash(seed + "|" + salt) & Integer.MAX_VALUE) % 66);
    }

    private int stableHash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return ((digest[0] & 0xff) << 24) | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8) | (digest[3] & 0xff);
        } catch (Exception error) {
            return input.hashCode();
        }
    }

    private String zodiac(int month, int day) {
        String[] signs = {"摩羯座","水瓶座","雙魚座","牡羊座","金牛座","雙子座","巨蟹座","獅子座","處女座","天秤座","天蠍座","射手座","摩羯座"};
        int[] edge = {20,19,21,20,21,22,23,23,23,24,23,22};
        return day < edge[month - 1] ? signs[month - 1] : signs[month];
    }

    private static final class ParsedBirth {
        final int month;
        final int day;
        ParsedBirth(int month, int day) { this.month = month; this.day = day; }
    }
}
