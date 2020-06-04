package fi.dy.masa.worldprimer.util;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public enum Coordinate
{
    X(Vec3i::getX, (v) -> v.x),
    Y(Vec3i::getY, (v) -> v.y),
    Z(Vec3i::getZ, (v) -> v.z);

    private final IIntFactory<Vec3i> intFactory;
    private final IDoubleFactory<Vec3d> doubleFactory;

    Coordinate(IIntFactory<Vec3i> posFactory, IDoubleFactory<Vec3d> coordFactory)
    {
        this.intFactory = posFactory;
        this.doubleFactory = coordFactory;
    }

    public int getIntPos(Vec3i pos)
    {
        return this.intFactory.getInt(pos);
    }

    public double getDoublePos(Vec3d pos)
    {
        return this.doubleFactory.getDouble(pos);
    }

    public interface IIntFactory<T>
    {
        int getInt(T obj);
    }

    public interface IDoubleFactory<T>
    {
        double getDouble(T obj);
    }
}
