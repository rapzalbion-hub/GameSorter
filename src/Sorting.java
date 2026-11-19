/**
 * Algoritma pengurutan yang ditulis manual (tanpa Arrays.sort):
 * Bubble Sort, Selection Sort, dan Insertion Sort.
 * Semuanya mengurutkan array Game[] di tempat (in-place).
 */
public class Sorting {

    public static boolean isValidField(String f) {
        return f.equals("name") || f.equals("genre") || f.equals("platform")
                || f.equals("year") || f.equals("no");
    }

    /** Membandingkan dua game berdasarkan kolom tertentu. */
    public static int compare(Game x, Game y, String field) {
        switch (field) {
            case "name":     return x.name.compareToIgnoreCase(y.name);
            case "genre":    return x.genre.compareToIgnoreCase(y.genre);
            case "platform": return x.platform.compareToIgnoreCase(y.platform);
            case "year":     return Integer.compare(x.year, y.year);
            default:         return Integer.compare(x.no, y.no);
        }
    }

    // perbandingan + penghitung; hasil dibalik bila urutan menurun
    private static int cmp(Game x, Game y, String field, boolean asc, Stats s) {
        s.comparisons++;
        int c = compare(x, y, field);
        return asc ? c : -c;
    }

    private static void swap(Game[] a, int i, int j) {
        Game tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /** Bubble Sort: bandingkan tetangga, tukar bila salah urutan. O(n^2). */
    public static Stats bubbleSort(Game[] a, String field, boolean asc) {
        Stats s = new Stats();
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (cmp(a[j], a[j + 1], field, asc, s) > 0) {
                    swap(a, j, j + 1);
                    s.swaps++;
                    swapped = true;
                }
            }
            if (!swapped) break; // sudah terurut, berhenti lebih awal
        }
        s.timeMs = (System.nanoTime() - t0) / 1_000_000.0;
        return s;
    }

    /** Selection Sort: pilih elemen terkecil, taruh di posisi depan. O(n^2). */
    public static Stats selectionSort(Game[] a, String field, boolean asc) {
        Stats s = new Stats();
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                if (cmp(a[j], a[min], field, asc, s) < 0) min = j;
            }
            if (min != i) {
                swap(a, i, min);
                s.swaps++;
            }
        }
        s.timeMs = (System.nanoTime() - t0) / 1_000_000.0;
        return s;
    }

    /** Insertion Sort: sisipkan tiap elemen ke bagian kiri yang sudah terurut. O(n^2). */
    public static Stats insertionSort(Game[] a, String field, boolean asc) {
        Stats s = new Stats();
        long t0 = System.nanoTime();
        int n = a.length;
        for (int i = 1; i < n; i++) {
            Game key = a[i];
            int j = i - 1;
            while (j >= 0 && cmp(a[j], key, field, asc, s) > 0) {
                a[j + 1] = a[j]; // geser ke kanan
                s.swaps++;
                j--;
            }
            a[j + 1] = key;
        }
        s.timeMs = (System.nanoTime() - t0) / 1_000_000.0;
        return s;
    }
}
