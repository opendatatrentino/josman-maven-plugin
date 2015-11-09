/*
 * Copyright 2015  Trento Rise  (trentorise.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.trentorise.opendata.josman.maven;

import static eu.trentorise.opendata.commons.TodUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import eu.trentorise.opendata.josman.JosmanProject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Base class to extend for josman mojos
 *
 * @author David Leoni
 */
public abstract class JosmanMojo extends AbstractMojo {

    private String messagePrefix;

    /**
     * The oauth2 token for authentication
     * TODO Currently it's not really used.
     */
    @Parameter(defaultValue = "${github.global.oauth2Token}", readonly = true)
    private String oauth2Token;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * These versions will be ignored by the site generator     
     */
    @Parameter(defaultValue = "")
    private List<String> ignoredVersions;

    /**
     * If true only a website for the latest snapshot version will be generated
     */
    @Parameter(property = "site.snapshot", defaultValue = "false")
    private String snapshot;

    protected JosmanMojo(String messagePrefix) {
        checkNotEmpty(messagePrefix, "Invalid message prefix!");
        this.messagePrefix = messagePrefix;
    }

    protected void debug(String msg) {
        getLog().debug(messagePrefix + ":  " + msg);
    }

    protected void info(String msg) {
        getLog().info(messagePrefix + ":  " + msg);
    }

    protected void error(String msg) {
        getLog().error(messagePrefix + ":  " + msg);
    }

    public MavenProject getProject() {
        return project;
    }

    /**
     * If oauth token was not provided returns empty string.
     */
    public String getOauth2Token() {
        return oauth2Token == null ? "" : oauth2Token;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a josman project with information from pom and git
     */
    protected JosmanProject loadProjectInfo() throws MojoExecutionException {
        getLog().info("");
        getLog().info("");
        debug("Parsing options and project...");
        getLog().info("");

        String repoName = getProject().getArtifactId();

        checkNotEmpty(repoName, "Found wrong repo name!");

        String repoTitle = getProject().getName();

        checkNotEmpty(repoTitle, "Found wrong repo title!");

        String projectUrl = getProject().getUrl();

        checkNotEmpty(projectUrl, "project url is invalid!");

        //https://github.com/opendatatrentino/tod-commons
        String stripGithub = projectUrl.substring("https://github.com/".length());

        String repoOrganization = stripGithub.substring(0, stripGithub.indexOf("/"));

        getLog().info("");
        info("repo name:         " + repoName);
        info("repo title:        " + repoTitle);
        info("repo organization: " + repoOrganization);
        getLog().info("");

        boolean isSnapshot;
        try {
            isSnapshot = Boolean.parseBoolean(snapshot);
        }
        catch (Exception ex) {
            throw new MojoExecutionException("Couldn't parse 'local' parameter, found string: " + snapshot, ex);
        }

        List<SemVersion> parsedIgnoredVersions = new ArrayList();
        if (ignoredVersions != null) {
            for (String s : ignoredVersions) {
                try {
                    parsedIgnoredVersions.add(SemVersion.of(s));
                }
                catch (Exception ex) {
                    throw new MojoExecutionException("Couldn't parse 'ignoredVersions' parameter for version " + s + ", found list: " + ignoredVersions, ex);
                }
            }
        }

        return new JosmanProject(
                repoName,
                repoTitle,
                repoOrganization,
                "",
                "target" + File.separator + "site",
                parsedIgnoredVersions,
                isSnapshot
        );

    }

}
