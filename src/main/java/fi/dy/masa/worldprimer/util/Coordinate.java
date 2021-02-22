package fi.dy.masa.worldprimer.util;

import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public enum Coordinate
{
    X(Vec3i::getX, (v) -> v.x),
    Y(Vec3i::getY, (v) -> v.y),
    Z(Vec3i::getZ, (v) -> v.z);

    private final ToIntFunction<Vec3i> intFunction;
    private final ToDoubleFunction<Vec3d> doubleFunction;

    Coordinate(ToIntFunction<Vec3i> posFactory, ToDoubleFunction<Vec3d> coordFactory)
    {
        this.intFunction = posFactory;
        this.doubleFunction = coordFactory;
    }

    public int getCoordinateAsInt(Vec3i pos)
    {
        return this.intFunction.applyAsInt(pos);
    }

    public double getCoordinateAsDouble(Vec3d pos)
    {
        return this.doubleFunction.applyAsDouble(pos);
    }

    public String getCoordinateAsIntString(Vec3i pos)
    {
        return String.valueOf(this.getCoordinateAsInt(pos));
    }

    public String getCoordinateAsDoubleString(Vec3d pos)
    {
        return String.valueOf(this.getCoordinateAsDouble(pos));
    }
}
