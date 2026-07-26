package artframework.c2;

/**
 * Stable reference to a dungeon map node for interceptors/pins (no STS types).
 */
public final class MapNodeRef {

    public final int row;
    public final int col;
    /** Room kind hint such as {@code monster}, {@code rest}, {@code unknown}; may be empty. */
    public final String roomType;

    public MapNodeRef(int row, int col, String roomType) {
        this.row = row;
        this.col = col;
        this.roomType = roomType != null ? roomType : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MapNodeRef)) {
            return false;
        }
        MapNodeRef that = (MapNodeRef) o;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return "MapNodeRef{" + row + "," + col + "," + roomType + "}";
    }
}
