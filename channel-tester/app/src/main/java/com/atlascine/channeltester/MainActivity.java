package com.atlascine.channeltester;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final String ENDPOINT = "https://config.e-droid.net/srv/config.php";
    private static final String APP_ID = "3713506";
    private static final String SOURCE_PACKAGE = "go.geh";
    private static final String USER_AGENT = "Android Vinebre Software";

    private static final int BG = Color.rgb(7, 9, 14);
    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int PANEL_2 = Color.rgb(25, 30, 42);
    private static final int ACCENT = Color.rgb(56, 189, 248);
    private static final int MUTED = Color.rgb(155, 164, 181);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Channel> channels = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();

    private TextView status;
    private Button refreshButton;
    private Button exportButton;
    private Button diagnosticButton;
    private ProgressBar progress;
    private ArrayAdapter<String> adapter;
    private String lastRequest = "";
    private String lastResponse = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        fetchChannels();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("ATLAS LIVE PROBE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleBox.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Leitor técnico de canais ao vivo");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        titleBox.addView(subtitle);

        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        refreshButton = button("ATUALIZAR");
        refreshButton.setOnClickListener(v -> fetchChannels());
        header.addView(refreshButton);
        root.addView(header);

        status = new TextView(this);
        status.setText("Preparando consulta…");
        status.setTextColor(ACCENT);
        status.setTextSize(14);
        status.setTextIsSelectable(true);
        status.setPadding(0, dp(14), 0, dp(10));
        root.addView(status);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(4)));

        TextView tip = new TextView(this);
        tip.setText("Toque em um canal para ver o link final, formato, headers e opções de cópia. Segure para copiar o link rapidamente.");
        tip.setTextColor(MUTED);
        tip.setTextSize(13);
        tip.setPadding(0, dp(12), 0, dp(12));
        root.addView(tip);

        ListView list = new ListView(this);
        list.setDividerHeight(dp(8));
        list.setBackgroundColor(BG);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setTextSize(16);
                view.setTypeface(Typeface.DEFAULT_BOLD);
                view.setPadding(dp(16), dp(16), dp(16), dp(16));
                view.setBackground(rounded(PANEL, 16));
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showChannel(channels.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            copy("Link do canal", channels.get(position).playbackUrl());
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);

        exportButton = button("COPIAR JSON");
        exportButton.setEnabled(false);
        exportButton.setOnClickListener(v -> copyAllAsJson());
        actions.addView(exportButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        diagnosticButton = button("DIAGNÓSTICO");
        diagnosticButton.setEnabled(false);
        diagnosticButton.setOnClickListener(v -> showDiagnostic());
        LinearLayout.LayoutParams diagParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        diagParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(diagnosticButton, diagParams);

        root.addView(actions);
        setContentView(root);
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(rounded(PANEL_2, 14));
        return button;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), Color.rgb(42, 50, 68));
        return drawable;
    }

    private void fetchChannels() {
        setLoading(true, "Consultando os canais autorizados…");
        channels.clear();
        rows.clear();
        adapter.notifyDataSetChanged();

        executor.execute(() -> {
            try {
                lastRequest = buildRequest();
                HttpResult result = request(lastRequest, USER_AGENT, "");
                lastResponse = result.body;
                List<Channel> parsed = parseChannels(result.body);

                runOnUiThread(() -> {
                    channels.addAll(parsed);
                    for (Channel channel : parsed) {
                        String host = channel.host();
                        rows.add(channel.title + "\n" + channel.kind()
                                + (host.isEmpty() ? "" : "  •  " + host));
                    }
                    adapter.notifyDataSetChanged();
                    setLoading(false, parsed.isEmpty()
                            ? "HTTP " + result.code + ": a resposta chegou, mas nenhum canal foi reconhecido."
                            : parsed.size() + " canal(is) ao vivo encontrado(s). HTTP " + result.code + ".");
                    exportButton.setEnabled(!parsed.isEmpty());
                    diagnosticButton.setEnabled(true);
                });
            } catch (Exception error) {
                lastResponse = "ERRO: " + error.getClass().getSimpleName()
                        + "\n" + String.valueOf(error.getMessage());
                runOnUiThread(() -> {
                    setLoading(false, "Falha: " + error.getClass().getSimpleName()
                            + " — " + String.valueOf(error.getMessage()));
                    diagnosticButton.setEnabled(true);
                });
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        refreshButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    private String buildRequest() throws Exception {
        Locale locale = Locale.getDefault();
        String language = emptyFallback(locale.getLanguage(), "pt");
        String country = emptyFallback(locale.getCountry(), "BR");
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        androidId = emptyFallback(androidId, "unknown");

        return ENDPOINT
                + "?v=231"
                + "&vname=" + enc("7.1.0.8")
                + "&idapp=" + enc(APP_ID)
                + "&idusu=0"
                + "&cod_g="
                + "&gp=0"
                + "&am=0"
                + "&idl=" + enc(language)
                + "&pa_env=1"
                + "&pa=" + enc(country)
                + "&pn=" + enc(SOURCE_PACKAGE)
                + "&fus=" + enc("010100000000")
                + "&aid=" + enc(androidId)
                + "&recup_todo=1";
    }

    private HttpResult request(String requestUrl, String userAgent, String rawHeaders) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", emptyFallback(userAgent, USER_AGENT));
            connection.setRequestProperty("Accept", "text/plain, application/json, application/vnd.apple.mpegurl, */*");
            connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag());
            applyHeaders(connection, rawHeaders);

            int code = connection.getResponseCode();
            InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) {
                return new HttpResult(code, "", connection.getURL().toString());
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                    if (body.length() > 5_000_000) {
                        throw new IllegalStateException("Resposta maior que 5 MB.");
                    }
                }
            }
            return new HttpResult(code, body.toString(), connection.getURL().toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void applyHeaders(HttpURLConnection connection, String rawHeaders) {
        if (rawHeaders == null || rawHeaders.trim().isEmpty()) {
            return;
        }
        String normalized = rawHeaders.replace("\\n", "\n").replace("||", "\n");
        for (String piece : normalized.split("[\\n|;]+")) {
            int separator = piece.indexOf(':');
            if (separator <= 0) {
                separator = piece.indexOf('=');
            }
            if (separator <= 0) {
                continue;
            }
            String key = piece.substring(0, separator).trim();
            String value = piece.substring(separator + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                try {
                    connection.setRequestProperty(key, value);
                } catch (Exception ignored) {
                    // Ignore malformed provider headers in the diagnostic app.
                }
            }
        }
    }

    private List<Channel> parseChannels(String raw) {
        Map<String, String> values = parseValues(raw);
        Set<String> ids = new LinkedHashSet<>();

        addIds(ids, values.get("idseccs"));
        addIds(ids, values.get("seccs"));
        for (String key : values.keySet()) {
            if (key.endsWith("_tit") && key.length() > 4) {
                ids.add(key.substring(0, key.length() - 4));
            }
        }

        LinkedHashMap<String, Channel> unique = new LinkedHashMap<>();
        for (String id : ids) {
            String title = first(values.get(id + "_tit"), "Canal " + id);
            if (looksLikeCamera(title)) {
                continue;
            }

            String directUrl = decode(values.get(id + "_url"));
            String playlist = decode(values.get(id + "_pl"));
            String stream = first(values.get(id + "_stream"), "");
            String player = first(values.get(id + "_tp"), "");
            String type = first(values.get(id + "_tipo"), "");
            String userAgent = decode(values.get(id + "_ua"));
            String headers = decode(values.get(id + "_h"));
            String drmHeaders = decode(values.get(id + "_hd"));
            String licenseUrl = decode(values.get(id + "_li"));

            List<String> candidates = extractUrls(directUrl + "\n" + playlist);
            String selected = chooseBestUrl(candidates);
            if (selected.isEmpty() || !isLive(stream, directUrl, playlist, selected)) {
                continue;
            }

            String[] split = splitInlineOptions(selected);
            Channel channel = new Channel(
                    id,
                    title,
                    split[0],
                    selected,
                    candidates,
                    directUrl,
                    playlist,
                    player,
                    type,
                    userAgent,
                    headers,
                    drmHeaders,
                    licenseUrl,
                    split[1]
            );
            unique.put(title.toLowerCase(Locale.ROOT) + "|" + channel.url, channel);
        }
        return new ArrayList<>(unique.values());
    }

    private Map<String, String> parseValues(String raw) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (String piece : raw.replace("\r", "").split("\\]")) {
            String cleaned = piece.trim();
            while (cleaned.startsWith("[")) {
                cleaned = cleaned.substring(1).trim();
            }
            int equals = cleaned.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = cleaned.substring(0, equals).trim();
            String value = cleaned.substring(equals + 1).trim();
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private void addIds(Set<String> ids, String rawIds) {
        if (rawIds == null) {
            return;
        }
        for (String id : rawIds.split("[,;\\s]+")) {
            if (!id.trim().isEmpty()) {
                ids.add(id.trim());
            }
        }
    }

    private boolean looksLikeCamera(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.contains("câmera")
                || lower.contains("camera")
                || lower.contains("webcam")
                || lower.contains("monitoramento");
    }

    private boolean isLive(String stream, String firstUrl, String secondUrl, String finalUrl) {
        String flag = stream.trim().toLowerCase(Locale.ROOT);
        if (flag.equals("1") || flag.equals("true") || flag.equals("yes") || flag.equals("sim")) {
            return true;
        }
        String combined = (firstUrl + " " + secondUrl + " " + finalUrl).toLowerCase(Locale.ROOT);
        return combined.contains(".m3u8")
                || combined.contains(".m3u")
                || combined.contains(".mpd")
                || combined.contains(".ts")
                || combined.contains("rtmp://")
                || combined.contains("rtsp://")
                || combined.contains("/live/")
                || combined.contains("live=");
    }

    private String decode(String value) {
        if (value == null) {
            return "";
        }
        String decoded = value
                .replace("@yy1111@", "https://")
                .replace("@yy111@", "https://www.")
                .replace("@yy11@", "http://")
                .replace("@yy1@", "http://www.")
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u003d", "=")
                .replace("&amp;", "&")
                .trim();
        if (!decoded.contains("://") && decoded.matches("(?i).*%3a%2f%2f.*")) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                // Keep original if URL decoding fails.
            }
        }
        return decoded;
    }

    private List<String> extractUrls(String source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (source == null || source.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Matcher matcher = Pattern.compile("(?i)(?:https?|rtmp|rtsp)://[^\\s\\\"'<>\\[\\]]+").matcher(source);
        while (matcher.find()) {
            String value = matcher.group().trim();
            while (!value.isEmpty()) {
                char last = value.charAt(value.length() - 1);
                if (last == ',' || last == ';' || last == ')' || last == '}' || last == ']') {
                    value = value.substring(0, value.length() - 1);
                } else {
                    break;
                }
            }
            if (!value.isEmpty()) {
                unique.add(value);
            }
        }
        return new ArrayList<>(unique);
    }

    private String chooseBestUrl(List<String> urls) {
        if (urls.isEmpty()) {
            return "";
        }
        for (String url : urls) {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains(".m3u8") || lower.contains(".mpd")
                    || lower.startsWith("rtmp://") || lower.startsWith("rtsp://")) {
                return url;
            }
        }
        for (String url : urls) {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains(".m3u") || lower.contains(".ts") || lower.contains("/live/")) {
                return url;
            }
        }
        return urls.get(0);
    }

    private String[] splitInlineOptions(String value) {
        int pipe = value.indexOf('|');
        if (pipe <= 0) {
            return new String[]{value, ""};
        }
        return new String[]{value.substring(0, pipe), value.substring(pipe + 1)};
    }

    private void showChannel(Channel channel) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(8));
        scroll.addView(box);

        addDetail(box, "FORMATO", channel.kind());
        addDetail(box, "LINK FINAL", channel.playbackUrl());
        addDetail(box, "LINK BRUTO", channel.rawSelectedUrl);
        addDetail(box, "PLAYER", emptyFallback(channel.player, "—"));
        addDetail(box, "TIPO", emptyFallback(channel.type, "—"));
        addDetail(box, "USER-AGENT", emptyFallback(channel.userAgent, "—"));
        addDetail(box, "HEADERS", emptyFallback(channel.headers, "—"));
        addDetail(box, "OPÇÕES INLINE", emptyFallback(channel.inlineOptions, "—"));
        addDetail(box, "DRM HEADERS", emptyFallback(channel.drmHeaders, "—"));
        addDetail(box, "LICENÇA DRM", emptyFallback(channel.licenseUrl, "—"));

        if (channel.candidates.size() > 1) {
            addDetail(box, "OUTROS LINKS ENCONTRADOS", join(channel.candidates, "\n\n"));
        }

        Button resolve = button("TENTAR ENCONTRAR M3U8/HLS");
        resolve.setOnClickListener(v -> resolveFinalStream(channel));
        LinearLayout.LayoutParams resolveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        resolveParams.setMargins(0, dp(12), 0, 0);
        box.addView(resolve, resolveParams);

        Button copyJson = button("COPIAR ESTE CANAL EM JSON");
        copyJson.setOnClickListener(v -> copy("Canal em JSON", channel.toJson().toString()));
        LinearLayout.LayoutParams jsonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        jsonParams.setMargins(0, dp(8), 0, 0);
        box.addView(copyJson, jsonParams);

        new AlertDialog.Builder(this)
                .setTitle(channel.title)
                .setView(scroll)
                .setPositiveButton("COPIAR LINK", (dialog, which) -> copy("Link do canal", channel.playbackUrl()))
                .setNeutralButton("ABRIR", (dialog, which) -> openChannel(channel.playbackUrl()))
                .setNegativeButton("FECHAR", null)
                .show();
    }

    private void addDetail(LinearLayout box, String label, String value) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ACCENT);
        labelView.setTextSize(12);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setPadding(0, dp(10), 0, dp(3));
        box.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.DKGRAY);
        valueView.setTextSize(14);
        valueView.setTextIsSelectable(true);
        valueView.setPadding(dp(10), dp(10), dp(10), dp(10));
        valueView.setBackground(rounded(Color.rgb(235, 238, 244), 10));
        box.addView(valueView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void resolveFinalStream(Channel channel) {
        status.setText("Tentando resolver o link de “" + channel.title + "”…");
        executor.execute(() -> {
            try {
                HttpResult result = request(channel.url, channel.userAgent, channel.headers);
                List<String> found = extractUrls(result.body);
                if (isDirectMedia(result.finalUrl)) {
                    found.add(0, result.finalUrl);
                }
                String best = chooseBestUrl(found);
                if (!best.isEmpty() && isDirectMedia(best)) {
                    String[] split = splitInlineOptions(best);
                    channel.resolvedUrl = split[0];
                    runOnUiThread(() -> {
                        status.setText("Link resolvido para “" + channel.title + "”: " + channel.kind());
                        copy("Link resolvido", channel.playbackUrl());
                        showResolved(channel, found, result.code);
                    });
                } else {
                    runOnUiThread(() -> {
                        status.setText("Não foi encontrado um link HLS/M3U8 direto nessa tentativa.");
                        showResolved(channel, found, result.code);
                    });
                }
            } catch (Exception error) {
                runOnUiThread(() -> status.setText("Falha ao resolver: " + error.getMessage()));
            }
        });
    }

    private boolean isDirectMedia(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains(".m3u8") || lower.contains(".m3u")
                || lower.contains(".mpd") || lower.contains(".ts")
                || lower.startsWith("rtmp://") || lower.startsWith("rtsp://");
    }

    private void showResolved(Channel channel, List<String> found, int code) {
        String text = "HTTP " + code
                + "\n\nLINK ESCOLHIDO:\n" + channel.playbackUrl()
                + "\n\nLINKS ENCONTRADOS:\n"
                + (found.isEmpty() ? "Nenhum link de mídia direto encontrado." : join(found, "\n\n"));
        TextView value = new TextView(this);
        value.setText(text);
        value.setTextIsSelectable(true);
        value.setPadding(dp(16), dp(16), dp(16), dp(16));
        new AlertDialog.Builder(this)
                .setTitle("Resultado da resolução")
                .setView(value)
                .setPositiveButton("COPIAR", (dialog, which) -> copy("Resultado", text))
                .setNegativeButton("FECHAR", null)
                .show();
    }

    private void openChannel(String rawUrl) {
        String url = splitInlineOptions(rawUrl)[0];
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
                Toast.makeText(this, "Instale um player compatível, como VLC.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void copyAllAsJson() {
        JSONArray array = new JSONArray();
        for (Channel channel : channels) {
            array.put(channel.toJson());
        }
        JSONObject root = new JSONObject();
        try {
            root.put("provider", "authorized-live-provider");
            root.put("generatedAt", System.currentTimeMillis());
            root.put("count", channels.size());
            root.put("channels", array);
        } catch (Exception ignored) {
            // JSONObject operations with valid values should not fail.
        }
        copy("Canais em JSON", root.toString());
    }

    private void showDiagnostic() {
        TextView text = new TextView(this);
        text.setText("REQUISIÇÃO\n" + lastRequest + "\n\nRESPOSTA BRUTA\n" + lastResponse);
        text.setTextIsSelectable(true);
        text.setPadding(dp(12), dp(12), dp(12), dp(12));

        new AlertDialog.Builder(this)
                .setTitle("Diagnóstico técnico")
                .setView(text)
                .setPositiveButton("COPIAR", (dialog, which) -> copy("Diagnóstico", text.getText().toString()))
                .setNegativeButton("FECHAR", null)
                .show();
    }

    private void copy(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
            Toast.makeText(this, "Copiado.", Toast.LENGTH_SHORT).show();
        }
    }

    private String enc(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private String first(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(separator);
            }
            out.append(value);
        }
        return out.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class HttpResult {
        final int code;
        final String body;
        final String finalUrl;

        HttpResult(int code, String body, String finalUrl) {
            this.code = code;
            this.body = body;
            this.finalUrl = finalUrl;
        }
    }

    private static final class Channel {
        final String id;
        final String title;
        final String url;
        final String rawSelectedUrl;
        final List<String> candidates;
        final String rawDirectUrl;
        final String rawPlaylist;
        final String player;
        final String type;
        final String userAgent;
        final String headers;
        final String drmHeaders;
        final String licenseUrl;
        final String inlineOptions;
        String resolvedUrl = "";

        Channel(
                String id,
                String title,
                String url,
                String rawSelectedUrl,
                List<String> candidates,
                String rawDirectUrl,
                String rawPlaylist,
                String player,
                String type,
                String userAgent,
                String headers,
                String drmHeaders,
                String licenseUrl,
                String inlineOptions
        ) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.rawSelectedUrl = rawSelectedUrl;
            this.candidates = candidates;
            this.rawDirectUrl = rawDirectUrl;
            this.rawPlaylist = rawPlaylist;
            this.player = player;
            this.type = type;
            this.userAgent = userAgent;
            this.headers = headers;
            this.drmHeaders = drmHeaders;
            this.licenseUrl = licenseUrl;
            this.inlineOptions = inlineOptions;
        }

        String playbackUrl() {
            return resolvedUrl.isEmpty() ? url : resolvedUrl;
        }

        String kind() {
            String lower = playbackUrl().toLowerCase(Locale.ROOT);
            if (lower.startsWith("rtmp://")) return "RTMP";
            if (lower.startsWith("rtsp://")) return "RTSP";
            if (lower.contains(".mpd")) return "DASH";
            if (lower.contains(".m3u8")) return "HLS / M3U8";
            if (lower.contains(".m3u")) return "M3U";
            if (lower.contains(".ts")) return "MPEG-TS";
            return "HTTP / WEB";
        }

        String host() {
            try {
                String host = Uri.parse(playbackUrl()).getHost();
                return host == null ? "" : host;
            } catch (Exception ignored) {
                return "";
            }
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("name", title);
                object.put("streamType", kind());
                object.put("url", playbackUrl());
                object.put("rawUrl", rawSelectedUrl);
                object.put("userAgent", userAgent);
                object.put("headers", headers);
                object.put("inlineOptions", inlineOptions);
                object.put("player", player);
                object.put("type", type);
                object.put("drmHeaders", drmHeaders);
                object.put("licenseUrl", licenseUrl);
                object.put("rawDirectUrl", rawDirectUrl);
                object.put("rawPlaylist", rawPlaylist);
                JSONArray all = new JSONArray();
                for (String candidate : candidates) {
                    all.put(candidate);
                }
                object.put("candidates", all);
            } catch (Exception ignored) {
                // Keep the fields that were successfully written.
            }
            return object;
        }
    }
}
