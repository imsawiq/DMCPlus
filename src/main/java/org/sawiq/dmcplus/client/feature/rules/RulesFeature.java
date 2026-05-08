package org.sawiq.dmcplus.client.feature.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import org.sawiq.dmcplus.client.ui.RulesScreen;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class RulesFeature {

    private static final String BASE_URL = "https://rules.dmc-minecraft.net";
    private static final String INDEX_URL = BASE_URL + "/~gitbook/site-index";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final Pattern AGENT_INSTRUCTIONS = Pattern.compile("(?s)\\n---\\s*\\n# Agent Instructions:.*$");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private CompletableFuture<List<RulesCategory>> categoriesFuture;
    private List<RulesCategory> categories = List.of();
    private final Map<String, CompletableFuture<String>> pageCache = new LinkedHashMap<>();

    public void open(MinecraftClient client) {
        client.setScreen(new RulesScreen(client.currentScreen, this));
        this.loadCategories();
    }

    public CompletableFuture<List<RulesCategory>> loadCategories() {
        if (this.categoriesFuture != null) {
            return this.categoriesFuture;
        }

        this.categoriesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<RulesCategory> loaded = parseIndex(fetch(INDEX_URL));
                this.categories = loaded;
                return loaded;
            } catch (Exception exception) {
                return List.of();
            }
        });
        return this.categoriesFuture;
    }

    public List<RulesCategory> categories() {
        return this.categories;
    }

    public CompletableFuture<String> loadPage(RulesPage page) {
        CompletableFuture<String> future = this.pageCache.computeIfAbsent(page.path(), path -> CompletableFuture.supplyAsync(() -> {
            try {
                return cleanMarkdown(fetch(BASE_URL + path + ".md"));
            } catch (Exception exception) {
                throw new IllegalStateException("Не удалось загрузить страницу", exception);
            }
        }));
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                this.pageCache.remove(page.path(), future);
            }
        });
        return future;
    }

    public CompletableFuture<String> reloadPage(RulesPage page) {
        this.pageCache.remove(page.path());
        return this.loadPage(page);
    }

    private static List<RulesCategory> parseIndex(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray pages = root.getAsJsonArray("pages");
        Map<String, List<RulesPage>> grouped = new LinkedHashMap<>();

        for (JsonElement element : pages) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject page = element.getAsJsonObject();
            String path = getString(page, "pathname");
            if (path.isBlank() || "/".equals(path)) {
                continue;
            }

            String category = categoryName(page);
            String title = getString(page, "title");
            String description = getString(page, "description");
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>())
                    .add(new RulesPage(title, path, description));
        }

        List<RulesCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<RulesPage>> entry : grouped.entrySet()) {
            categories.add(new RulesCategory(entry.getKey(), List.copyOf(entry.getValue())));
        }
        return List.copyOf(categories);
    }

    private static String categoryName(JsonObject page) {
        if (page.has("breadcrumbs") && page.get("breadcrumbs").isJsonArray()) {
            JsonArray breadcrumbs = page.getAsJsonArray("breadcrumbs");
            if (!breadcrumbs.isEmpty() && breadcrumbs.get(0).isJsonObject()) {
                String label = getString(breadcrumbs.get(0).getAsJsonObject(), "label");
                if (!label.isBlank()) {
                    return label;
                }
            }
        }
        return getString(page, "title");
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json, text/markdown, text/plain");
            connection.setRequestProperty("User-Agent", "DMCPlus-rules");

            if (connection.getResponseCode() != 200) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }

            try (java.io.InputStream input = connection.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String cleanMarkdown(String markdown) {
        String text = AGENT_INSTRUCTIONS.matcher(markdown).replaceAll("");
        text = text.replace("\r\n", "\n");
        text = text.replace("{% hint style=\"danger\" %}", "\n> [Важно]");
        text = text.replace("{% hint style=\"warning\" %}", "\n> [Внимание]");
        text = text.replace("{% hint style=\"info\" %}", "\n> [Инфо]");
        text = text.replace("{% endhint %}", "");
        text = text.replace("<details>", "\n");
        text = text.replace("</details>", "\n");
        text = text.replaceAll("(?s)<summary>(.*?)</summary>", "\n$1\n");
        text = text.replaceAll("\\[(.*?)]\\((.*?)\\)", "$1");
        text = text.replace("**", "");
        text = text.replace("`", "");
        text = text.replace("&#x20;", " ");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&quot;", "\"");
        text = text.replace("&amp;", "&");
        text = HTML_TAG.matcher(text).replaceAll("");
        text = text.replaceAll("(?m)^\\s*[-*]\\s+", "- ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
}
