package example.practice.world;

// The eight sectors of the one world, each a real direction on the map. dx/dy
// are unit vectors so a front's travel can be projected onto them - that's what
// makes "a front sweeps west to east across the sectors" a physical computation
// rather than a label. Kingdom id i sits in values()[i].
public enum CompassDirection {
    N (  0f,    1f,    "the Northern Reach"),
    NE( 0.7071f, 0.7071f, "the Northeast Marches"),
    E (  1f,    0f,    "the Eastern Provinces"),
    SE( 0.7071f, -0.7071f, "the Southeast Coast"),
    S (  0f,   -1f,    "the Southern Shore"),
    SW(-0.7071f, -0.7071f, "the Southwest Fens"),
    W ( -1f,    0f,    "the Western Frontier"),
    NW(-0.7071f, 0.7071f, "the Northwest Highlands");

    public final float dx, dy;
    public final String region;

    CompassDirection(float dx, float dy, String region) {
        this.dx = dx;
        this.dy = dy;
        this.region = region;
    }
}