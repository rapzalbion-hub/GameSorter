import java.util.ArrayList;
import java.util.List;

/** Algoritma pencarian: Linear Search dan Binary Search (ditulis manual). */
public class Searching {

    public static class Result {
        public final List<Integer> indexes = new ArrayList<>(); // indeks array (0-based), dibatasi maxShow
        public int total = 0;                                   // total data yang cocok
        public final Stats stats = new Stats();
    }

    // Membandingkan nilai kolom sebuah game dengan kata kunci.
    private static int compareToKey(Game g, String field, String q, int qInt) {
        switch (field) {
            case "name":     return g.name.compareToIgnoreCase(q);
            case "genre":    return g.genre.compareToIgnoreCase(q);
            case "platform": return g.platform.compareToIgnoreCase(q);
            case "year":     return Integer.compare(g.year, qInt);
            default:         return Integer.compare(g.no, qInt);
        }
    }

    /** Linear Search: periksa elemen satu per satu dari awal sampai akhir. O(n). */
    public static Result linear(Game[] a, String field, String q, int qInt, int maxShow) {
        Result r = new Result();
        long t0 = System.nanoTime();
        for (int i = 0; i < a.length; i++) {
            r.stats.comparisons++;
            if (compareToKey(a[i], field, q, qInt) == 0) {
                r.total++;
                if (r.indexes.size() < maxShow) r.indexes.add(i);
            }
        }
        r.stats.timeMs = (System.nanoTime() - t0) / 1_000_000.0;
        return r;
    }

    /**
     * Binary Search: syarat array HARUS sudah terurut pada kolom yang sama.
     * Mencari kemunculan pertama (lower bound), lalu membaca data kembar di sebelahnya.
     * O(log n).
     */
    public static Result binary(Game[] a, String field, String q, int qInt, boolean asc, int maxShow) {
        Result r = new Result();
        long t0 = System.nanoTime();
        int lo = 0, hi = a.length - 1, first = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            r.stats.comparisons++;
            int c = compareToKey(a[mid], field, q, qInt);
            if (!asc) c = -c;
            if (c == 0) {
                first = mid;   // simpan, lalu cari kemunculan yang lebih awal
                hi = mid - 1;
            } else if (c < 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        if (first >= 0) {
            for (int i = first; i < a.length; i++) {
                r.stats.comparisons++;
                if (compareToKey(a[i], field, q, qInt) != 0) break;
                r.total++;
                if (r.indexes.size() < maxShow) r.indexes.add(i);
            }
        }
        r.stats.timeMs = (System.nanoTime() - t0) / 1_000_000.0;
        return r;
    }
}
