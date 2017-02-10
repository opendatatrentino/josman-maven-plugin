package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;

import java.io.File;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import eu.trentorise.opendata.commons.SemVersion;
import eu.trentorise.opendata.josman.exceptions.JosmanException;

/**
 * Immutable config for JosmanProject
 * 
 * @since 0.8.0
 *
 */
public class JosmanConfig {

    /**
     * If enables gens documentation for the current snapshot
     * 
     * @since 0.8.0
     */
    private boolean snapshot;

    /**
     * If enabled gens documentation also for past released versions
     * 
     * @since 0.8.0
     */
    private boolean releases;

    /**
     * If enabled copies latest javadoc (if present)
     * 
     * @since 0.8.0
     */
    private boolean javadoc;

    /**
     * If true josman fails on warnings and errors.
     * 
     * @since 0.8.0
     */
    private boolean failOnError;

    /**
     * A modality - see {@link JosmanMode}
     * 
     * @since 0.8.0
     */
    private JosmanMode mode;

    /**
     * @since 0.8.0
     */
    private File sourceRepoDir;

    /**
     * @since 0.8.0
     */
    private File pagesDir;

    /**
     * @since 0.8.0
     */
    private ImmutableList<SemVersion> ignoredVersions;

    /**
     * @since 0.8.0
     */
    private JosmanConfig() {
        this.mode = JosmanMode.dev;
        this.ignoredVersions = ImmutableList.of();
        this.snapshot = true;
        this.javadoc = false;
        this.releases = false;
        this.failOnError = false;        
    }


    /**
     * @since 0.8.0
     */
    public boolean isSnapshot() {
        return snapshot;
    }

    /**
     * @since 0.8.0
     */
    public boolean isReleases() {
        return releases;
    }

    /**
     * @since 0.8.0
     */
    public boolean isJavadoc() {
        return javadoc;
    }

    /**
     * @since 0.8.0
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    /**
     * @since 0.8.0
     */
    public JosmanMode getMode() {
        return mode;
    }

    /**
     * @since 0.8.0
     */
    public File getSourceRepoDir() {
        return sourceRepoDir;
    }

    /**
     * @since 0.8.0
     */
    public File getPagesDir() {
        return pagesDir;
    }

    /**
     * @since 0.8.0
     */
    public ImmutableList<SemVersion> getIgnoredVersions() {
        return ignoredVersions;
    }

    /**
     * @since 0.8.0
     */
    static public class Builder {

        boolean built;
        JosmanConfig config;

        /**
         * @since 0.8.0
         */
        private Builder() {
            this.built = false;
            this.config = new JosmanConfig();
        }

        /**
         * @since 0.8.0
         */
        private void checkBuilt() {
            if (built) {
                throw new JosmanException("Trying to set a field on already built object !");
            }
        }

        /**
         * @since 0.8.0
         */
        public Builder setSnapshot(boolean snapshot) {
            checkBuilt();
            config.snapshot = snapshot;
            return this;
        }

        /**
         * @since 0.8.0
         */
        public Builder setReleases(boolean releases) {
            checkBuilt();
            config.releases = releases;
            return this;
        }

        /**
         * @since 0.8.0
         */
        public Builder setJavadoc(boolean javadoc) {
            checkBuilt();
            config.javadoc = javadoc;
            return this;
        }

        /**
         * @since 0.8.0
         */
        public Builder setFailOnError(boolean failOnError) {
            checkBuilt();
            config.failOnError = failOnError;
            return this;
        }


        /**
         * @since 0.8.0
         */
        public Builder setPagesDir(String pagesDirPath) {
            checkBuilt();

            checkNotEmpty(pagesDirPath, "Invalid pages dir path!");
            config.pagesDir = new File(pagesDirPath);
            return this;
        }

        /**
         * @since 0.8.0
         */
        public Builder setIgnoredVersions(Iterable<SemVersion> ignoredVersions) {
            checkBuilt();
            Preconditions.checkNotNull(ignoredVersions);

            config.ignoredVersions = ImmutableList.copyOf(ignoredVersions);
            return this;
        }

        /**
         * @since 0.8.0
         */
        public Builder setSourceRepoDir(String sourceRepoDirPath) {
            checkBuilt();
            checkNotNull(sourceRepoDirPath, "Invalid repository source dir path!");
            if (sourceRepoDirPath.isEmpty()) {
                config.sourceRepoDir = new File("." + File.separator);
            } else {
                config.sourceRepoDir = new File(sourceRepoDirPath);
            }
            return this;
        }
        
        /**
         * Sets the mode, overwriting all flags set before calling this method.
         * 
         * Default mode is {@link JosmanMode#dev}
         * 
         * @since 0.8.0
         */
        public Builder setMode(JosmanMode mode) {
            checkBuilt();
            checkNotNull(mode);
            config.mode = mode;
            
            switch (mode) {
            case dev:
                config.snapshot = true;                
                config.releases = false;
                config.failOnError = false;
                config.javadoc = false;                
                break;
            case ci:
                config.snapshot = true;
                config.releases = false;
                config.failOnError = false;
                config.javadoc = true;
                break;
            case staging:
                config.snapshot = true;
                config.releases = true;
                config.failOnError = true;
                config.javadoc = true;                
                break;
            case release:
                config.snapshot = false;
                config.releases = true;
                config.failOnError = true;
                config.javadoc = true;                
                break;
            default: 
                throw new JosmanException("Unrecognized mode: " + mode);
            }            
            return this;
        }
        

        /**
         * Returns the built object. A builder can build only one object,
         * calling twice this method will raise an exception.
         * 
         * @since 0.8.0
         */
        public JosmanConfig build() {
            checkBuilt();
            check();
            built = true;
            return config;
        }

        /**
         * @since 0.8.0
         */
        private void check() {
            checkArgument(!config.sourceRepoDir.getAbsolutePath()
                                               .equals(config.pagesDir.getAbsolutePath()),
                    "Source folder and target folder coincide! They are " + config.sourceRepoDir.getAbsolutePath());

        }

    }

    /**
     * Returns a new instance of a builder.
     * 
     * @since 0.8.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a string that can be displayed nicely in mojo log.
     * 
     * @since 0.8.0
     */
    @Override
    public String toString() {
        return "  JosmanConfig:"
                + "\n    mode            = " + mode
                + "\n    snapshot        = " + snapshot 
                + "\n    releases        = " + releases 
                + "\n    javadoc         = " + javadoc 
                + "\n    failOnErrors    = " + failOnError 
                 
                + "\n    ignoredVersions = " + ignoredVersions 
                + "\n";
    }

    
    
}