package example.practice.config;

// The five estates. basePower is their institutional weight before headcount and
// economy adjust it - the Clergy and Nobility carry standing the Commons can only
// match through sheer numbers.
public enum FactionType {
    COMMONS("The Commons", 0.30f),
    ARMY("The Army", 0.20f),
    CLERGY("The Clergy", 0.45f),
    MERCHANTS("The Merchants", 0.35f),
    NOBILITY("The Nobility", 0.50f);

    public final String title;
    public final float basePower;
    FactionType(String title, float basePower){ this.title = title; this.basePower = basePower; }
}