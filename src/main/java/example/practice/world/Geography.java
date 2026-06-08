package example.practice.world;

// The static substrate - the board the dynamic systems run on. It doesn't tick;
// it answers spatial questions: where each sector sits, which face the sea, and
// how a travelling front's path projects onto each sector.
public class Geography {

    public static class Sector {
        public final int id;            // == kingdom id
        public final CompassDirection dir;
        public final boolean coastal;
        public Sector(int id, CompassDirection dir, boolean coastal) {
            this.id = id; this.dir = dir; this.coastal = coastal;
        }
        public String region() { return dir.region; }
    }

    private final Sector[] sectors;

    public Geography() {
        CompassDirection[] dirs = CompassDirection.values();
        sectors = new Sector[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            CompassDirection d = dirs[i];
            // A southern sea: the southern sectors are coastal (matters later for
            // floods and storms that come off the water).
            boolean coastal = (d == CompassDirection.S || d == CompassDirection.SE || d == CompassDirection.SW);
            sectors[i] = new Sector(i, d, coastal);
        }
    }

    public int count() { return sectors.length; }
    public Sector sector(int id) { return sectors[id]; }

    // How far along a travel direction a given sector lies.
    public float projection(int id, float dx, float dy) {
        return sectors[id].dir.dx * dx + sectors[id].dir.dy * dy;
    }

    public float minProjection(float dx, float dy) {
        float m = Float.MAX_VALUE;
        for (Sector s : sectors) m = Math.min(m, s.dir.dx * dx + s.dir.dy * dy);
        return m;
    }

    public float maxProjection(float dx, float dy) {
        float m = -Float.MAX_VALUE;
        for (Sector s : sectors) m = Math.max(m, s.dir.dx * dx + s.dir.dy * dy);
        return m;
    }

    // The sector a front is currently centred over (for reporting).
    public Sector nearestSector(WeatherFront f) {
        Sector best = sectors[0];
        float bestD = Float.MAX_VALUE;
        for (Sector s : sectors) {
            float ap = s.dir.dx * f.dirX + s.dir.dy * f.dirY;
            float d = Math.abs(ap - f.center);
            if (d < bestD) { bestD = d; best = s; }
        }
        return best;
    }
}