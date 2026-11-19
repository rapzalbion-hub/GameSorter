/** Satu baris data game. */
public class Game {
    public final int no;
    public final String name;
    public final String genre;
    public final String platform;
    public final int year;

    public Game(int no, String name, String genre, String platform, int year) {
        this.no = no;
        this.name = name;
        this.genre = genre;
        this.platform = platform;
        this.year = year;
    }

    /** pos = posisi (mulai dari 1) pada array saat ini. */
    public String toJson(int pos) {
        return "{\"pos\":" + pos
                + ",\"no\":" + no
                + ",\"name\":" + Json.str(name)
                + ",\"genre\":" + Json.str(genre)
                + ",\"platform\":" + Json.str(platform)
                + ",\"year\":" + year + "}";
    }
}
