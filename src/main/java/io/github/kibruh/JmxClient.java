package io.github.kibruh;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.Optional;

public interface JmxClient extends AutoCloseable {

    void connect() throws IOException;

    Optional<Object> getAttribute(ObjectName objectName, String attributeName) throws IOException;

    Optional<Object> getAttribute(ObjectName objectName, String attributeName, String path) throws IOException;

    void close() throws IOException;

    boolean isConnected();
}
