package io.contained.internals;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Path implements Iterable<String> {
    private static final String DEFAULT_DELIMITER = "/";
    private static final Pattern DELIMITER = Pattern.compile("[/\\\\]");
    private final String usedDelimiter;
    private final List<String> path;

    public Path(String path) {
        var matcher = DELIMITER.matcher(path);
        if (matcher.find()) {
            this.usedDelimiter = matcher.group(0);
        } else {
            this.usedDelimiter = DEFAULT_DELIMITER;
        }
        this.path = List.of(DELIMITER.split(normalize(path)));
    }

    public String getName() {
        return mapIfNotEmpty(path::getLast);
    }

    public int size() {
        return path.size();
    }

    public String getPart(int index) {
        return mapIfNotEmpty(() -> path.get(index));
    }

    public Path getParentPath() {
        if (path.isEmpty()) {
            return null;
        }
        if (path.size() == 1) {
            return new Path("");
        }
        return new Path(String.join(usedDelimiter, path.subList(0, path.size() - 1)));
    }

    public String join(String name) {
        return this + usedDelimiter + name;
    }

    @Override
    public String toString() {
        return mapIfNotEmpty(() -> String.join(usedDelimiter, path), usedDelimiter);
    }

    @Override
    public Iterator<String> iterator() {
        return path.iterator();
    }

    @Override
    public void forEach(Consumer<? super String> action) {
        path.forEach(action);
    }

    @Override
    public Spliterator<String> spliterator() {
        return path.spliterator();
    }

    private String normalize(String path) {
        if (!path.startsWith(usedDelimiter)) {
            path = usedDelimiter + path;
        }
        return path;
    }

    private String mapIfNotEmpty(Supplier<String> supplier) {
        return mapIfNotEmpty(supplier, "");
    }

    private String mapIfNotEmpty(Supplier<String> supplier, String defaultValue) {
        return path.isEmpty() ? defaultValue : supplier.get();
    }
}
