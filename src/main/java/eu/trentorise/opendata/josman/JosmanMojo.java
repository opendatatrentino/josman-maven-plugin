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
package eu.trentorise.opendata.josman;

import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import eu.trentorise.opendata.josman.JosmanProject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Base class to extend for Josman mojos
 *
 * 
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
    protected MavenProject project;

    /**
     * These versions will be ignored by the site generator     
     */
    @Parameter(defaultValue = "")
    private List<String> ignoredVersions;

    /**
     * If true only a website for the latest snapshot version will be generated
     * 
     * @deprecated use josman.snapshot instead . See <a href="https://github.com/opendatatrentino/josman-maven-plugin/issues/29" target=_blank>related issue</a>
     *             <br/><b>NOTE: STARTING WITH 0.8.0, SNAPSHOT GENERATION IS TRUE BY DEFAULT.</b>.  
     */
    @Deprecated()
    @Parameter(property = "site.snapshot")    
    private String siteSnapshot;

    /**
     * If enabled generates documentation for the current snapshot. Supercedes 'site.snapshot'.
     * 
     * <br/><b>NOTE: STARTING WITH 0.8.0, SNAPSHOT GENERATION IS TRUE BY DEFAULT.</b>
     */
    @Parameter(property = "josman.snapshot")
    private String snapshot;
    
    /**
     * If enabled generates documentation also for past released versions. False by default.
     * 
     */
    @Parameter(property = "josman.releases")
    private String releases;
    
    /**
     * If enabled copies latest javadoc (if present). False by default.
     * 
     */
    @Parameter(property = "josman.javadoc")
    private String javadoc;
    
    /**
     * If true josman fails on warnings and errors. False by default.
     * 
     */
    @Parameter(property = "josman.failOnError")
    private String failOnError;    
    
    
    /**
     * Modality of execution:
     * 
     * <ul>
     * <li><b>dev (default)</b>  : generates documentation only about current snapshot, 
     * doesn't fail on errors/warnings and doesn't copy javadocs.
     * </li>
     * <li><b>ci</b>: generates documentation only about current snapshot, doesn't fail on errors/warnings
     * and copies javadocs if present.</li>
     * <li><b>staging</b>: generates all it can about released versions, and fails on errors/warnings.
     * Also generates documentation about the  current snapshot.</li>             
     * <li><b>release</b>: generates all it can about released versions, and fails on errors/warnings.</li>
     * </ul>
     * See also <a href="https://github.com/opendatatrentino/josman-maven-plugin/issues/29" target="_blank">related discussion</a> and 
     * {@link JosmanMode}</br>
     * 
     * @since 0.8.0
     */         
    @Parameter(property = "josman.mode", defaultValue = "dev")
    private String mode;
    
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

    /**
     * @since 0.8.0
     */    
    protected void fatalError(String msg, MojoExecutionException ex) throws MojoExecutionException {              
        throw ex;
    }

    /**
     * @since 0.8.0
     */    
    protected void fatalError(String msg, Exception ex) throws MojoExecutionException {
        String s = "\n         " + messagePrefix + ":  " + msg;        
        throw new MojoExecutionException(s, ex);
    }
    
    
    /**
     * @since 0.8.0
     */
    protected void fatalError(String msg) throws MojoExecutionException {
        String s = "\n         " + messagePrefix + ":  " + msg;               
        throw new MojoExecutionException(s);
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


        getLog().info("");
        info("repo name:         " + repoName);
        info("repo title:        " + repoTitle);
        info("organization repo: " + Josmans.organization(getProject().getUrl()));
        info("organization site: " + getProject().getOrganization().getName() + "   " + getProject().getOrganization().getUrl());
        getLog().info("");

        JosmanConfig.Builder configb = JosmanConfig.builder()
                                        .setSourceRepoDir("")
                                        .setPagesDir("target/site");
                
        
        if (siteSnapshot != null && siteSnapshot.length() > 0){            
            fatalError("Starting with version v0.8.0 of Josman, site.snapshot is not used anymore! "
                    + "\n   See https://github.com/opendatatrentino/josman-maven-plugin/issues/29");
        }

        try {
            configb.setMode(JosmanMode.valueOf(mode));
        } catch (Exception ex) {
            
            fatalError("Couldn't parse 'josman.mode' parameter, found string: '" + mode + "'\n"
                       +"\n Possible options are: \n"
                       + Arrays.toString(JosmanMode.values()).replace("[", "").replace("]","")
                       + "\n\n");
        }
        
        try {
            if (snapshot != null){
                configb.setSnapshot(Boolean.parseBoolean(snapshot));
            }
        } catch (Exception ex) {            
            fatalError("Couldn't parse 'josman.snapshot' parameter, found string: " + snapshot, ex);
        }
        
        try {
            if (releases != null){
                configb.setReleases(Boolean.parseBoolean(releases));
            }
        } catch (Exception ex) {
            fatalError("Couldn't parse 'josman.releases' parameter, found string: " + releases, ex);
        }

        try {
            if (javadoc != null){
                configb.setJavadoc(Boolean.parseBoolean(javadoc));                
            }
        } catch (Exception ex) {
            fatalError("Couldn't parse 'josman.javadoc' parameter, found string: " + javadoc, ex);
        }

        try {
            if (failOnError != null){
                configb.setFailOnError(Boolean.parseBoolean(failOnError));               
            } 
        } catch (Exception ex) {
            fatalError("Couldn't parse 'josman.failOnError' parameter, found string: " + failOnError, ex);
        }       
                       
        
        
        List<SemVersion> parsedIgnoredVersions = new ArrayList<>();
        if (ignoredVersions != null) {
            for (String iv : ignoredVersions) {
                try {
                    parsedIgnoredVersions.add(SemVersion.of(iv));
                }
                catch (Exception ex) {
                    fatalError("Couldn't parse 'ignoredVersions' parameter for version " 
                    + iv + ", found list: " + ignoredVersions, ex);
                }
            }
        }
        configb.setIgnoredVersions(parsedIgnoredVersions);        

        JosmanConfig cfg = configb.build();
        
        info("\n\n" + cfg.toString());        
        
        return new JosmanProject(
                getProject(),
                cfg);

    }
    
    

}
