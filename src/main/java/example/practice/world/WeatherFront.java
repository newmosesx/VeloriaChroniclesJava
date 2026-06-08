package example.practice.world;

// One travelling front. Its influence is a gaussian centred at `center` along its
// travel axis; as center advances each day, the bump of effect sweeps across the
// sectors. When it outlives `life`, it dissipates.
public class WeatherFront {
    public FrontType type;
    public float dirX, dirY;   // unit travel direction
    public float center;       // position along the travel axis (advances daily)
    public float speed;        // axis units per day
    public float sigma;        // width
    public float intensity;    // peak strength
    public int age, life;
    public boolean alive = true;

    // Strength felt by a sector whose projection onto the travel axis is axisPos.
    public float effectAt(float axisPos) {
        float d = axisPos - center;
        return (float) (intensity * Math.exp(-(d * d) / (2 * sigma * sigma)));
    }

    public void advance() {
        center += speed;
        age++;
        if (age >= life) alive = false;
    }
}