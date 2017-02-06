package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.TodUtils;
import eu.trentorise.opendata.josman.exceptions.ExprNotFoundException;
import eu.trentorise.opendata.josman.exceptions.JosmanException;
import eu.trentorise.opendata.josman.exceptions.JosmanIoException;
import eu.trentorise.opendata.josman.exceptions.JosmanNotFoundException;

import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jodd.jerry.Jerry;
import jodd.jerry.JerryFunction;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.RepositoryTag;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.parboiled.common.ImmutableList;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;

/**
 * Represents a Josman project, holding information about maven and git
 * repository.
 * 
 * @author David Leoni
 */
public class JosmanProject {

    private static final Logger LOG = Logger.getLogger(JosmanProject.class.getName());
    private static final int DEPTH = 8000;

    /**
     * Relative filepath of the eval map inside javadoc directory
     * 
     * NOTE:
     * <pre>
     * mvn javadoc:javadoc</pre>
     *  creates {@code target/site/apidocs}
     * while
     * <pre>
     *  mvn javadoc:jar 
     *  </pre> creates {@code target/apidocs} !!
     * <p>
     * Neither of those deletes existing extra files in those directories.
     * </p>
     * @since 0.8.0
     */
    public final static String RELATIVE_EVAL_FILEPATH = "resources/josman-eval.csv";

    /**
     * Filepath of the eval map inside maven target/apidocs javadoc directory
     * 
     * See {@link #RELATIVE_EVAL_FILEPATH} for details.
     * 
     * @since 0.8.0
     */
    public final static String TARGET_EVAL_FILEPATH = "target/apidocs/" + RELATIVE_EVAL_FILEPATH;

    /**
     * Folder in source code where user documentation is held.
     */
    public static final String DOCS_FOLDER = "docs";

    public static final String README_MD = "README.md";
    public static final String CHANGES_MD = "CHANGES.md";
    
    static final String JOSMAN_PROGRAM_LOGO_LINK = "#josman-program-logo-link";
    static final String JOSMAN_ORG_LOGO_LINK = "#josman-org-logo-link";

    private MavenProject mvnPrj;
    private boolean snapshotMode;    
    private File sourceRepoDir;
    private File pagesDir;
    private ImmutableList<SemVersion> ignoredVersions;

    private Repository repo;

    /**
     * Null means tags were not fetched. Notice we may also have fetched tags
     * and discovered there where none, so there might also be an empty array.
     */
    @Nullable
    private ImmutableList<RepositoryTag> repoTags;

    private PegDownProcessor pegDownProcessor;

    /**
     * @throws JosmanIoException
     * 
     */
    private static void deleteOutputVersionDir(File outputVersionDir, int major, int minor) {

        String versionName = "" + major + "." + minor;

        if (outputVersionDir.exists()) {
            // let's be strict before doing moronic things
            checkArgument(major >= 0);
            checkArgument(minor >= 0);
            if (outputVersionDir.getAbsolutePath()
                                .endsWith(versionName)) {
                LOG.info("Found already existing output dir, cleaning it...");
                try {
                    FileUtils.deleteDirectory(outputVersionDir);
                    LOG.info("Done cleaning directory.");
                } catch (Exception ex) {
                    throw new JosmanIoException("Error while deleting directory!", ex);
                }
            } else {
                throw new JosmanIoException("output path " + outputVersionDir.getAbsolutePath() + " doesn't end with '"
                        + versionName + "', avoiding cleaning it for safety reasons!");
            }
        }

    }

    /**
     * @deprecated just for debugging
     * @since 0.8.0
     */
    private void showArtifacts() {
        List<Artifact> testArtifacts = mvnPrj.getTestArtifacts();

        System.out.println("test artifacts...");
        for (Artifact art : testArtifacts) {
            System.out.println("test art jar = " + art.getFile()
                                                      .getAbsolutePath());
        }

        Set<Artifact> artifacts = mvnPrj.getArtifacts();

        System.out.println("artifacts...");
        for (Artifact art : artifacts) {
            System.out.println("test art jar = " + art.getFile()
                                                      .getAbsolutePath());
        }
    }

    /**
     *
     * @param snapshotMode
     *            if true the website generator will only process the
     *            current branch snapshot. Otherwise all released versions
     *            except
     *            {@code ignoredVersions} will be processed,
     *
     * @throws JosmanException
     * @since 0.8.0
     */
    public JosmanProject(
            MavenProject mvnPrj,
            String sourceRepoDirPath,
            String pagesDirPath,
            List<SemVersion> ignoredVersions,
            boolean snapshotMode) {

        checkNotNull(mvnPrj, "Invalid Maven project!");
        checkNotEmpty(mvnPrj.getUrl(), "Invalid url!");

        checkNotEmpty(mvnPrj.getArtifactId(), "Invalid artifactId!");
        checkArgument(!MavenProject.EMPTY_PROJECT_ARTIFACT_ID.equals(mvnPrj.getArtifactId()),
                "Invalid artifactId: " + mvnPrj.getArtifactId());
        checkNotEmpty(mvnPrj.getGroupId(), "Invalid groupId!");
        checkArgument(!MavenProject.EMPTY_PROJECT_GROUP_ID.equals(mvnPrj.getGroupId()),
                "Invalid groupId: " + mvnPrj.getGroupId());
        checkNotEmpty(mvnPrj.getVersion(), "Invalid version!");

        checkNotNull(sourceRepoDirPath, "Invalid repository source dir path!");
        checkNotEmpty(pagesDirPath, "Invalid pages dir path!");
        checkNotNull(ignoredVersions, "Invalid versions to ignore!");

        this.mvnPrj = mvnPrj;

        this.ignoredVersions = ImmutableList.copyOf(ignoredVersions);
        if (sourceRepoDirPath.isEmpty()) {
            this.sourceRepoDir = new File("." + File.separator);
        } else {
            this.sourceRepoDir = new File(sourceRepoDirPath);
        }
        
        this.snapshotMode = snapshotMode;
        this.pagesDir = new File(pagesDirPath);
        checkArgument(!sourceRepoDir.getAbsolutePath()
                                    .equals(pagesDir.getAbsolutePath()),
                "Source folder and target folder coincide! They are " + sourceRepoDir.getAbsolutePath());
        this.pegDownProcessor = new PegDownProcessor(
                Parser.QUOTES
                        | Parser.HARDWRAPS
                        | Parser.AUTOLINKS
                        | Parser.TABLES
                        | Parser.FENCED_CODE_BLOCKS
                        | Parser.WIKILINKS
                        | Parser.STRIKETHROUGH // not supported in netbeans flow
                                               // 2.0 yet
                        | Parser.ANCHORLINKS // not supported in netbeans flow
                                             // 2.0 yet
        );

        addClasspaths();
    }

    /**
     * Needed for $eval{cmd} so we can use test classes AND dependencies.
     * The solution is taken from
     * <a href="http://stackoverflow.com/a/16263482" target="_blank">here</a>
     * 
     * @since 0.8.0
     */
    private void addClasspaths() {

        try {
            Set<URL> urls = new HashSet<>();

            LOG.fine("test classpath elements...");

            List<String> elements = mvnPrj.getTestClasspathElements();
            // getRuntimeClasspathElements()
            // getCompileClasspathElements()
            // getSystemClasspathElements()
            for (String element : elements) {
                LOG.fine("test classpath element = " + element);
                try {
                    urls.add(new File(element).toURI()
                                              .toURL());
                } catch (MalformedURLException ex) {
                    throw new JosmanException("Something went wrong!", ex);
                }
            }

            ClassLoader contextClassLoader = URLClassLoader.newInstance(
                    urls.toArray(new URL[0]),
                    Thread.currentThread()
                          .getContextClassLoader());

            Thread.currentThread()
                  .setContextClassLoader(contextClassLoader);

        } catch (DependencyResolutionRequiredException ex) {
            throw new JosmanException(ex);
        }

    }

    private File sourceDocsDir() {
        return new File(sourceRepoDir, DOCS_FOLDER);
    }

    private File targetJavadocDir(SemVersion version) {
        return new File(targetVersionDir(version), "javadoc");
    }

    private File sourceJavadocDir(SemVersion version) {
        if (snapshotMode) {

            // NOTE: mvn javadoc:javadoc creates target/site/apidocs
            // and mvn javadoc:jar creates target/apidocs !!

            return new File(sourceRepoDir, "target/apidocs");
        } else {
            throw new UnsupportedOperationException("todo non-local javadoc not supported yet");
        }
    }

    static String programLogoName(String repoName) {
        return repoName + "-200px.png";
    }

        
    static File programLogo(File sourceDocsDir, String repoName) {
        return new File(sourceDocsDir, "img" + File.separator + programLogoName(repoName));
    }
    
    /**
     * @since 0.8.0
     */    
    static String orgLogoName(String repoName) {
        return repoName + "-org-200px.png";
    }    
    
    /**
     * @since 0.8.0
     */
    static File orgLogo(File sourceDocsDir, String repoName) {
        return new File(sourceDocsDir, "img" + File.separator + orgLogoName(repoName));
    }


    /**
     * Copies provided stream to destination, which is determined according to
     * {@code relPath}, {@code preprendedPath} and {@code version}
     *
     * @param sourceStream
     * @param relPath
     *            path relative to root, i.e. docs/README.md or
     *            img/mypic.jpg
     *
     * @param version
     * @return the target file
     * 
     * @throws JosmanIoException
     */
    File copyStream(
            InputStream sourceStream,
            String relPath,
            final SemVersion version,
            List<String> relPaths,
            Map<String, String> evals) {

        checkNotNull(sourceStream, "Invalid source stream!");
        checkNotEmpty(relPath, "Invalid relative path!");
        checkNotEmpty(relPaths, "Invalid relative paths!");
        checkNotNull(version);

        File targetFile = Josmans.targetFile(pagesDir, relPath, version);

        if (targetFile.exists()) {
            throw new JosmanIoException("Target file already exists! "
                    + targetFile.getAbsolutePath());
        }

        if (relPath.endsWith(".md")) {

            LOG.log(Level.INFO, "Creating file {0}", targetFile.getAbsolutePath());
            if (targetFile.exists()) {
                throw new JosmanIoException("Target file already exists! Target is " + targetFile.getAbsolutePath());
            }
            copyMdAsHtml(sourceStream, relPath, version, relPaths, evals);
        } else {

            LOG.log(Level.INFO, "Copying file into {0}", targetFile.getAbsolutePath());

            try {
                FileUtils.copyInputStreamToFile(sourceStream, targetFile);
                sourceStream.close();
                LOG.info("Done copying file.");
            } catch (Exception ex) {
                throw new JosmanIoException("Error while copying stream to file!", ex);
            }
        }
        return targetFile;
    }
    
    
     

    /**
     * Writes an md stream as html to outputFile
     *
     * @param outputFile
     *            Must not exist. Eventual needed directories in the path
     *            will be created
     * @param relPath
     *            path relative to {@link #sourceRepoDir}, i.e.
     *            img/mypic.jpg or docs/README.md
     * @param version
     *            The version the md page refers to.
     * @param relpaths
     *            a list of relative paths for the sidebar
     * 
     * @throws JosmanIoException
     */
    void copyMdAsHtml(
            InputStream sourceMdStream,
            String relPath,
            final SemVersion version,
            List<String> relpaths,
            Map<String, String> evals) {

        checkNotNull(version);
        checkNotEmpty(relPath, "Invalid relative path!");
        checkNotEmpty(relpaths, "Invalid relative paths!");

        final String prependedPath = Josmans.prependedPath(relPath);

        File targetFile = Josmans.targetFile(pagesDir, relPath, version);

        if (targetFile.exists()) {
            throw new JosmanIoException("Trying to write md file to target that already exists!! Target is "
                    + targetFile.getAbsolutePath());
        }

        String sourceMdString;
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(sourceMdStream, writer, "UTF-8"); // todo fixed
                                                           // encoding...
            sourceMdString = writer.toString();
        } catch (Exception ex) {
            throw new JosmanIoException(
                    "Couldn't read source md stream! Target path is " + targetFile.getAbsolutePath(), ex);
        }

        Josmans.checkNotMeaningful(sourceMdString, "Invalid source md file: " + relPath);

        String filteredSourceMdString = sourceMdString
                                                        // '#' for legacy compat
                                                      .replaceAll("#\\{version}", version.toString())
                                                      .replaceAll("#\\{majorMinorVersion}", Josmans.majorMinor(version))
                                                      .replaceAll("#\\{repoRelease}",
                                                              Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()),
                                                                      mvnPrj.getArtifactId(), version))
                                                      .replaceAll("#'\\{version}", "#{version}")
                                                      .replaceAll("#'\\{majorMinorVersion}", "#{majorMinorVersion}")
                                                      .replaceAll("#'\\{repoRelease}", "#{repoRelease}")                                                       
                                                      
                                                      .replaceAll("jedoc", "josman")                                                                                                          
                                                       
                                                       .replaceAll("\\$\\{josman.majorMinorVersion}", Josmans.majorMinor(version))
                                                       .replaceAll("\\$\\{josman.repoRelease}",
                                                                Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()),
                                                                        mvnPrj.getArtifactId(), version))
                                                       .replaceAll("\\$'\\{josman.majorMinorVersion}", "\\${josman.majorMinorVersion}")
                                                       .replaceAll("\\$'\\{josman.repoRelease}", "\\${josman.repoRelease}");                                                       
        // injecting maven pro
        Properties props = mvnPrj.getProperties();

        if (props != null){
            
            for (Object obj : props.keySet()){
                String key = (String) obj;
                String prop = props.getProperty(key);
                
                filteredSourceMdString = replaceProperty(filteredSourceMdString, key, prop);                
            }            
        }
                       
        Scm scm;
        if (mvnPrj.getScm() == null){
            scm = new Scm();
            scm.setConnection("");
            scm.setDeveloperConnection("");
            scm.setTag("");
            scm.setUrl("");
        } else {
            scm = mvnPrj.getScm();
        }
        filteredSourceMdString = replaceProperties(                
                filteredSourceMdString, "project.artifactId", mvnPrj.getArtifactId(),
                "project.groupId", mvnPrj.getGroupId(),
                "project.description", mvnPrj.getDescription(),
                "project.name", mvnPrj.getName(),
                "project.version", mvnPrj.getVersion(),
                "project.url", mvnPrj.getUrl(),
                "project.basedir", mvnPrj.getBasedir() == null ? "" : mvnPrj.getBasedir().getAbsolutePath(),
                "project.scm.connection", scm.getConnection(),
                "project.scm.developerConnection", scm.getDeveloperConnection(),
                "project.scm.tag", scm.getTag(),
                "project.scm.url", scm.getUrl(),                
                "pom.artifactId", mvnPrj.getArtifactId(),                
                "pom.groupId", mvnPrj.getGroupId(),
                "pom.description", mvnPrj.getDescription(),
                "pom.name", mvnPrj.getName(),
                "pom.version", mvnPrj.getVersion()); 
        
        filteredSourceMdString = Josmans.expandExprs(filteredSourceMdString,
                    evals,
                    relPath,
                    Thread.currentThread()
                          .getContextClassLoader(),
                           snapshotMode);

        String skeletonString;
        try {
            StringWriter writer = new StringWriter();
            InputStream stream = Josmans.findResourceStream("/skeleton.html");
            IOUtils.copy(stream, writer, "UTF-8");
            skeletonString = writer.toString();
        } catch (Exception ex) {
            throw new JosmanIoException("Couldn't read skeleton file!", ex);
        }

        String skeletonStringFixedPaths;
        if (Josmans.isRootpath(relPath)) {
            skeletonStringFixedPaths = skeletonString;
        } else {
            // fix paths
            skeletonStringFixedPaths = skeletonString.replaceAll("src=\"js/", "src=\"../js/")
                                                     .replaceAll("src=\"img/", "src=\"../img/")
                                                     .replaceAll("href=\"css/", "href=\"../css/");

        }

        Jerry skeleton = Jerry.jerry(skeletonStringFixedPaths);
        skeleton.$("title")
                .text(mvnPrj.getName());
        String contentFromMdHtml = pegDownProcessor.markdownToHtml(filteredSourceMdString);
        Jerry contentFromMd = Jerry.jerry(contentFromMdHtml);

        contentFromMd.$("a")
                     .each(new JerryFunction() {

                         @Override
                         public boolean onNode(Jerry arg0, int arg1) {
                             String href = arg0.attr("href");
                             if (href.startsWith(prependedPath + "src")) {
                                 arg0.attr("href",
                                         href.replace(prependedPath + "src",
                                                 Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()),
                                                         mvnPrj.getArtifactId(), version) + "/src"));
                                 return true;
                             }
                             if (href.endsWith(".md")) {
                                 arg0.attr("href", Josmans.htmlizePath(href));
                                 return true;
                             }

                             if (href.equals(prependedPath + "../../wiki")) {
                                 arg0.attr("href", href.replace(prependedPath + "../../wiki", Josmans.repoWiki(
                                         Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                                 return true;
                             }

                             if (href.equals(prependedPath + "../../issues")) {
                                 arg0.attr("href", href.replace(prependedPath + "../../issues", Josmans.repoIssues(
                                         Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                                 return true;
                             }

                             if (href.equals(prependedPath + "../../milestones")) {
                                 arg0.attr("href",
                                         href.replace(prependedPath + "../../milestones", Josmans.repoMilestones(
                                                 Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                                 return true;
                             }

                             if (TodUtils.removeTrailingSlash(href)
                                         .equals(DOCS_FOLDER)) {
                                 arg0.attr("href", Josmans.majorMinor(version) + "/index.html");
                                 return true;
                             }

                             if (href.startsWith(DOCS_FOLDER + "/")) {
                                 arg0.attr("href", Josmans.majorMinor(version) + href.substring(DOCS_FOLDER.length()));
                                 return true;
                             }

                             return true;
                         }
                     });

        contentFromMd.$("img")
                     .each(new JerryFunction() {

                         @Override
                         public boolean onNode(Jerry arg0, int arg1) {
                             String src = arg0.attr("src");
                             if (src.startsWith(DOCS_FOLDER + "/")) {
                                 arg0.attr("src", Josmans.majorMinor(version) + src.substring(DOCS_FOLDER.length()));
                                 return true;
                             }
                             return true;
                         }
                     });

        skeleton.$("#josman-internal-content")
                .html(contentFromMd.html());

        skeleton.$("#josman-repo-link")
                .html(mvnPrj.getName())
                .attr("href", prependedPath + "index.html");

        File programLogo = programLogo(sourceDocsDir(), mvnPrj.getArtifactId());

        if (programLogo.exists()) {
            skeleton.$("#josman-program-logo")
                    .attr("src", prependedPath + "img/" + programLogoName(mvnPrj.getArtifactId()));
            skeleton.$(JOSMAN_PROGRAM_LOGO_LINK)
                    .attr("href", prependedPath + "index.html");
        } else {
            LOG.warning("Couldn't find program logo in " + programLogo.getAbsolutePath());
            skeleton.$(JOSMAN_PROGRAM_LOGO_LINK)
                    .css("display", "none");
        }

        File orgLogo = orgLogo(sourceDocsDir(), mvnPrj.getArtifactId());

        if (orgLogo.exists()) {
            skeleton.$("#josman-org-logo")
                    .attr("src", prependedPath + "img/" + orgLogoName(mvnPrj.getArtifactId()));
            
            skeleton.$(JOSMAN_ORG_LOGO_LINK)
                    .attr("href", mvnPrj.getOrganization().getUrl())
                    .attr("title", mvnPrj.getOrganization().getName());
        } else {
            LOG.warning("Couldn't find organization logo in " + orgLogo.getAbsolutePath());
            skeleton.$(JOSMAN_ORG_LOGO_LINK)
                    .css("display", "none");
        }
        
        

        skeleton.$("#josman-wiki")
                .attr("href", Josmans.repoWiki(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId()));
        skeleton.$("#josman-project")
                .attr("href", Josmans.repoUrl(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId()));

        skeleton.$("#josman-home")
                .attr("href", prependedPath + "index.html");
        if (Josmans.isRootpath(relPath)) {
            skeleton.$("#josman-home")
                    .addClass("josman-tag-selected");
        }

        // cleaning example versions
        skeleton.$(".josman-version-tab-header")
                .remove();

        List<RepositoryTag> tags = new ArrayList<RepositoryTag>(
                Josmans.versionTagsToProcess(mvnPrj.getArtifactId(), repoTags, ignoredVersions)
                       .values());
        Collections.reverse(tags);

        if (Josmans.isRootpath(relPath)) {
            skeleton.$("#josman-internal-sidebar")
                    .text("");
            skeleton.$("#josman-sidebar-managed-block")
                    .css("display", "none");
        } else {
            Jerry sidebar = makeSidebar(contentFromMdHtml, relPath, relpaths);
            skeleton.$("#josman-internal-sidebar")
                    .html(sidebar.htmlAll(true));
        }

        skeleton.$(".josman-to-strip")
                .remove();

        if (snapshotMode) {

            if (tags.size() > 0) {
                SemVersion ver = Josmans.version(mvnPrj.getArtifactId(), tags.get(0)
                                                                             .getName());
                if (version.getMajor() >= ver.getMajor()
                        && version.getMinor() >= ver.getMinor()) {
                    addVersionHeaderTag(skeleton, prependedPath, version, prependedPath.length() != 0);
                }

            } else {
                addVersionHeaderTag(skeleton, prependedPath, version, prependedPath.length() != 0);
            }

        } else {

            for (RepositoryTag tag : tags) {
                SemVersion ver = Josmans.version(mvnPrj.getArtifactId(), tag.getName());
                addVersionHeaderTag(
                        skeleton,
                        prependedPath,
                        ver,
                        !Josmans.isRootpath(relPath) && ver.equals(version));
            }

            Pattern p = Pattern.compile("todo", Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(skeleton.html());
            if (matcher.find()) {
                // throw new JosmanIoException("Found '" + matcher.group() + "'
                // string in stream for " + targetFile.getAbsolutePath() + " (at
                // position " + matcher.start() + ")");
                LOG.warning("Found '" + matcher.group() + "' string in stream for " + targetFile.getAbsolutePath());
            }
        }

        if (!targetFile.getParentFile()
                       .exists()) {
            if (!targetFile.getParentFile()
                           .mkdirs()) {
                throw new JosmanIoException(
                        "Couldn't create target directories to host processed md file " + targetFile.getAbsolutePath());
            }
        }

        try {

            FileUtils.write(targetFile, skeleton.html());
        } catch (Exception ex) {
            throw new JosmanIoException("Couldn't write into " + targetFile.getAbsolutePath() + "!", ex);
        }

    }
    
    /**
     * Replaces key / value properties in {@code text}
     * 
     * @since 0.8.0
     */
    // todo not efficient
    private String replaceProperties(String text, String... props) {
        
        String ret = text;       
        
        checkArgument(props.length % 2 == 0, 
                "Props must be an even sequence of key/value pairs!"
                + " Found instead " + props.length + " elements: " + props);
        
        for (int i = 0; i < props.length; i += 2){
            String key = props[i];
            String prop = props[i+1];
            ret = replaceProperty(ret, key, prop);
        }
        return ret;
    }

    /**
     * @since 0.8.0
     */
    private String replaceProperty(String text, String key, String prop) {        
        return text
                .replaceAll("\\$\\{" + key + "}", prop)
                .replaceAll("\\$'\\{" + key + "}", "\\${" + key + "}");                
        
    }

    private static void addVersionHeaderTag(Jerry skeleton, String prependedPath, SemVersion version,
            boolean selected) {
        String verShortName = Josmans.majorMinor(version);
        String classSelected = selected ? "josman-tag-selected" : "";
        skeleton.$("#josman-usage")
                .append(
                        "<a class='josman-version-tab-header " + classSelected + "' href='"
                                + prependedPath
                                + verShortName
                                + "/index.html'>" + verShortName + "</a>");
    }

    private void buildIndex(SemVersion latestVersion, Map<String, String> evals) {
        try {
            File sourceMdFile = new File(sourceRepoDir, README_MD);
            copyMdAsHtml(new FileInputStream(sourceMdFile), README_MD, latestVersion, ImmutableList.of(README_MD),
                    evals);
        } catch (FileNotFoundException ex) {
            throw new JosmanException("Error while building index!", ex);
        }
    }

    private File targetVersionDir(SemVersion semVersion) {
        checkNotNull(semVersion);
        return new File(pagesDir, "" + semVersion.getMajor() + "." + semVersion.getMinor());
    }

    /**
     * Returns the directory where docs about the latest version will end up.
     */
    private File targetLatestDocsDir() {
        return new File(pagesDir, "latest");
    }

    /**
     * Processes a directory 'docs' that holds documentation for a given version
     * of the software
     */
    private void processDocsDir(SemVersion version, Map<String, String> evals) {
        checkNotNull(version);

        if (!sourceDocsDir().exists()) {
            throw new JosmanIoException("Can't find source dir! " + sourceDocsDir().getAbsolutePath());
        }
       
        
        File targetVersionDir = targetVersionDir(version);

        deleteOutputVersionDir(targetVersionDir, version.getMajor(), version.getMinor());

        List<String> relPaths = new ArrayList<String>();
        File[] files = sourceDocsDir().listFiles();
        // If this pathname does not denote a directory, then listFiles()
        // returns null.

        for (File file : files) {
            if (file.isFile() && file.getName()
                                     .endsWith(".md")) {
                relPaths.add(DOCS_FOLDER + "/" + file.getName());
            }
        }

        DirWalker dirWalker = new DirWalker(
                sourceDocsDir(),
                targetVersionDir,
                this,
                version,
                relPaths,
                evals);
        dirWalker.process();
        
    }

    /**
     * @param path
     *            the exact path. Careful: must be *exact* dir name (i.e.
     *            'docs' will work for docs/a.txt but 'doc' won't work)
     * @throws JosmanIoException
     *             on error
     */
    private TreeWalk makeGitDocsWalk(RevTree tree, String path) {
        checkNotNull(tree);
        try {
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            treeWalk.setFilter(PathFilter.create(path));
            return treeWalk;
        } catch (Exception ex) {
            throw new JosmanIoException(ex);
        }
    }

    /**
     * Processes a directory 'docs' at tag repoName-version that holds
     * documentation for a given version of the software
     * 
     * @throws JosmanIoException
     */
    private void processGitDocsDir(SemVersion version, Map<String, String> evals) {
        checkNotNull(version);
        checkNotNull(evals);

        checkNotNull(repo);

        String releaseTag = Josmans.releaseTag(mvnPrj.getArtifactId(), version);

        try {

            ObjectId lastCommitId = repo.resolve(releaseTag);

            // a RevWalk allows to walk over commits based on some filtering
            // that is defined
            DepthWalk.RevWalk revWalk = new DepthWalk.RevWalk(repo, DEPTH);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();

            TreeWalk relPathsWalk = makeGitDocsWalk(tree, DOCS_FOLDER);

            List<String> relpaths = new ArrayList<String>();
            while (relPathsWalk.next()) {
                String pathString = relPathsWalk.getPathString();

                if (pathString.endsWith(".md")) {
                    FileMode fileMode = relPathsWalk.getFileMode(0);
                    LOG.log(Level.FINE, "Collecting {0}:  mode: {1}, type: {2}", new Object[] { pathString,
                            Josmans.gitFileModeToString(fileMode), fileMode.getObjectType() });
                    relpaths.add(pathString);
                }
            }

            String path;
            List<String> fixedRelpaths;

            if (relpaths.isEmpty()) {
                LOG.log(Level.WARNING, "COULDN''T FIND ANY FILE IN " + DOCS_FOLDER
                        + " for version {0}! TRYING TO USE README.md instead", version);
                path = README_MD;
            } else {
                path = DOCS_FOLDER;
            }

            TreeWalk treeWalk = makeGitDocsWalk(tree, path);
            while (treeWalk.next()) {

                String pathString = treeWalk.getPathString();

                FileMode fileMode = treeWalk.getFileMode(0);
                LOG.log(Level.FINE, "{0}:  mode: {1}, type: {2}",
                        new Object[] { pathString, Josmans.gitFileModeToString(fileMode), fileMode.getObjectType() });

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(objectId);

                InputStream stream = loader.openStream();

                if (relpaths.isEmpty()) {
                    copyStream(stream,
                            DOCS_FOLDER + "/" + pathString, version,
                            ImmutableList.of(DOCS_FOLDER + "/" + path),
                            evals);
                } else {
                    copyStream(stream, pathString, version, relpaths, evals);
                }

            }

        } catch (Exception ex) {
            throw new JosmanIoException("Error while extracting docs from git local repo at commit " + releaseTag, ex);
        }

    }

    /**
     * Copies target version to 'latest' directory, cleaning it before the copy
     *
     * @param version
     * 
     * @throws JosmanIoException
     */
    private void createLatestDocsDirectory(SemVersion version) {

        File targetLatestDocsDir = targetLatestDocsDir();
        LOG.log(Level.INFO, "Creating latest docs directory {0}", targetLatestDocsDir.getAbsolutePath());

        if (!targetLatestDocsDir.getAbsolutePath()
                                .endsWith("latest")) {
            throw new JosmanIoException("Trying to delete a latest docs dir which doesn't end with 'latest'!");
        }
        try {
            LOG.log(Level.INFO, "Deleting directory {0}  ...", targetLatestDocsDir.getAbsolutePath());
            FileUtils.deleteDirectory(targetLatestDocsDir);
            LOG.log(Level.INFO, "Done deleting directory.");
            LOG.log(Level.INFO, "Copying files from directory {0} to {1}  ...",
                    new Object[] { targetVersionDir(version).getAbsolutePath(),
                            targetLatestDocsDir.getAbsolutePath() });
            FileUtils.copyDirectory(targetVersionDir(version), targetLatestDocsDir);
            LOG.log(Level.INFO, "Done copying directory.");
        } catch (Throwable tr) {
            throw new JosmanIoException("Error while creating latest docs directory ", tr);
        }

    }

    /**
     * Returns true if the website generator will only process the current
     * branch snapshot. Otherwise all released versions will be processed.
     *
     */
    public boolean isSnapshotMode() {
        return snapshotMode;
    }

    /**
     * @throws JosmanNotFoundException
     * @throws JosmanException
     */
    public void generateSite() {

        MavenXpp3Reader reader = new MavenXpp3Reader();

        try {
            File repoFile = new File(sourceRepoDir, ".git");

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repo = builder.setGitDir(repoFile)
                          .readEnvironment() // scan environment GIT_* variables
                          .build();
        } catch (Exception ex) {
            throw new JosmanException("Error while reading local git repo!", ex);
        }

        LOG.log(Level.INFO, "Cleaning target: {0}  ....", pagesDir.getAbsolutePath());
        if (!pagesDir.getAbsolutePath()
                     .endsWith("site")) {
            throw new JosmanException("target directory does not end with 'site': " + pagesDir.getAbsolutePath());
        }
        try {
            FileUtils.deleteDirectory(pagesDir);
            LOG.info("Done deleting directory");
        } catch (IOException ex) {
            throw new JosmanException("Error while deleting directory " + pagesDir.getAbsolutePath(), ex);
        }

        SemVersion snapshotVersion = SemVersion.of(mvnPrj.getVersion())
                                               .withPreReleaseVersion("");

        if (snapshotMode) {
            LOG.log(Level.INFO, "Processing local version");
            File evalMap = new File(sourceRepoDir, "target/apidocs/" + RELATIVE_EVAL_FILEPATH);
            @Nullable
            Map<String, String> curEvals;
            if (evalMap.exists()) {
                curEvals = Josmans.loadEvalMap(evalMap);
            } else {
                LOG.info("Couldn't find evals map (to create it just run the tests): " + evalMap.getAbsolutePath());
                curEvals = Collections.EMPTY_MAP;
            }

            copyJavadoc(snapshotVersion);
            try {
                buildIndex(snapshotVersion, curEvals);
                processDocsDir(snapshotVersion, curEvals);
            } catch (ExprNotFoundException ex) {
                throw new ExprNotFoundException("SNAPSHOT VERSION IS MISSING EVALUATED EXPRESSION: "
                        + ex.getExpr() + " FOUND IN FILE " + ex.getRelPath()+ "\n!!!!!!   MAYBE YOU FORGOT TO RUN   mvn josman:eval ? \n\n", ex.getExpr(), ex.getRelPath());
            }
            
            createLatestDocsDirectory(snapshotVersion);


        } else {
            LOG.log(Level.INFO, "Fetching {0}/{1} tags.",
                    new Object[] { Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId() });

            repoTags = Josmans.fetchTags(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId());

            if (repoTags.isEmpty()) {
                throw new JosmanNotFoundException("There are no tags at all in the repository!!");
            }
            SemVersion latestPublishedVersion = Josmans.latestVersion(mvnPrj.getArtifactId(), repoTags);
            LOG.log(Level.INFO, "Processing published version");
            String curBranch = Josmans.readRepoCurrentBranch(sourceRepoDir);

            SortedMap<String, RepositoryTag> filteredTags = Josmans.versionTagsToProcess(mvnPrj.getArtifactId(),
                    repoTags, ignoredVersions);

            for (RepositoryTag tag : filteredTags.values()) {
                LOG.log(Level.INFO, "Processing release tag {0}", tag.getName());
                SemVersion version = Josmans.version(mvnPrj.getArtifactId(), tag.getName());
                copyJavadoc(version); // before processGit so we can have the
                                      // eval file ready

                Map<String, String> evals;
                File evalMapFile = new File(targetJavadocDir(version), RELATIVE_EVAL_FILEPATH);
                if (evalMapFile.exists()) {
                    evals = Josmans.loadEvalMap(evalMapFile);
                } else {
                    evals = Collections.EMPTY_MAP;
                }
                try {
                    processGitDocsDir(version, evals);
                } catch (ExprNotFoundException ex) {
                    throw new ExprNotFoundException(
                            "RELEASED VERSION " + version + " IS MISSING EVALUATION OF EXPRESSION: " + ex.getExpr()
                            + " FOUND IN FILE: " + ex.getRelPath(),
                            ex.getExpr(),
                            ex.getRelPath(),
                            ex);
                }
            }

            File evalMap = new File(targetJavadocDir(latestPublishedVersion), RELATIVE_EVAL_FILEPATH);
            Map<String, String> latestPublishedEvals;
            if (evalMap.exists()) {
                latestPublishedEvals = Josmans.loadEvalMap(evalMap);
            } else {
                LOG.info("Couldn't find eval map (if docs don't contain $eval it's not necessary).");
                latestPublishedEvals = Collections.EMPTY_MAP;
            }

            try {

                buildIndex(latestPublishedVersion, latestPublishedEvals);
            } catch (ExprNotFoundException ex) {
                throw new ExprNotFoundException("RELEASED VERSION " + latestPublishedVersion
                        + " IS MISSING EVALUATION OF EXPRESSION: " + ex.getExpr()
                        + " FOUND IN FILE " + ex.getRelPath(),
                        ex.getExpr(),
                        ex.getRelPath(),
                        ex);
            }

        }

        Josmans.copyDirFromResource(Josmans.class, "/website-template", pagesDir);       

            File targetImgDir = new File(pagesDir, "img");

            try {
            File programLogo = programLogo(sourceDocsDir(), mvnPrj.getArtifactId());

            if (programLogo.exists()) {
                LOG.log(Level.INFO, "Found program logo: {0}", programLogo.getAbsolutePath());
                LOG.log(Level.INFO, "      copying it into dir {0}", targetImgDir.getAbsolutePath());

                FileUtils.copyFile(programLogo, new File(targetImgDir, programLogoName(mvnPrj.getArtifactId())));
            }
            } catch (Exception ex){
                if (snapshotMode){
                    LOG.severe("COULDN'T COPY THE PROGRAM LOGO!");
                    LOG.log(Level.FINE,"Exception was: ", ex);
                } else {
                    throw new JosmanException("Error while copying files!", ex);
                }                
            }

            try {
            File orgLogo = orgLogo(sourceDocsDir(), mvnPrj.getArtifactId());

            if (orgLogo.exists()) {
                LOG.log(Level.INFO, "Found org logo: {0}", orgLogo.getAbsolutePath());
                LOG.log(Level.INFO, "      copying it into dir {0}", targetImgDir.getAbsolutePath());

                FileUtils.copyFile(orgLogo, new File(targetImgDir, orgLogoName(mvnPrj.getArtifactId())));
            }
            } catch (Exception ex){
                if (snapshotMode){
                    LOG.severe("COULDN'T COPY THE organization logo!");
                    LOG.log(Level.FINE,"Exception was: ", ex);
                } else {
                    throw new JosmanException("Error while copying files!", ex);
                }                
            }

            try {
            FileUtils.copyFile(new File(sourceRepoDir, "LICENSE.txt"), new File(pagesDir, "LICENSE.txt"));

            } catch (Exception ex) {
                if (snapshotMode){
                    LOG.severe("COULDN'T COPY THE LICENCE.txt FILE!");
                    LOG.log(Level.FINE,"Exception was: ", ex);
                } else {
                    throw new JosmanException("Error while copying files!", ex);
                }
            }

        LOG.log(Level.INFO, "\n\nYou can now browse the website at {0}/index.html\n\n", pagesDir.getAbsolutePath());
    }

    /**
     * Returns a Jerry object repereenting the non-managed sidebar for a given
     * version page
     *
     * @param contentFromMdHtml
     *            the content of page we're making the sidebar for
     * @param relpaths
     *            a list of relpaths of pages related to the version we're
     *            making the sidebar for
     * @param currentRelPath
     *            the relpath of the page we're making the sidebar
     *            for
     */
    private Jerry makeSidebar(String contentFromMdHtml, String currentRelPath, List<String> relpaths) {
        checkNotNull(contentFromMdHtml);
        checkNotEmpty(currentRelPath, "Invalid current rel path!");
        checkNotEmpty(relpaths, "Invalid list of relpaths!");

        Jerry html = Jerry.jerry(contentFromMdHtml);

        Jerry allLinksContainer = Jerry.jerry("<ul>")
                                       .$("ul")
                                       .addClass("josman-tree");

        List<String> orderedRelpaths = Josmans.orderRelpaths(relpaths);

        for (String relpath : orderedRelpaths) {
            Jerry pageItemContainer = Jerry.jerry("<li>")
                                           .$("li");

            Jerry pageTitle = Jerry.jerry("<div>")
                                   .$("div")
                                   .addClass("josman-sidebar-page-title");

            Jerry pageLinksContainer = Jerry.jerry("<ul>")
                                            .$("ul")
                                            .addClass("josman-tree");

            if (relpath.equals(currentRelPath)) {
                pageTitle.text(Josmans.targetName(relpath));
                pageTitle.addClass("josman-sidebar-selected");

                for (Jerry sourceHeaderLink : html.$("h3 a")) {
                    // <a href="#header1">Header 1</a><br/>

                    /*
                     * <ul class="josman-tree">
                     * <li>
                     * <div class="josman-sidebar-page-title">Usage</div>
                     * <ul class="josman-tree">
                     * <li><a href="#header1">Maven</a>
                     */
                    Jerry linkContainer = Jerry.jerry("<div>")
                                               .$("div");

                    Jerry link = Jerry.jerry("<a>")
                                      .$("a")
                                      .attr("href", sourceHeaderLink.first()
                                                                    .first()
                                                                    .attr("href"))
                                      .text(sourceHeaderLink.first()
                                                            .text());
                    linkContainer.append(link.htmlAll(true));

                    pageLinksContainer.append(linkContainer.htmlAll(true));

                    // ret += "<div> <a href='" +
                    // sourceHeaderLink.first().first().attr("href") + "'>" +
                    // sourceHeaderLink.first().text() + "</a></div> \n";
                    /*
                     * Jerry.jerry("<a>")
                     * .attr("href","#" + sourceHeaderLink.attr("id"))
                     * .text(sourceHeaderLink.text());
                     */
                }
            } else {
                pageTitle.append(Jerry.jerry("<a>")
                                      .$("a")
                                      .attr("href", Josmans.htmlizePath(relpath.substring(DOCS_FOLDER.length() + 1)))
                                      .text(Josmans.targetName(relpath))
                                      .htmlAll(true));
            }

            pageItemContainer.append(pageTitle.htmlAll(true));
            pageItemContainer.append(pageLinksContainer.htmlAll(true));

            allLinksContainer.append(pageItemContainer.htmlAll(true));
        }

        return allLinksContainer;
    }

    /**
     * Copies javadoc into target website according to the artifact version.
     * 
     * @throws JosmanIoException
     * @throws JosmanNotFoundException
     */
    private void copyJavadoc(SemVersion version) {
        File targetJavadoc = targetJavadocDir(version);
        if (targetJavadoc.exists() && (targetJavadoc.isFile() || targetJavadoc.length() > 0)) {
            throw new JosmanIoException(
                    "Target directory for Javadoc already exists!!! " + targetJavadoc.getAbsolutePath());
        }
        if (snapshotMode) {
            File sourceJavadoc = sourceJavadocDir(version);
            if (sourceJavadoc.exists()) {

                try {
                    LOG.log(Level.INFO, "Now copying Javadoc from {0} to {1} ...",
                            new Object[] { sourceJavadoc.getAbsolutePath(), targetJavadoc.getAbsolutePath() });
                    FileUtils.copyDirectory(sourceJavadoc, targetJavadoc);
                    LOG.info("Done copying javadoc.");
                } catch (Exception ex) {
                    throw new JosmanIoException("Error while copying Javadoc from " + sourceJavadoc.getAbsolutePath()
                            + " to " + targetJavadoc.getAbsolutePath(), ex);
                }
            } else {
                LOG.log(Level.INFO, "Couldn''t find javadoc, skipping it. Looked in {0}",
                        sourceJavadoc.getAbsolutePath());
            }
        } else {
            File jardocs;
            try {
                jardocs = Josmans.fetchJavadoc(mvnPrj.getGroupId(), mvnPrj.getArtifactId(), version);
            } catch (Exception ex) {
                String sep = File.separator;
                String localJarPath = sourceRepoDir.getAbsolutePath() + sep + "target" + sep + "checkout" + sep
                        + "target" + sep + Josmans.javadocJarName(mvnPrj.getArtifactId(), version);
                LOG.log(Level.WARNING,
                        "Error while fetching javadoc from Maven Central, trying to locate it at " + localJarPath, ex);
                jardocs = new File(localJarPath);
                if (!jardocs.exists()) {
                    throw new JosmanNotFoundException("Couldn't find any jar for javadoc!");
                }
            }
            Josmans.copyDirFromJar(jardocs, targetJavadocDir(version), "");
        }

    }

    public String getRepoName() {
        return mvnPrj.getArtifactId();
    }

    public ImmutableList<SemVersion> getIgnoredVersions() {
        return ignoredVersions;
    }

    /**
     * Searches docs for $eval{EXPR} staments and evaluates them using test classpath. 
     * Results are then put in CSV file {@value #TARGET_EVAL_FILEPATH}    
     * 
     * @since 0.8.0
     */
    public void evalDocs() {


        final Map<String, String> evals = new HashMap<>();

        new DirectoryWalker() {
            /**
             * @throws JosmanIoException
             */
            public void process() {
                try {
                    walk(sourceDocsDir(), new ArrayList());
                } catch (IOException ex) {
                    throw new JosmanIoException("Error while walking docs " + sourceDocsDir().getAbsolutePath(), ex);
                }
            }

            @Override
            protected boolean handleDirectory(File directory, int depth, Collection results) {
                LOG.log(Level.INFO, "Processing directory {0}", directory.getAbsolutePath());
                return true;
            }

            @Override
            protected void handleFile(File file, int depth, Collection results) throws IOException {

                if (file.getAbsolutePath()
                        .endsWith(".md")) {
                    String text = FileUtils.readFileToString(file, "UTF-8");
                    evals.putAll(Josmans.evalExprsInText(text, 
                                                            file.getPath(),
                                                            Thread.currentThread()
                                                             .getContextClassLoader(),
                                                             snapshotMode));
                }
            }

        }.process();

        Josmans.saveEvalMap(evals, new File(sourceRepoDir, TARGET_EVAL_FILEPATH));

    }

    /**
     * @since 0.8.0
     */
    public Map<String, String> loadEvalMap(SemVersion version) {
        checkNotNull(version);
        return Josmans.loadEvalMap(new File(targetJavadocDir(version), RELATIVE_EVAL_FILEPATH));
    }

}
