package cz.cvut.fel.dsv.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.dsv.node.NodeDetails;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ConfigLoader {

    private static final String CONFIG_PATH = "nodes.json";

    public static NodeDetails load(String nodeId) {
        log.info("Loading configuration for node '{}'", nodeId);

        try (InputStream is = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_PATH)) {

            if (is == null) {
                log.error("Configuration file '{}' not found on classpath", CONFIG_PATH);
                throw new IllegalStateException(
                        "Configuration file not found: " + CONFIG_PATH
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            List<NodeDetails> nodes = mapper.readValue(is, new TypeReference<>() {});
            NodeDetails details = nodes.stream().filter(node -> Objects.equals(node.getNodeId(), nodeId)).findFirst().get();

            log.info(
                    "Configuration loaded: id={} address={}:{}",
                    details.getNodeId(),
                    details.getHost(),
                    details.getPort()
            );

            return details;

        } catch (Exception e) {
            log.error("Failed to load node configuration", e);
            throw new IllegalStateException(
                    "Failed to load node configuration", e
            );
        }
    }

}
