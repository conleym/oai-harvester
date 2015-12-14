package org.unizin.cmp.harvester.service;

import java.sql.SQLException;
import java.util.Objects;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.lifecycle.Managed;

public class ManagedH2Server implements Managed {
    public static interface CreateServer {
        Server apply(String[] args) throws SQLException;
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(
            ManagedH2Server.class);

    private final Server server;

    private static String arg(final String type, final String suffix) {
        return "-" + type + suffix;
    }

    private final String type;

    public ManagedH2Server(final int port, final String type,
            final CreateServer makeServer)
                    throws Exception {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(makeServer, "makeServer");
        this.type = type;
        final String[] args = {
                arg(type, "AllowOthers"),
                arg(type, "Port"),
                String.valueOf(port)
        };
        server = makeServer.apply(args);
    }

    public String getStatus() {
        return server.getStatus();
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting H2 {} server...", type);
        server.start();
        LOGGER.info("Done: {}", getStatus());
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping H2 {} server...", type);
        server.stop();
        LOGGER.info("Done: {}", getStatus());
    }
}
