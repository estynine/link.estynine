package com.atlascine.channeltester;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Channel> channels = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();

    private TextView status;
    private Button fetchButton;
    private Button diagnosticButton;
    private ArrayAdapter<String> adapter;
    private String lastRequest = "";
    private String lastResponse = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        int pad = dp(14);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(16, 16, 16));

        TextView title = new TextView(this);
        title.setText("Teste dos canais ao vivo");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Toque em Buscar. O app consulta a configuração e mostra somente transmissões ao vivo identificadas.");
        description.setTextColor(Color.LTGRAY);
        description.setTextSize(14);
        description.setPadding(0, 0, 0, dp(12));
        root.addView(description);

        fetchButton = new Button(this);
        fetchButton.setText("BUSCAR CANAIS");
        fetchButton.setOnClickListener(v -> fetchChannels());
        root.addView(fetchButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Pronto para testar.");
        status.setTextColor(Color.rgb(130, 200, 255));
        status.setTextSize(14);
        status.setTextIsSelectable(true);
        status.setPadding(0, dp(10), 0, dp(10));
        root.addView(status);

        ListView list = new ListView(this);
        list.setBackgroundColor(Color.rgb(28, 28, 28));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setTextSize(16);
                view.setPadding(dp(12), dp(14), dp(12), dp(14));
                view.setBackgroundColor(Color.rgb(28, 28, 28));
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> showChannel(channels.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            copy(channels.get(position).url);
            return true;
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        diagnosticButton = new Button(this);
        diagnosticButton.setText("VER DIAGNÓSTICO");
        diagnosticButton.setEnabled(false);
        diagnosticButton.setOnClickListener(v -> showDiagnostic());
        LinearLayout.LayoutParams diagnosticParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        diagnosticParams.setMargins(0, dp(8), 0, 0);
        root.addView(diagnosticButton, diagnosticParams);

        setContentView(root);
    }

    private void fetchChannels() {
        fetchButton.setEnabled(false);
        diagnosticButton.setEnabled(false);
        status.setText("Chamando o servidor…");
        channels.clear();
        rows.clear();
        adapter.notifyDataSetChanged();

        executor.execute(() -> {
            try {
                lastRequest = buildRequest();
                HttpResult result = request(lastRequest);
                lastResponse = result.body;
                List<Channel> parsed = parseChannels(result.body);

                runOnUiThread(() -> {
                    channels.addAll(parsed);
                    for (Channel channel : parsed) {
                        rows.add(channel.title + "\n" + channel.kind());
                    }
                    adapter.notifyDataSetChanged();
                    fetchButton.setEnabled(true);
                    diagnosticButton.setEnabled(true);

                    if (parsed.isEmpty()) {
                        status.setText("HTTP " + result.code + ": resposta recebida, mas nenhum canal foi reconhecido. Abra o diagnóstico.");
                    } else {
                        status.setText(parsed.size() + " canal(is) encontrado(s). HTTP " + result.code + ".");
                    }
                });
            } catch (Exception error) {
                lastResponse = "ERRO: " + error.getClass().getSimpleName() + "\n" + String.valueOf(error.getMessage());
                runOnUiThread(() -> {
                    fetchButton.setEnabled(true);
                    diagnosticButton.setEnabled(true);
                    status.setText("Falha: " + error.getClass().getSimpleName() + " — " + String.valueOf(error.getMessage()));
                });
            }
        });
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

    private HttpResult request(String requestUrl) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "text/plain, application/json, */*");
            connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag());

            int code = connection.getResponseCode();
            InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) {
                return new HttpResult(code, "");
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
            return new HttpResult(code, body.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
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
            String directUrl = decode(values.get(id + "_url"));
            String playlist = decode(values.get(id + "_pl"));
            String stream = first(values.get(id + "_stream"), "");
            String player = first(values.get(id + "_tp"), "");
            String userAgent = decode(values.get(id + "_ua"));
            String headers = decode(values.get(id + "_h"));

            String url = extractUrl(directUrl);
            if (url.isEmpty()) {
                url = extractUrl(playlist);
            }
            if (url.isEmpty() || !isLive(stream, directUrl, playlist, url)) {
                continue;
            }

            Channel channel = new Channel(id, title, url, player, userAgent, headers);
            unique.put(title.toLowerCase(Locale.ROOT) + "|" + url, channel);
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

    private boolean isLive(String stream, String firstUrl, String secondUrl, String finalUrl) {
        String flag = stream.trim().toLowerCase(Locale.ROOT);
        if (flag.equals("1") || flag.equals("true") || flag.equals("yes") || flag.equals("sim")) {
            return true;
        }
        String combined = (firstUrl + " " + secondUrl + " " + finalUrl).toLowerCase(Locale.ROOT);
        return combined.contains(".m3u8")
                || combined.contains(".m3u")
                || combined.contains(".mpd")
                || combined.contains("rtmp://")
                || combined.contains("rtsp://")
                || combined.contains("/live/")
                || combined.contains("live=");
    }

    private String decode(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("@yy1111@", "https://")
                .replace("@yy111@", "https://www.")
                .replace("@yy11@", "http://")
                .replace("@yy1@", "http://www.")
                .replace("\\/", "/")
                .replace("&amp;", "&")
                .trim();
    }

    private String extractUrl(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "";
        }
        Matcher matcher = Pattern.compile("(?i)(?:https?|rtmp|rtsp)://[^\\s\\\"'<>\\[\\]]+").matcher(source);
        if (!matcher.find()) {
            return "";
        }
        String value = matcher.group().trim();
        while (!value.isEmpty()) {
            char last = value.charAt(value.length() - 1);
            if (last == ',' || last == ';' || last == ')' || last == '}' || last == ']') {
                value = value.substring(0, value.length() - 1);
            } else {
                break;
            }
        }
        return value;
    }

    private void showChannel(Channel channel) {
        String details = "ID: " + channel.id
                + "\nFormato: " + channel.kind()
                + "\nPlayer: " + emptyFallback(channel.player, "—")
                + "\nUser-Agent: " + emptyFallback(channel.userAgent, "—")
                + "\nHeaders: " + emptyFallback(channel.headers, "—")
                + "\n\nURL:\n" + channel.url;

        new AlertDialog.Builder(this)
                .setTitle(channel.title)
                .setMessage(details)
                .setPositiveButton("ABRIR", (dialog, which) -> openChannel(channel.url))
                .setNeutralButton("COPIAR URL", (dialog, which) -> copy(channel.url))
                .setNegativeButton("FECHAR", null)
                .show();
    }

    private void openChannel(String rawUrl) {
        String url = rawUrl;
        int pipe = url.indexOf('|');
        if (pipe > 0) {
            url = url.substring(0, pipe);
        }
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

    private void showDiagnostic() {
        TextView text = new TextView(this);
        text.setText("REQUISIÇÃO\n" + lastRequest + "\n\nRESPOSTA\n" + lastResponse);
        text.setTextIsSelectable(true);
        text.setPadding(dp(12), dp(12), dp(12), dp(12));

        new AlertDialog.Builder(this)
                .setTitle("Diagnóstico")
                .setView(text)
                .setPositiveButton("COPIAR", (dialog, which) -> copy(text.getText().toString()))
                .setNegativeButton("FECHAR", null)
                .show();
    }

    private void copy(String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Canal", value));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    private static final class Channel {
        final String id;
        final String title;
        final String url;
        final String player;
        final String userAgent;
        final String headers;

        Channel(String id, String title, String url, String player, String userAgent, String headers) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.player = player;
            this.userAgent = userAgent;
            this.headers = headers;
        }

        String kind() {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.startsWith("rtmp://")) return "RTMP";
            if (lower.startsWith("rtsp://")) return "RTSP";
            if (lower.contains(".mpd")) return "DASH";
            if (lower.contains(".m3u8") || lower.contains(".m3u")) return "HLS/M3U";
            return "HTTP";
        }
    }
}
