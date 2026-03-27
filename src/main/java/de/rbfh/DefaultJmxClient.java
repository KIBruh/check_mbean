package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class DefaultJmxClient implements JmxClient {
    private static final Logger logger = LoggerFactory.getLogger(DefaultJmxClient.class);

    private final String jmxUrl;
    private final String username;
    private final String password;
    private final int timeout;

    private JMXConnector connector;
    private MBeanServerConnection connection;

    public DefaultJmxClient(String jmxUrl, String username, String password, int timeout) {
        this.jmxUrl = jmxUrl;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
    }

    @Override
    public void connect() throws IOException {
        try {
            JMXServiceURL url = new JMXServiceURL(jmxUrl);
            Map<String, Object> environment = createEnvironment();

            connector = JMXConnectorFactory.connect(url, environment);
            connector.connect();

            connection = connector.getMBeanServerConnection();
            logger.debug("Connected to JMX at {}", jmxUrl);
        } catch (IOException e) {
            logger.error("Failed to connect to JMX at {}: {}", jmxUrl, e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> createEnvironment() {
        Map<String, Object> env = new java.util.HashMap<>();

        if (username != null && password != null) {
            String[] credentials = {username, password};
            env.put(javax.management.remote.JMXConnector.CREDENTIALS, credentials);
        }

        env.put("jmx.remote.x.request.waiting", timeout * 1000);

        return env;
    }

    @Override
    public Optional<Object> getAttribute(ObjectName objectName, String attributeName) throws IOException {
        ensureConnected();

        try {
            Object value = connection.getAttribute(objectName, attributeName);
            logger.debug("Retrieved attribute {}/{} = {}", objectName, attributeName, value);
            return Optional.ofNullable(value);
        } catch (javax.management.AttributeNotFoundException e) {
            logger.error("Attribute not found: {}/{}", objectName, attributeName);
            return Optional.empty();
        } catch (javax.management.InstanceNotFoundException e) {
            logger.error("MBean not found: {}", objectName);
            throw new IOException("MBean not found: " + objectName, e);
        } catch (javax.management.MBeanException e) {
            logger.error("MBean exception for {}/{}: {}", objectName, attributeName, e.getMessage());
            throw new IOException("MBean exception: " + e.getMessage(), e);
        } catch (javax.management.ReflectionException e) {
            logger.error("Reflection exception for {}/{}: {}", objectName, attributeName, e.getMessage());
            throw new IOException("Reflection exception: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Object> getAttribute(ObjectName objectName, String attributeName, String path) throws IOException {
        Optional<Object> baseValue = getAttribute(objectName, attributeName);

        if (baseValue.isEmpty()) {
            return Optional.empty();
        }

        if (path == null || path.isEmpty()) {
            return baseValue;
        }

        return traversePath(baseValue.get(), path);
    }

    private Optional<Object> traversePath(Object current, String path) {
        String[] parts = path.split("\\.");

        for (String part : parts) {
            if (current instanceof CompositeData cd) {
                Object value = cd.get(part);
                if (value == null) {
                    logger.debug("Path component not found: {}", part);
                    return Optional.empty();
                }
                current = value;
            } else if (current instanceof CompositeDataSupport cds) {
                Object value = cds.get(part);
                if (value == null) {
                    logger.debug("Path component not found: {}", part);
                    return Optional.empty();
                }
                current = value;
            } else {
                logger.debug("Cannot traverse path at {} - not CompositeData", part);
                return Optional.empty();
            }
        }

        return Optional.of(current);
    }

    private void ensureConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to JMX server");
        }
    }

    @Override
    public void close() throws IOException {
        if (connector != null) {
            try {
                connector.close();
                logger.debug("Closed JMX connection");
            } catch (IOException e) {
                logger.warn("Error closing JMX connection: {}", e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public boolean isConnected() {
        return connector != null && connection != null;
    }
}
