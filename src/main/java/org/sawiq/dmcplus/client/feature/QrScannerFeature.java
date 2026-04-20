package org.sawiq.dmcplus.client.feature;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class QrScannerFeature {

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dmcplus-qr-scanner");
        thread.setDaemon(true);
        return thread;
    });

    public void scanCurrentFrame(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        try {
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), screenshot -> {
                if (client.player == null) {
                    screenshot.close();
                    return;
                }

                BufferedImage image = toBufferedImage(screenshot);
                screenshot.close();

                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.translatable("message.dmcplus.qr_scanning").formatted(Formatting.GRAY), true);
                    }
                });

                this.executor.submit(() -> this.decodeAsync(client, image));
            });
        } catch (Exception exception) {
            client.player.sendMessage(Text.translatable("message.dmcplus.qr_failed_capture").formatted(Formatting.RED), true);
        }
    }

    private void decodeAsync(MinecraftClient client, BufferedImage image) {
        String decoded = decodeQr(image);
        client.execute(() -> {
            if (client.player == null) {
                return;
            }

            if (decoded == null || decoded.isBlank()) {
                client.player.sendMessage(Text.translatable("message.dmcplus.qr_not_found").formatted(Formatting.RED), false);
                return;
            }

            MutableText resultText;
            if (URL_PATTERN.matcher(decoded).matches()) {
                resultText = Text.literal(decoded).formatted(Formatting.AQUA, Formatting.UNDERLINE)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(decoded)))
                                .withHoverEvent(new HoverEvent.ShowText(Text.translatable("message.dmcplus.qr_open_link")))
                        );
            } else {
                resultText = Text.literal(decoded).formatted(Formatting.GOLD)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(decoded))
                                .withHoverEvent(new HoverEvent.ShowText(Text.translatable("message.dmcplus.qr_copy_text")))
                        );
            }

            client.player.sendMessage(Text.translatable("message.dmcplus.qr_found", resultText), false);
        });
    }

    private static BufferedImage toBufferedImage(NativeImage image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                bufferedImage.setRGB(x, y, image.getColorArgb(x, y));
            }
        }
        return bufferedImage;
    }

    private static String decodeQr(BufferedImage image) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);

        LuminanceSource source = new BufferedImageLuminanceSource(image);
        String result = tryDecode(source, hints);
        if (result != null) {
            return result;
        }

        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        return tryDecode(source, hints);
    }

    private static String tryDecode(LuminanceSource source, Map<DecodeHintType, Object> hints) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            return new MultiFormatReader().decode(bitmap, hints).getText();
        } catch (NotFoundException ignored) {
        }

        try {
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            return new MultiFormatReader().decode(bitmap, hints).getText();
        } catch (NotFoundException ignored) {
        }

        try {
            GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(new MultiFormatReader());
            Result[] results = reader.decodeMultiple(new BinaryBitmap(new HybridBinarizer(source)), hints);
            if (results.length > 0) {
                return results[0].getText();
            }
        } catch (NotFoundException ignored) {
        }

        return null;
    }
}
