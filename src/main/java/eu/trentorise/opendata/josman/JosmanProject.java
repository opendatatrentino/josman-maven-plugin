package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.NotFoundException;
import eu.trentorise.opendata.commons.TodUtils;


import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import jodd.jerry.Jerry;
import jodd.jerry.JerryFunction;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.RepositoryTag;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
 * Represents a Josman project, holding information about maven and git repository.
 * @author David Leoni
 */
public class JosmanProject {

    private static final Logger LOG = Logger.getLogger(JosmanProject.class.getName());
    private static final int DEPTH = 8000;
    
    /**
     * Folder in source code where user documentation is held.
     */
    public static final String DOCS_FOLDER = "docs";
    
    public static final String README_MD = "README.md";
    public static final String CHANGES_MD = "CHANGES.md";

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

    PegDownProcessor pegDownProcessor;

    private static void deleteOutputVersionDir(File outputVersionDir, int major, int minor) {

        String versionName = "" + major + "." + minor;

        if (outputVersionDir.exists()) {
            // let's be strict before doing moronic things
            checkArgument(major >= 0);
            checkArgument(minor >= 0);
            if (outputVersionDir.getAbsolutePath().endsWith(versionName)) {
                LOG.info("Found already existing output dir, cleaning it...");
                try {
                    FileUtils.deleteDirectory(outputVersionDir);
                    LOG.info("Done cleaning directory.");
                }
                catch (Exception ex) {
                    throw new RuntimeException("Error while deleting directory!", ex);
                }
            } else {
                throw new RuntimeException("output path " + outputVersionDir.getAbsolutePath() + " doesn't end with '" + versionName + "', avoiding cleaning it for safety reasons!");
            }
        }

    }

    /**
     *
     * @param snapshotMode if true the website generator will only process the
     * current branch snapshot. Otherwise all released versions except
     * {@code ignoredVersions} will be processed,
     *
     */
    public JosmanProject(
            MavenProject mvnPrj,
            String sourceRepoDirPath,
            String pagesDirPath,
            List<SemVersion> ignoredVersions,
            boolean snapshotMode) {
               
        checkNotNull(mvnPrj, "Invalid Maven project!");
        checkNotNull(sourceRepoDirPath, "Invalid repository source docs dir path!");
        checkNotNull(pagesDirPath, "Invalid pages dir path!");
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
        checkArgument(!sourceRepoDir.getAbsolutePath().equals(pagesDir.getAbsolutePath()),
                "Source folder and target folder coincide! They are " + sourceRepoDir.getAbsolutePath());
        this.pegDownProcessor = new PegDownProcessor(
                Parser.QUOTES
                | Parser.HARDWRAPS
                | Parser.AUTOLINKS
                | Parser.TABLES
                | Parser.FENCED_CODE_BLOCKS
                | Parser.WIKILINKS
                | Parser.STRIKETHROUGH // not supported in netbeans flow 2.0 yet
                | Parser.ANCHORLINKS // not supported in netbeans flow 2.0 yet		
        );
    }

    private File sourceDocsDir() {
        return new File(sourceRepoDir, DOCS_FOLDER);
    }

    private File targetJavadocDir(SemVersion version) {
        return new File(targetVersionDir(version), "javadoc");
    }

    private File sourceJavadocDir(SemVersion version) {
        if (snapshotMode) {
            return new File(sourceRepoDir, "target" + File.separator + "apidocs");
        } else {
            throw new UnsupportedOperationException("todo non-local javadoc not supported yet");
        }
    }

    static String programLogoName(String repoName) {
        return repoName + "-logo-200px.png";
    }

    static File programLogo(File sourceDocsDir, String repoName) {
        return new File(sourceDocsDir, "img" + File.separator + programLogoName(repoName));
    }

    /**
     * Copies provided stream to destination, which is determined according to
     * {@code relPath}, {@code preprendedPath} and {@code version}
     *
     * @param sourceStream
     * @param relPath path relative to root, i.e. docs/README.md or
     * img/mypic.jpg
     *
     * @param version
     * @return the target file
     */
    File copyStream(
            InputStream sourceStream,
            String relPath,
            final SemVersion version,
            List<String> relPaths) {

        checkNotNull(sourceStream, "Invalid source stream!");
        checkNotEmpty(relPath, "Invalid relative path!");
        checkNotEmpty(relPaths, "Invalid relative paths!");
        checkNotNull(version);

        File targetFile = Josmans.targetFile(pagesDir, relPath, version);

        if (targetFile.exists()) {
            throw new RuntimeException("Target file already exists! "
                    + targetFile.getAbsolutePath());
        }

        if (relPath.endsWith(".md")) {

            LOG.log(Level.INFO, "Creating file {0}", targetFile.getAbsolutePath());
            if (targetFile.exists()) {
                throw new RuntimeException("Target file already exists! Target is " + targetFile.getAbsolutePath());
            }
            copyMdAsHtml(sourceStream, relPath, version, relPaths);
        } else {

            LOG.log(Level.INFO, "Copying file into {0}", targetFile.getAbsolutePath());

            try {
                FileUtils.copyInputStreamToFile(sourceStream, targetFile);
                sourceStream.close();
                LOG.info("Done copying file.");
            }
            catch (Exception ex) {
                throw new RuntimeException("Error while copying stream to file!", ex);
            }
        }
        return targetFile;
    }

    /**
     * Writes an md stream as html to outputFile
     *
     * @param outputFile Must not exist. Eventual needed directories in the path
     * will be created
     * @param relPath path relative to {@link #sourceRepoDir}, i.e.
     * img/mypic.jpg or docs/README.md
     * @param version The version the md page refers to.
     * @param relpaths a list of relative paths for the sidebar
     */
    void copyMdAsHtml(
            InputStream sourceMdStream,
            String relPath,
            final SemVersion version,
            List<String> relpaths) {

        checkNotNull(version);
        checkNotEmpty(relPath, "Invalid relative path!");
        checkNotEmpty(relpaths, "Invalid relative paths!");

        final String prependedPath = Josmans.prependedPath(relPath);

        File targetFile = Josmans.targetFile(pagesDir, relPath, version);

        if (targetFile.exists()) {
            throw new RuntimeException("Trying to write md file to target that already exists!! Target is " + targetFile.getAbsolutePath());
        }

        String sourceMdString;
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(sourceMdStream, writer, "UTF-8"); // todo fixed encoding...
            sourceMdString = writer.toString();
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't read source md stream! Target path is " + targetFile.getAbsolutePath(), ex);
        }

        Josmans.checkNotMeaningful(sourceMdString, "Invalid source md file!");

        String filteredSourceMdString = sourceMdString
                .replaceAll("#\\{version}", version.toString())
                .replaceAll("#\\{majorMinorVersion}", Josmans.majorMinor(version))
                .replaceAll("#\\{repoRelease}", Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId(), version))
                .replaceAll("jedoc", "josman"); // for legacy compat 
        
        String skeletonString;
        try {
            StringWriter writer = new StringWriter();
            InputStream stream = Josmans.findResourceStream("/skeleton.html");
            IOUtils.copy(stream, writer, "UTF-8");
            skeletonString = writer.toString();
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't read skeleton file!", ex);
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
        skeleton.$("title").text(mvnPrj.getName());
        String contentFromMdHtml = pegDownProcessor.markdownToHtml(filteredSourceMdString);
        Jerry contentFromMd = Jerry.jerry(contentFromMdHtml);

        contentFromMd.$("a")
                .each(new JerryFunction() {

                    @Override
                    public boolean onNode(Jerry arg0, int arg1) {
                        String href = arg0.attr("href");
                        if (href.startsWith(prependedPath + "src")) {
                            arg0.attr("href", href.replace(prependedPath + "src", Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId(), version) + "/src"));
                            return true;
                        }
                        if (href.endsWith(".md")) {
                            arg0.attr("href", Josmans.htmlizePath(href));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../wiki")) {
                            arg0.attr("href", href.replace(prependedPath + "../../wiki", Josmans.repoWiki(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../issues")) {
                            arg0.attr("href", href.replace(prependedPath + "../../issues", Josmans.repoIssues(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                            return true;
                        }

                        if (href.equals(prependedPath + "../../milestones")) {
                            arg0.attr("href", href.replace(prependedPath + "../../milestones", Josmans.repoMilestones(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId())));
                            return true;
                        }

                        if (TodUtils.removeTrailingSlash(href).equals(DOCS_FOLDER)) {
                            arg0.attr("href", Josmans.majorMinor(version) + "/index.html");
                            return true;
                        }

                        if (href.startsWith(DOCS_FOLDER + "/")) {
                            arg0.attr("href", Josmans.majorMinor(version) + href.substring(DOCS_FOLDER.length()));
                            return true;
                        }

                        return true;
                    }
                }
                );

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

        skeleton.$("#josman-internal-content").html(contentFromMd.html());

        skeleton.$("#josman-repo-link").html(mvnPrj.getName()).attr("href", prependedPath + "index.html");

        File programLogo = programLogo(sourceDocsDir(), mvnPrj.getArtifactId());

        if (programLogo.exists()) {
            skeleton.$("#josman-program-logo").attr("src", prependedPath + "img/" + mvnPrj.getArtifactId() + "-logo-200px.png");
            skeleton.$("#josman-program-logo-link").attr("href", prependedPath + "index.html");
        } else {
            skeleton.$("#josman-program-logo-link").css("display", "none");
        }

        skeleton.$("#josman-wiki").attr("href", Josmans.repoWiki(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId()));
        skeleton.$("#josman-project").attr("href", Josmans.repoUrl(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId()));

        skeleton.$("#josman-home").attr("href", prependedPath + "index.html");
        if (Josmans.isRootpath(relPath)) {
            skeleton.$("#josman-home").addClass("josman-tag-selected");
        }

        // cleaning example versions
        skeleton.$(".josman-version-tab-header").remove();

        List<RepositoryTag> tags = new ArrayList(Josmans.versionTagsToProcess(mvnPrj.getArtifactId(), repoTags, ignoredVersions).values());
        Collections.reverse(tags);

        if (Josmans.isRootpath(relPath)) {
            skeleton.$("#josman-internal-sidebar").text("");
            skeleton.$("#josman-sidebar-managed-block").css("display", "none");
        } else {
            Jerry sidebar = makeSidebar(contentFromMdHtml, relPath, relpaths);
            skeleton.$("#josman-internal-sidebar").html(sidebar.htmlAll(true));
        }

        skeleton.$(".josman-to-strip").remove();

        if (snapshotMode) {

            if (tags.size() > 0) {
                SemVersion ver = Josmans.version(mvnPrj.getArtifactId(), tags.get(0).getName());
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
                //throw new RuntimeException("Found '" + matcher.group() + "' string in stream for " + targetFile.getAbsolutePath() + " (at position " + matcher.start() + ")");
                LOG.warning("Found '" + matcher.group() + "' string in stream for " + targetFile.getAbsolutePath());
            }
        }

        if (!targetFile.getParentFile().exists()) {
            if (!targetFile.getParentFile().mkdirs()) {
                throw new RuntimeException("Couldn't create target directories to host processed md file " + targetFile.getAbsolutePath());
            }
        }

        try {

            FileUtils.write(targetFile, skeleton.html());
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't write into " + targetFile.getAbsolutePath() + "!", ex);
        }

    }

    private static void addVersionHeaderTag(Jerry skeleton, String prependedPath, SemVersion version, boolean selected) {
        String verShortName = Josmans.majorMinor(version);
        String classSelected = selected ? "josman-tag-selected" : "";
        skeleton.$("#josman-usage").append(
                "<a class='josman-version-tab-header " + classSelected + "' href='"
                + prependedPath
                + verShortName
                + "/index.html'>" + verShortName + "</a>");
    }

    private void buildIndex(SemVersion latestVersion) {
        try {
            File sourceMdFile = new File(sourceRepoDir, README_MD);
            copyMdAsHtml(new FileInputStream(sourceMdFile), README_MD, latestVersion, ImmutableList.of(README_MD));
        }
        catch (FileNotFoundException ex) {
            throw new RuntimeException("Error while building index!", ex);
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
    private void processDocsDir(SemVersion version) {
        checkNotNull(version);

        if (!sourceDocsDir().exists()) {
            throw new RuntimeException("Can't find source dir!" + sourceDocsDir().getAbsolutePath());
        }

        File targetVersionDir = targetVersionDir(version);

        deleteOutputVersionDir(targetVersionDir, version.getMajor(), version.getMinor());

        List<String> relPaths = new ArrayList<String>();
        File[] files = sourceDocsDir().listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null. 

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".md")) {
                relPaths.add(DOCS_FOLDER + "/" + file.getName());
            }
        }

        DirWalker dirWalker = new DirWalker(
                sourceDocsDir(),
                targetVersionDir,
                this,
                version,
                relPaths
        );
        dirWalker.process();
        copyJavadoc(version);
    }

    /**
     * @param path the exact path. Careful: must be *exact* dir name (i.e.
     * 'docs' will work for docs/a.txt but 'doc' won't work)
     * @throws RuntimeException on error
     */
    private TreeWalk makeGitDocsWalk(RevTree tree, String path) {
        checkNotNull(tree);
        try {
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            treeWalk.setFilter(PathFilter.create(path));
            return treeWalk;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Processes a directory 'docs' at tag repoName-version that holds
     * documentation for a given version of the software
     */
    private void processGitDocsDir(SemVersion version) {
        checkNotNull(version);

        checkNotNull(repo);

        String releaseTag = Josmans.releaseTag(mvnPrj.getArtifactId(), version);

        try {
            ObjectId lastCommitId = repo.resolve(releaseTag);

            // a RevWalk allows to walk over commits based on some filtering that is defined
            DepthWalk.RevWalk revWalk = new DepthWalk.RevWalk(repo, DEPTH);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();

            TreeWalk relPathsWalk = makeGitDocsWalk(tree, DOCS_FOLDER);

            List<String> relpaths = new ArrayList();
            while (relPathsWalk.next()) {
                String pathString = relPathsWalk.getPathString();

                if (pathString.endsWith(".md")) {
                    FileMode fileMode = relPathsWalk.getFileMode(0);
                    LOG.log(Level.FINE, "Collecting {0}:  mode: {1}, type: {2}", new Object[]{pathString, Josmans.gitFileModeToString(fileMode), fileMode.getObjectType()});
                    relpaths.add(pathString);
                }
            }

            String path;
            List<String> fixedRelpaths;

            if (relpaths.isEmpty()) {
                LOG.log(Level.WARNING, "COULDN''T FIND ANY FILE IN " + DOCS_FOLDER + " for version {0}! TRYING TO USE README.md instead", version);
                path = README_MD;
            } else {
                path = DOCS_FOLDER;
            }

            TreeWalk treeWalk = makeGitDocsWalk(tree, path);
            while (treeWalk.next()) {

                String pathString = treeWalk.getPathString();

                FileMode fileMode = treeWalk.getFileMode(0);
                LOG.log(Level.FINE, "{0}:  mode: {1}, type: {2}", new Object[]{pathString, Josmans.gitFileModeToString(fileMode), fileMode.getObjectType()});

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(objectId);

                InputStream stream = loader.openStream();

                if (relpaths.isEmpty()) {
                    copyStream(stream, DOCS_FOLDER + "/" + pathString, version, ImmutableList.of(DOCS_FOLDER + "/" + path));
                } else {
                    copyStream(stream, pathString, version, relpaths);
                }

            }

            copyJavadoc(version);
        }

        catch (Exception ex) {
            throw new RuntimeException("Error while extracting docs from git local repo at commit " + releaseTag, ex);
        }

        // ------------------------------------------------------
        // File sourceMdFile = new File(wikiDir, "userdoc\\" + ver.getMajor() + ver.getMinor() + "\\Usage.md");
        // File outputFile = new File(pagesDir, version + "\\usage.html");
        // buildMd(sourceMdFile, outputFile, "../");            
        /*
         if (!sourceDocsDir().exists()) {
         throw new RuntimeException("Can't find source dir!" + sourceDocsDir().getAbsolutePath());
         }

        
         File targetVersionDir = targetVersionDir(version);

         deleteOutputVersionDir(targetVersionDir, version.getMajor(), version.getMinor());

         DirWalker dirWalker = new DirWalker(
         sourceDocsDir(),
         targetVersionDir,
         this,
         version
         );
         dirWalker.process();

         copyJavadoc(version);
         */
    }

    /**
     * Copies target version to 'latest' directory, cleaning it before the copy
     *
     * @param version
     */
    private void createLatestDocsDirectory(SemVersion version) {

        File targetLatestDocsDir = targetLatestDocsDir();
        LOG.log(Level.INFO, "Creating latest docs directory {0}", targetLatestDocsDir.getAbsolutePath());

        if (!targetLatestDocsDir.getAbsolutePath().endsWith("latest")) {
            throw new RuntimeException("Trying to delete a latest docs dir which doesn't end with 'latest'!");
        }
        try {
            LOG.log(Level.INFO, "Deleting directory {0}  ...", targetLatestDocsDir.getAbsolutePath());
            FileUtils.deleteDirectory(targetLatestDocsDir);
            LOG.log(Level.INFO, "Done deleting directory.");
            LOG.log(Level.INFO, "Copying files from directory {0} to {1}  ...", 
                    new Object[]{targetVersionDir(version).getAbsolutePath(),
                    targetLatestDocsDir.getAbsolutePath()});
            FileUtils.copyDirectory(targetVersionDir(version), targetLatestDocsDir);
            LOG.log(Level.INFO, "Done copying directory.");
        }
        catch (Throwable tr) {
            throw new RuntimeException("Error while creating latest docs directory ", tr);
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

    public void generateSite() {

        LOG.log(Level.INFO, "Fetching {0}/{1} tags.", new Object[]{Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId()});
        repoTags = Josmans.fetchTags(Josmans.organization(mvnPrj.getUrl()), mvnPrj.getArtifactId());
        MavenXpp3Reader reader = new MavenXpp3Reader();             
        
        try {
            File repoFile = new File(sourceRepoDir, ".git");

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repo = builder.setGitDir(repoFile)
                    .readEnvironment() // scan environment GIT_* variables
                    .build();
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while reading local git repo!", ex);
        }

        LOG.log(Level.INFO, "Cleaning target: {0}  ....", pagesDir.getAbsolutePath());
        if (!pagesDir.getAbsolutePath().endsWith("site")) {
            throw new RuntimeException("target directory does not end with 'site' !");
        }
        try {
            FileUtils.deleteDirectory(pagesDir);
            LOG.info("Done deleting directory");
        }
        catch (IOException ex) {
            throw new RuntimeException("Error while deleting directory " + pagesDir.getAbsolutePath(), ex);
        }
        
        SemVersion snapshotVersion = SemVersion.of(mvnPrj.getVersion()).withPreReleaseVersion("");

        if (snapshotMode) {
            LOG.log(Level.INFO, "Processing local version");

            buildIndex(snapshotVersion);
            processDocsDir(snapshotVersion);
            createLatestDocsDirectory(snapshotVersion);

        } else {
            if (repoTags.isEmpty()) {
                throw new NotFoundException("There are no tags at all in the repository!!");
            }
            SemVersion latestPublishedVersion = Josmans.latestVersion(mvnPrj.getArtifactId(), repoTags);
            LOG.log(Level.INFO, "Processing published version");
            buildIndex(latestPublishedVersion);
            String curBranch = Josmans.readRepoCurrentBranch(sourceRepoDir);


            SortedMap<String, RepositoryTag> filteredTags = Josmans.versionTagsToProcess(mvnPrj.getArtifactId(), repoTags, ignoredVersions);

            for (RepositoryTag tag : filteredTags.values()) {
                LOG.log(Level.INFO, "Processing release tag {0}", tag.getName());
                processGitDocsDir(Josmans.version(mvnPrj.getArtifactId(), tag.getName()));
            }

        }

        Josmans.copyDirFromResource(Josmans.class, "/website-template", pagesDir);

        try {

            File targetImgDir = new File(pagesDir, "img");

            File programLogo = programLogo(sourceDocsDir(), mvnPrj.getArtifactId());

            if (programLogo.exists()) {
                LOG.log(Level.INFO, "Found program logo: {0}", programLogo.getAbsolutePath());
                LOG.log(Level.INFO, "      copying it into dir {0}", targetImgDir.getAbsolutePath());

                FileUtils.copyFile(programLogo, new File(targetImgDir, programLogoName(mvnPrj.getArtifactId())));
            }

            FileUtils.copyFile(new File(sourceRepoDir, "LICENSE.txt"), new File(pagesDir, "LICENSE.txt"));

        }
        catch (Exception ex) {
            throw new RuntimeException("Error while copying files!", ex);
        }

        LOG.log(Level.INFO, "\n\nSite is now browsable at {0}\n\n", pagesDir.getAbsolutePath());
    }

    /**
     * Returns a Jerry object repereenting the non-managed sidebar for a given
     * version page
     *
     * @param contentFromMdHtml the content of page we're making the sidebar for
     * @param relpaths a list of relpaths of pages related to the version we're
     * making the sidebar for
     * @param currentRelPath the relpath of the page we're making the sidebar
     * for
     */
    private Jerry makeSidebar(String contentFromMdHtml, String currentRelPath, List<String> relpaths) {
        checkNotNull(contentFromMdHtml);
        checkNotEmpty(currentRelPath, "Invalid current rel path!");
        checkNotEmpty(relpaths, "Invalid list of relpaths!");

        Jerry html = Jerry.jerry(contentFromMdHtml);

        Jerry allLinksContainer = Jerry.jerry("<ul>").$("ul")
                .addClass("josman-tree");

        List<String> orderedRelpaths = Josmans.orderRelpaths(relpaths);

        for (String relpath : orderedRelpaths) {
            Jerry pageItemContainer = Jerry.jerry("<li>").$("li");

            Jerry pageTitle
                    = Jerry.jerry("<div>")
                    .$("div")
                    .addClass("josman-sidebar-page-title");

            Jerry pageLinksContainer = Jerry.jerry("<ul>").$("ul")
                    .addClass("josman-tree");

            if (relpath.equals(currentRelPath)) {
                pageTitle.text(Josmans.targetName(relpath));
                pageTitle.addClass("josman-sidebar-selected");

                for (Jerry sourceHeaderLink : html.$("h3 a")) {
                    // <a href="#header1">Header 1</a><br/>

                    /*<ul class="josman-tree">
                     <li>
                     <div class="josman-sidebar-page-title">Usage</div>
                     <ul class="josman-tree">
                     <li><a href="#header1">Maven</a>   */
                    Jerry linkContainer = Jerry.jerry("<div>").$("div");

                    Jerry link = Jerry.jerry("<a>").$("a")
                            .attr("href", sourceHeaderLink.first().first().attr("href"))
                            .text(sourceHeaderLink.first().text());
                    linkContainer.append(link.htmlAll(true));

                    pageLinksContainer.append(linkContainer.htmlAll(true));

                    // ret += "<div> <a href='" + sourceHeaderLink.first().first().attr("href") + "'>" + sourceHeaderLink.first().text() + "</a></div> \n";
                    /*Jerry.jerry("<a>")
                     .attr("href","#" + sourceHeaderLink.attr("id"))
                     .text(sourceHeaderLink.text()); */
                }
            } else {
                pageTitle.append(Jerry.jerry("<a>").$("a")
                        .attr("href", Josmans.htmlizePath(relpath.substring(DOCS_FOLDER.length() + 1)))
                        .text(Josmans.targetName(relpath)).htmlAll(true));
            }

            pageItemContainer.append(pageTitle.htmlAll(true));
            pageItemContainer.append(pageLinksContainer.htmlAll(true));

            allLinksContainer.append(pageItemContainer.htmlAll(true));
        }

        return allLinksContainer;
    }

    /**
     * Copies javadoc into target website according to the artifact version.
     */
    private void copyJavadoc(SemVersion version) {
        File targetJavadoc = targetJavadocDir(version);
        if (targetJavadoc.exists() && (targetJavadoc.isFile() || targetJavadoc.length() > 0)) {
            throw new RuntimeException("Target directory for Javadoc already exists!!! " + targetJavadoc.getAbsolutePath());
        }
        if (snapshotMode) {
            File sourceJavadoc = sourceJavadocDir(version);
            if (sourceJavadoc.exists()) {

                try {
                    LOG.log(Level.INFO, "Now copying Javadoc from {0} to {1} ...", new Object[]{sourceJavadoc.getAbsolutePath(), targetJavadoc.getAbsolutePath()});
                    FileUtils.copyDirectory(sourceJavadoc, targetJavadoc);
                    LOG.info("Done copying javadoc.");
                }
                catch (Exception ex) {
                    throw new RuntimeException("Error while copying Javadoc from " + sourceJavadoc.getAbsolutePath() + " to " + targetJavadoc.getAbsolutePath(), ex);
                }
            } else {
                LOG.log(Level.INFO, "Couldn''t find javadoc, skipping it. Looked in {0}", sourceJavadoc.getAbsolutePath());
            }
        } else {
            File jardocs;
            try {
                jardocs = Josmans.fetchJavadoc(mvnPrj.getGroupId(), mvnPrj.getArtifactId(), version);
            }
            catch (Exception ex) {
                String sep = File.separator;
                String localJarPath = sourceRepoDir.getAbsolutePath() + sep + "target" + sep + "checkout" + sep + "target" + sep + Josmans.javadocJarName(mvnPrj.getArtifactId(), version);
                LOG.log(Level.WARNING, "Error while fetching javadoc from Maven Central, trying to locate it at " + localJarPath, ex);
                jardocs = new File(localJarPath);
                if (!jardocs.exists()) {
                    throw new RuntimeException("Couldn't find any jar for javadoc!");
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

    
}
