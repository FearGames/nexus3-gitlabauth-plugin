package fr.auchan.nexus3.gitlabauth.plugin.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.auchan.nexus3.gitlabauth.plugin.GitlabAuthenticationException;
import fr.auchan.nexus3.gitlabauth.plugin.GitlabPrincipal;
import fr.auchan.nexus3.gitlabauth.plugin.config.GitlabAuthConfiguration;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.Pagination;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Named("GitlabApiClient")
public class GitlabApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabApiClient.class);

    private final GitlabAPI client;
    private final GitlabAuthConfiguration configuration;

    // Cache token lookups to reduce the load on Github's User API to prevent hitting the rate limit.
    private final Cache<String, GitlabPrincipal> tokenToPrincipalCache;

    /*
    public GitlabApiClient() {
    }

    public GitlabApiClient(GitlabAPI client, GitlabAuthConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
        initPrincipalCache();
    }
    */

    @Inject
    public GitlabApiClient(GitlabAuthConfiguration configuration) {
        this.configuration = configuration;
        client = GitlabAPI.connect(configuration.getApiUrl(), configuration.getApiKey());
        client.ignoreCertificateErrors(configuration.getIgnoreCertificateErrors());
        tokenToPrincipalCache = CacheBuilder.newBuilder()
                .expireAfterWrite(configuration.getCacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /*
    @PostConstruct
    public void init() {
    }

    private void initPrincipalCache() {
    }
    */

    public GitlabPrincipal authz(String login, char[] token) throws GitlabAuthenticationException {
        // Combine the login and the token as the cache key since they are both used to generate the principal. If either changes we should obtain a new
        // principal.
        String cacheKey = login + "|" + new String(token);
        GitlabPrincipal cached = tokenToPrincipalCache.getIfPresent(cacheKey);
        if (cached != null) {
            LOGGER.debug("Using cached principal for login: {}", login);
            return cached;
        } else {
            GitlabPrincipal principal = doAuthz(login, token);
            tokenToPrincipalCache.put(cacheKey, principal);
            return principal;
        }
    }

    private GitlabPrincipal doAuthz(String loginName, char[] token) throws GitlabAuthenticationException {
        GitlabUser gitlabUser;
        try {
            GitlabAPI gitlabAPI = GitlabAPI.connect(configuration.getApiUrl(), String.valueOf(token));
            gitlabAPI.ignoreCertificateErrors(configuration.getIgnoreCertificateErrors());
            gitlabUser = gitlabAPI.getUser();
        } catch (Exception e) {
            throw new GitlabAuthenticationException(e);
        }

        if (gitlabUser == null) {
            throw new GitlabAuthenticationException("Given username not found!");
        }

        if (!loginName.equalsIgnoreCase(gitlabUser.getUsername()) && !loginName.equalsIgnoreCase(gitlabUser.getEmail())) {
            throw new GitlabAuthenticationException("Given username does not match GitLab username or email!");
        }

        GitlabPrincipal principal = new GitlabPrincipal();

        principal.setUsername(gitlabUser.getUsername());

        Set<String> groups = new LinkedHashSet<>();
        if (gitlabUser.isAdmin() != null && gitlabUser.isAdmin() && configuration.getAdminMapping()) {
            groups.add("nx-admin");
        }
        if (!configuration.getDefaultRoles().isEmpty()) {
            groups.addAll(Arrays.asList(configuration.getDefaultRoles().split(",")));
        }
        if (configuration.getGroupMapping()) {
            groups.addAll(getGroups((gitlabUser.getUsername())));
        }
        principal.setGroups(groups);

        return principal;
    }

    private Set<String> getGroups(String username) throws GitlabAuthenticationException {
        List<GitlabGroup> groups;
        try {
            groups = client.getGroupsViaSudo(username, new Pagination().withPerPage(Pagination.MAX_ITEMS_PER_PAGE));
        } catch (IOException e) {
            throw new GitlabAuthenticationException("Could not fetch groups for given username");
        }
        return groups.stream().map(this::mapGitlabGroupToNexusRole).collect(Collectors.toSet());
    }

    private String mapGitlabGroupToNexusRole(GitlabGroup team) {
        return team.getPath();
    }
}