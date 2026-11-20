# Game Sorter & Searcher (Java, tanpa database)

Web app untuk mengurutkan dan mencari 10.000 data game. Data dibaca dari `data/games.csv`
ke dalam array `Game[]` di memori, tanpa database dan tanpa framework (memakai `HttpServer` bawaan JDK).

## Cara menjalankan
Syarat: JDK 11 atau lebih baru (perintah `javac` harus ada).

- Windows: klik dua kali `run.bat`
- Linux / macOS: `./run.sh`
- Manual:
      mkdir out
      javac -encoding UTF-8 -d out src/*.java
      java -cp out GameApp          (port lain: java -cp out GameApp 9000)

Lalu buka http://localhost:8080 di browser. Jalankan dari folder proyek ini
(program membaca `data/games.csv` dan `web/index.html` secara relatif).

## Struktur
    src/Sorting.java    Bubble, Selection, Insertion Sort (ditulis manual)
    src/Searching.java  Linear Search dan Binary Search (ditulis manual)
    src/GameApp.java    server HTTP + endpoint API + memuat CSV ke array
    src/Game.java, Stats.java, Json.java   kelas pendukung
    web/index.html      tampilan (HTML + JavaScript)
    data/games.csv      10.000 data game

## Algoritma
| Algoritma  | Waktu terbaik | Waktu rata-rata / terburuk | Catatan |
|------------|---------------|----------------------------|---------|
| Bubble Sort    | O(n) (data sudah terurut) | O(n^2) | banyak pertukaran |
| Selection Sort | O(n^2) | O(n^2) | pertukaran paling sedikit |
| Insertion Sort | O(n) | O(n^2) | cepat bila data hampir terurut |
| Linear Search  | O(1) | O(n) | tidak butuh data terurut |
| Binary Search  | O(1) | O(log n) | data HARUS terurut pada kolom yang dicari |

Kolom "Perbandingan" dan "Pertukaran/pergeseran" di halaman web dihitung langsung di dalam algoritma.

## Catatan
- Array hasil sorting tetap terurut di memori sampai tombol Reset ditekan, jadi sorting kedua
  (misalnya Insertion Sort pada data yang sudah terurut) akan jauh lebih cepat.
- Binary Search menolak berjalan bila data belum terurut pada kolom yang sama (server mengirim pesan error).
- Pencarian memakai kecocokan sama persis (tidak membedakan huruf besar/kecil).
- Array dipakai bersama oleh semua pengunjung (cocok untuk demo/tugas, bukan untuk banyak pengguna sekaligus).

## API
    GET /api/games?page=1&size=100
    GET /api/sort?algo=bubble|selection|insertion&field=name|genre|platform|year|no&order=asc|desc
    GET /api/search?algo=linear|binary&field=...&q=kata+kunci
    GET /api/sample?field=name      (nilai acak dari data, untuk contoh pencarian)
    GET /api/reset                  (kembali ke urutan awal)
    GET /api/info

## Deploy (Vercel + backend Java)

Vercel menyajikan halaman web dari folder `web`. Backend Java dijalankan sebagai Docker web service terpisah.

1. Push proyek ini ke repository GitHub.
2. Buat **Web Service** dari repository tersebut di Render, pilih runtime **Docker**, lalu deploy. Render akan memakai `Dockerfile` di root proyek.
3. Setelah backend aktif, buka URL-nya dan pastikan `/api/info` menampilkan JSON.
4. Masukkan URL backend ke `window.API_BASE_URL` di `web/config.js`, tanpa garis miring di akhir, lalu commit dan push perubahan.
5. Import repository ke Vercel. Framework Preset pilih **Other**, Build Command kosong, Output Directory isi `web`, lalu deploy.
6. Buka URL Vercel. Halaman web mengirim permintaan API ke backend yang URL-nya diisi pada langkah 4.

`vercel.json` sudah mengatur Output Directory ke `web`. Backend mengizinkan CORS untuk frontend karena keduanya berjalan di domain berbeda.
