import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Web app: Sorting & Searching 10.000 data game.
 * - Tanpa database: data dibaca dari CSV ke dalam array Game[].
 * - Tanpa framework: memakai HttpServer bawaan Java (JDK 11+).
 */
public class GameApp {

    private static final Object LOCK = new Object();
    private static Game[] original;          // urutan awal dari file
    private static Game[] data;              // array yang sedang dipakai (bisa terurut)
    private static String sortedField = null; // kolom tempat array terurut (null = belum terurut)
    private static boolean sortedAsc = true;
    private static final Random RND = new Random();

    /** Error karena input pengguna salah (HTTP 400). */
    private static class BadRequest extends Exception {
        BadRequest(String msg) { super(msg); }
    }

    private interface Handler {
        String handle(Map<String, String> q) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        original = loadCsv("data/games.csv");
        data = original.clone();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", GameApp::serveIndex);
        server.createContext("/api/info",   ex -> api(ex, q -> info()));
        server.createContext("/api/games",  ex -> api(ex, GameApp::games));
        server.createContext("/api/dataset", ex -> api(ex, GameApp::dataset));
        server.createContext("/api/sort",   ex -> api(ex, GameApp::sort));
        server.createContext("/api/search", ex -> api(ex, GameApp::search));
        server.createContext("/api/sample", ex -> api(ex, GameApp::sample));
        server.createContext("/api/reset",  ex -> api(ex, q -> reset()));
        server.start();
        System.out.println("Data dimuat: " + data.length + " game");
        System.out.println("Buka di browser: http://localhost:" + port);
    }

    // ---------- Memuat data CSV ke array ----------
    private static Game[] loadCsv(String path) throws IOException {
        List<Game> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            br.readLine(); // lewati header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 5) continue;
                list.add(new Game(Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(),
                        p[3].trim(), Integer.parseInt(p[4].trim())));
            }
        }
        return list.toArray(new Game[0]); // inilah arraynya
    }

    // ---------- Endpoint ----------
    private static String info() {
        synchronized (LOCK) {
            return "{\"total\":" + data.length
                    + ",\"sortedField\":" + (sortedField == null ? "null" : Json.str(sortedField))
                    + ",\"sortedOrder\":" + Json.str(sortedAsc ? "asc" : "desc") + "}";
        }
    }

    private static String dataset(Map<String, String> q) throws BadRequest {
        int size = intParam(q, "size", 10_000);
        if ((size != 100 && size != 1_000 && size != 10_000) || size > original.length) {
            throw new BadRequest("Ukuran data harus 100, 1.000, atau 10.000 dan tidak melebihi isi CSV.");
        }
        synchronized (LOCK) {
            data = Arrays.copyOf(original, size);
            sortedField = null;
            sortedAsc = true;
        }
        return info();
    }

    private static String games(Map<String, String> q) throws BadRequest {
        int page = intParam(q, "page", 1);
        int size = Math.max(1, Math.min(500, intParam(q, "size", 100)));
        synchronized (LOCK) {
            int pages = (data.length + size - 1) / size;
            page = Math.max(1, Math.min(page, pages));
            int from = (page - 1) * size;
            int to = Math.min(data.length, from + size);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"total\":").append(data.length)
              .append(",\"page\":").append(page)
              .append(",\"size\":").append(size)
              .append(",\"pages\":").append(pages)
              .append(",\"items\":[");
            for (int i = from; i < to; i++) {
                if (i > from) sb.append(',');
                sb.append(data[i].toJson(i + 1));
            }
            return sb.append("]}").toString();
        }
    }

    private static String sort(Map<String, String> q) throws BadRequest {
        String algo = q.getOrDefault("algo", "");
        String field = q.getOrDefault("field", "name");
        boolean asc = !"desc".equals(q.get("order"));
        if (!Sorting.isValidField(field)) throw new BadRequest("Kolom tidak dikenal: " + field);

        Stats st;
        synchronized (LOCK) {
            switch (algo) {
                case "bubble":    st = Sorting.bubbleSort(data, field, asc); break;
                case "selection": st = Sorting.selectionSort(data, field, asc); break;
                case "insertion": st = Sorting.insertionSort(data, field, asc); break;
                default: throw new BadRequest("Algoritma sorting tidak dikenal: " + algo);
            }
            sortedField = field;
            sortedAsc = asc;
        }
        return "{\"algo\":" + Json.str(algo)
                + ",\"field\":" + Json.str(field)
                + ",\"order\":" + Json.str(asc ? "asc" : "desc")
                + ",\"n\":" + data.length
                + ",\"timeMs\":" + String.format(Locale.US, "%.2f", st.timeMs)
                + ",\"comparisons\":" + st.comparisons
                + ",\"swaps\":" + st.swaps + "}";
    }

    private static String search(Map<String, String> q) throws BadRequest {
        String algo = q.getOrDefault("algo", "");
        String field = q.getOrDefault("field", "name");
        String key = q.getOrDefault("q", "").trim();
        if (!Sorting.isValidField(field)) throw new BadRequest("Kolom tidak dikenal: " + field);
        if (key.isEmpty()) throw new BadRequest("Kata kunci pencarian tidak boleh kosong.");

        int keyInt = 0;
        if (field.equals("year") || field.equals("no")) {
            try {
                keyInt = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                throw new BadRequest("Kolom ini berisi angka, jadi kata kunci harus angka.");
            }
        }

        final int maxShow = data.length;
        Searching.Result r;
        StringBuilder items = new StringBuilder();
        synchronized (LOCK) {
            switch (algo) {
                case "linear":
                    r = Searching.linear(data, field, key, keyInt, maxShow);
                    break;
                case "binary":
                    if (!field.equals(sortedField)) {
                        throw new BadRequest("Binary Search butuh data yang sudah terurut pada kolom yang sama. "
                                + "Urutkan dulu berdasarkan kolom ini memakai salah satu algoritma sorting.");
                    }
                    r = Searching.binary(data, field, key, keyInt, sortedAsc, maxShow);
                    break;
                default:
                    throw new BadRequest("Algoritma pencarian tidak dikenal: " + algo);
            }
            for (int k = 0; k < r.indexes.size(); k++) {
                int idx = r.indexes.get(k);
                if (k > 0) items.append(',');
                items.append(data[idx].toJson(idx + 1));
            }
        }
        return "{\"algo\":" + Json.str(algo)
                + ",\"field\":" + Json.str(field)
                + ",\"query\":" + Json.str(key)
                + ",\"found\":" + (r.total > 0)
                + ",\"total\":" + r.total
                + ",\"timeMs\":" + String.format(Locale.US, "%.3f", r.stats.timeMs)
                + ",\"comparisons\":" + r.stats.comparisons
                + ",\"items\":[" + items + "]}";
    }

    private static String sample(Map<String, String> q) throws BadRequest {
        String field = q.getOrDefault("field", "name");
        if (!Sorting.isValidField(field)) throw new BadRequest("Kolom tidak dikenal: " + field);
        synchronized (LOCK) {
            Game g = data[RND.nextInt(data.length)];
            String v;
            switch (field) {
                case "name":     v = g.name; break;
                case "genre":    v = g.genre; break;
                case "platform": v = g.platform; break;
                case "year":     v = String.valueOf(g.year); break;
                default:         v = String.valueOf(g.no);
            }
            return "{\"value\":" + Json.str(v) + "}";
        }
    }

    private static String reset() {
        synchronized (LOCK) {
            data = Arrays.copyOf(original, data.length);
            sortedField = null;
            sortedAsc = true;
        }
        return info();
    }

    // ---------- Infrastruktur HTTP ----------
    private static void serveIndex(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/index.html")) {
            send(ex, 404, "text/plain; charset=utf-8", "Tidak ditemukan".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] html = Files.readAllBytes(Paths.get("web", "index.html"));
        send(ex, 200, "text/html; charset=utf-8", html);
    }

    private static void api(HttpExchange ex, Handler h) throws IOException {
        int code = 200;
        String body;
        try {
            body = h.handle(parseQuery(ex.getRequestURI().getRawQuery()));
        } catch (BadRequest e) {
            code = 400;
            body = "{\"error\":" + Json.str(e.getMessage()) + "}";
        } catch (Exception e) {
            code = 500;
            body = "{\"error\":" + Json.str("Kesalahan server: " + e) + "}";
        }
        send(ex, code, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    private static int intParam(Map<String, String> q, String name, int def) throws BadRequest {
        String v = q.get(name);
        if (v == null) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new BadRequest("Parameter " + name + " harus angka.");
        }
    }
}
