package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.AbstractHologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.impl.HologramHelper;
import eu.pb4.holograms.mixin.accessors.EntityAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;


import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ImageAbstractHologramElement extends AbstractHologramElement {
    protected final static double TEXT_HEIGHT = 0.22;

    private static final String PIXEL_STRING = "█";
    private static final String TRANSPARENCY_STRING = "▒";

    protected final ArrayList<Text> texts = new ArrayList<>();

    //private int frame = 0;
    //private final ArrayList<ArrayList<Text>> frames = new ArrayList<>();

    protected final ArrayList<UUID> uuids = new ArrayList<>();
    protected final int holoHeight;

    public ImageAbstractHologramElement(BufferedImage image, int holoHeight, boolean smooth) {
        this.height = holoHeight * TEXT_HEIGHT;
        this.holoHeight = holoHeight;

        toText(this.texts, image, holoHeight, smooth);

        /*try {
            var grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(FabricLoader.getInstance().getGameDir().resolve("video.mp4").toFile()));


            while (true) {

                try {
                    Picture picture;

                    if (!(null != (picture = grab.getNativeFrame()))) break;

                    var x = new ArrayList<Text>();
                    toText(x, AWTUtil.toBufferedImage(picture), holoHeight, smooth);
                    this.frames.add(x);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }*/

        for (int i = 0; i < holoHeight; i++) {
            this.entityIds.add(EntityAccessor.getMaxEntityId().incrementAndGet());
            this.uuids.add(HologramHelper.getUUID());
        }
    }

    private static Text getPixel(int argba) {
        int alpha = argba >> 24;
        if (alpha == 0x00) return Text.literal("▒").formatted(Formatting.BLACK);

        TextColor color = TextColor.fromRgb(argba & 0xFFFFFF);
        return Text.literal(alpha >= 10 ? TRANSPARENCY_STRING : PIXEL_STRING).styled(style -> style.withColor(color));
    }

    public static void toText(List<Text> texts, BufferedImage image, int holoHeight, boolean smooth) {
        var stepSize = image.getHeight() / (double) holoHeight;

        var pixels = new Int2ObjectOpenHashMap<Text>();
        var width = image.getWidth();
        var height = image.getHeight();


        if (smooth && stepSize > 1) {
            for (double y = 0; y < height; y += stepSize) {
                var text = Text.empty();
                for (double x = 0; x < width; x += stepSize) {
                    int a = 0;
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    int pixelCount = 0;

                    for (double xi = 0; xi < stepSize; xi++) {
                        for (double yi = 0; yi < stepSize; yi++) {
                            pixelCount++;
                            int argba = image.getRGB((int) Math.min(x + xi, width - 1), (int) Math.min(y + yi, height - 1));

                            a += ColorHelper.Argb.getAlpha(argba);
                            r += ColorHelper.Argb.getRed(argba);
                            g += ColorHelper.Argb.getGreen(argba);
                            b += ColorHelper.Argb.getBlue(argba);
                        }
                    }

                    text.append(pixels.computeIfAbsent(ColorHelper.Argb.getArgb((a / pixelCount) & 0xFF, (r / pixelCount) & 0xFF, (g / pixelCount) & 0xFF, (b / pixelCount) & 0xFF), ImageAbstractHologramElement::getPixel));
                }
                texts.add(text);
            }
        } else {
            for (double y = 0; y < height; y += stepSize) {
                var text = Text.empty();
                for (double x = 0; x < width; x += stepSize) {
                    int argba = image.getRGB((int) x, (int) y);
                    text.append(pixels.computeIfAbsent(argba, ImageAbstractHologramElement::getPixel));
                }
                texts.add(text);
            }
        }
    }

    /*@Override
    public void onTick(AbstractHologram hologram) {
        var list = this.frames.get(this.frame);
        this.frame += 2;
        if (this.frame >= this.frames.size()) {
            this.frame = 0;
        }

        for (int i = 0; i < this.holoHeight; i++) {
            var packet = HologramHelper.createUnsafe(EntityTrackerUpdateS2CPacket.class);
            var accessor = (EntityTrackerUpdateS2CPacketAccessor) packet;

            accessor.setId(this.entityIds.getInt(i));
            List<DataTracker.Entry<?>> data = new ArrayList<>();
            data.add(new DataTracker.Entry<>(EntityAccessor.getCustomName(), Optional.of(list.get(i))));
            accessor.setTrackedValues(data);

            for (var player : hologram.getPlayerSet()) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }*/

    protected double getHeightDifference(int i, AbstractHologram hologram) {
        return (hologram.getAlignment() == AbstractHologram.VerticalAlign.BOTTOM ? 0 : this.height) - i * TEXT_HEIGHT;
    }
}
