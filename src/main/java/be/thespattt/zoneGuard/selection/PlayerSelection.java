package be.thespattt.zoneGuard.selection;

import be.thespattt.zoneGuard.model.BlockPoint;
import be.thespattt.zoneGuard.model.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerSelection {
    private SelectionMode mode = SelectionMode.CUBOID;
    private BlockPoint first;
    private BlockPoint second;
    private final List<BlockPoint> polygonPoints = new ArrayList<>();
    private boolean polygonClosed;

    public SelectionMode mode() {
        return mode;
    }

    public void start(SelectionMode nextMode) {
        mode = nextMode;
        first = null;
        second = null;
        polygonPoints.clear();
        polygonClosed = false;
    }

    public void setFirst(BlockPoint first) {
        this.first = first;
    }

    public void setSecond(BlockPoint second) {
        this.second = second;
    }

    public Optional<BlockPoint> first() {
        return Optional.ofNullable(first);
    }

    public Optional<BlockPoint> second() {
        return Optional.ofNullable(second);
    }

    public void addPolygonPoint(BlockPoint point) {
        if (!polygonClosed) {
            polygonPoints.add(point);
        }
    }

    public void closePolygon() {
        polygonClosed = polygonPoints.size() >= 3;
    }

    public boolean polygonClosed() {
        return polygonClosed;
    }

    public List<BlockPoint> polygonBlockPoints() {
        return List.copyOf(polygonPoints);
    }

    public List<Point2D> polygon2DPoints() {
        return polygonPoints.stream().map(point -> new Point2D(point.x(), point.z())).toList();
    }

    public Optional<String> world() {
        if (mode == SelectionMode.CUBOID) {
            return first().map(BlockPoint::world).or(() -> second().map(BlockPoint::world));
        }
        return polygonPoints.stream().findFirst().map(BlockPoint::world);
    }
}
