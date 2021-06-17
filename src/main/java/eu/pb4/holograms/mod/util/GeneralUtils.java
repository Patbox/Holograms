package eu.pb4.holograms.mod.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.minecraft.text.*;

import java.util.List;

public class GeneralUtils {
    public static String textToString(Text text) {
        StringBuffer string = new StringBuffer(text.asString());
        recursiveParsing(string, text.getSiblings());
        return string.toString();
    }

    private static void recursiveParsing(StringBuffer string, List<Text> textList) {
        for (Text text : textList) {
            string.append(text.asString());

            List<Text> siblings = text.getSiblings();
            if (siblings.size() != 0) {
                recursiveParsing(string, siblings);
            }
        }
    }

    public static String durationToString(long x) {
        long seconds = x % 60;
        long minutes = (x / 60) % 60;
        long hours = (x / (60 * 60)) % 24;
        long days = x / (60 * 60 * 24);

        if (days > 0) {
            return String.format("%dd%dh%dm%ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh%dm%ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm%ds", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%ds", seconds);
        } else {
            return "---";
        }
    }

    public static MutableText toGradient(Text base, Int2ObjectFunction<TextColor> posToColor) {
        return recursiveGradient(base, posToColor, 0).text();
    }

    private static TextLengthPair recursiveGradient(Text base, Int2ObjectFunction<TextColor> posToColor, int pos) {
        MutableText out = new LiteralText("").setStyle(base.getStyle());
        for (String letter : base.asString().split("")) {
            if (!letter.isEmpty()) {
                out.append(new LiteralText(letter).setStyle(Style.EMPTY.withColor(posToColor.apply(pos))));
                pos++;
            }
        }

        for (Text sibling : base.getSiblings()) {
            TextLengthPair pair = recursiveGradient(sibling, posToColor, pos);
            pos = pair.length;
            out.append(pair.text);
        }

        return new TextLengthPair(out, pos);
    }

    public static int hvsToRgb(float hue, float saturation, float value) {
        int h = (int) (hue * 6) % 6;
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        switch (h) {
            case 0: return rgbToInt(value, t, p);
            case 1: return rgbToInt(q, value, p);
            case 2: return rgbToInt(p, value, t);
            case 3: return rgbToInt(p, q, value);
            case 4: return rgbToInt(t, p, value);
            case 5: return rgbToInt(value, p, q);
            default: return 0;
        }
    }

    public static int rgbToInt(float r, float g, float b) {
        return ((int) (r * 0xff)) << 16 | ((int) (g * 0xff)) << 8 | ((int) (b * 0xff));
    }

    public static HSV rgbToHsv(int rgb) {
        float b = (float) (rgb % 256) / 255;
        rgb = rgb >> 8;
        float g = (float) (rgb % 256) / 255;
        rgb = rgb >> 8;
        float r = (float) (rgb % 256) / 255;

        float cmax = Math.max(r, Math.max(g, b));
        float cmin = Math.min(r, Math.min(g, b));
        float diff = cmax - cmin;
        float h = -1, s;

        if (cmax == cmin) {
            h = 0;
        } else if (cmax == r) {
            h = (0.1666f * ((g - b) / diff) + 1) % 1;
        } else if (cmax == g) {
            h = (0.1666f * ((b - r) / diff) + 0.333f) % 1;
        } else if (cmax == b) {
            h = (0.1666f * ((r - g) / diff) + 0.666f) % 1;
        }
        if (cmax == 0) {
            s = 0;
        } else {
            s = (diff / cmax);
        }

        return new HSV(h, s, cmax);
    }

    public static class HSV {
        public float hI;
        public float sI;
        public float vI;

        public HSV(float h, float s, float v) {
            this.hI = h;
            this.sI = s;
            this.vI = v;
        }

        public float h() {
            return hI;
        }

        public float s() {
            return sI;
        }

        public float v() {
            return vI;
        }
    }

    public static class TextLengthPair {
        public MutableText text;
        public int length;
        public static final TextLengthPair EMPTY = new TextLengthPair(null, 0);

        public TextLengthPair(MutableText textI, int length) {
            this.text = textI;
            this.length = length;
        }

        public MutableText text() {
            return text;
        }

        public int length() {
            return length;
        }
    }

    public static class Pair<L, R> {
        L leftI;
        R rightI;

        public Pair(L leftI, R rightI) {
            this.leftI = leftI;
            this.rightI = rightI;
        }

        public L left() {
            return leftI;
        }

        public R right() {
            return rightI;
        }
    }
}
