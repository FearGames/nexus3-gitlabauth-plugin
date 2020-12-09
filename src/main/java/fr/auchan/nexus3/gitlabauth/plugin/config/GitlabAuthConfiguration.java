package fr.auchan.nexus3.gitlabauth.plugin.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
@Named
public class GitlabAuthConfiguration {

    private static final String CONFIG_FILE = "gitlabauth.properties";

    private static final String API_URL_DEFAULT = "https://gitlab.com";
    private static final boolean IGNORE_CERTIFICATE_ERRORS_DEFAULT = false;
    private static final Duration CACHE_TTL_DEFAULT = Duration.ofMinutes(1);
    private static final String DEFAULT_ROLES_DEFAULT = "";
    private static final boolean ADMIN_MAPPING_DEFAULT = true;
    private static final boolean GROUP_MAPPING_DEFAULT = true;

    private static final String API_URL_KEY = "gitlab.api.url";
    private static final String IGNORE_CERTIFICATE_ERRORS_KEY = "gitlab.api.certificate.ignore_errors";
    private static final String API_KEY_KEY = "gitlab.api.key";
    private static final String CACHE_TTL_KEY = "gitlab.principal.cache.ttl";
    private static final String DEFAULT_ROLES_KEY = "gitlab.role.default";
    private static final String ADMIN_MAPPING_KEY = "gitlab.role.mapping.admin.enabled";
    private static final String GROUP_MAPPING_KEY = "gitlab.role.mapping.group.enabled";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabAuthConfiguration.class);

    private final Properties configuration;

    @Inject
    public GitlabAuthConfiguration() {
        configuration = new Properties();

        try {
            configuration.load(Files.newInputStream(Paths.get(".", "etc", CONFIG_FILE)));
        } catch (IOException e) {
            LOGGER.warn("Error reading GitLab oauth properties, falling back to default configuration", e);
        }
    }

    public String getApiUrl() {
        return configuration.getProperty(API_URL_KEY, API_URL_DEFAULT);
    }

    public boolean getIgnoreCertificateErrors() {
        return Boolean.parseBoolean(configuration.getProperty(IGNORE_CERTIFICATE_ERRORS_KEY, String.valueOf(IGNORE_CERTIFICATE_ERRORS_DEFAULT)));
    }

    public String getApiKey() {
        return configuration.getProperty(API_KEY_KEY, "");
    }

    public Duration getCacheTtl() {
        return Duration.parse(configuration.getProperty(CACHE_TTL_KEY, CACHE_TTL_DEFAULT.toString()));
    }

    public String getDefaultRoles() {
        return configuration.getProperty(DEFAULT_ROLES_KEY, DEFAULT_ROLES_DEFAULT);
    }

    public boolean getAdminMapping() {
        return Boolean.parseBoolean(configuration.getProperty(ADMIN_MAPPING_KEY, String.valueOf(ADMIN_MAPPING_DEFAULT)));
    }

    public boolean getGroupMapping() {
        return Boolean.parseBoolean(configuration.getProperty(GROUP_MAPPING_KEY, String.valueOf(GROUP_MAPPING_DEFAULT)));
    }
}