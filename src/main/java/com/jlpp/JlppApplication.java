package com.jlpp;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class JlppApplication {

    private static final Logger log = LoggerFactory.getLogger(JlppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JlppApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openAdminDashboard(ApplicationReadyEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        int port = environment.getProperty("local.server.port", Integer.class,
                environment.getProperty("server.port", Integer.class, 8080));
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String dashboardUrl = "http://localhost:" + port + contextPath + "/admin/dashboard";
        log.info("Admin dashboard is ready: {}", dashboardUrl);

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.warn("This environment cannot open a browser automatically. Open this URL manually: {}", dashboardUrl);
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create(dashboardUrl));
        } catch (IOException exception) {
            log.error("Could not open the Admin dashboard in the browser: {}", dashboardUrl, exception);
        }
    }
}
