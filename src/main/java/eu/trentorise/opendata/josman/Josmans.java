package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.commons.TodUtils;
import eu.trentorise.opendata.josman.exceptions.JosmanException;
import eu.trentorise.opendata.josman.exceptions.JosmanIoException;
import eu.trentorise.opendata.josman.exceptions.JosmanNotFoundException;

import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import static eu.trentorise.opendata.josman.JosmanProject.CHANGES_MD;
import static eu.trentorise.opendata.josman.JosmanProject.DOCS_FOLDER;
import static eu.trentorise.opendata.josman.JosmanProject.README_MD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.parboiled.common.ImmutableList;

import com.google.common.io.Files;

/**
 * Utilities for Josman
 *
 * @author David Leoni
 */
public final class Josmans {

    private static final Logger LOG = Logger.getLogger(Josmans.class.getName());

    public static final int CONNECTION_TIMEOUT = 1000;

    public static final String[] REQUIRED_DOCS = new String[] { "LICENSE.txt", "README.md", "docs/README.md" };

    private Josmans() {
    }

    /**
     * Returns a string describing the required docs in a human-comprehensible
     * way.
     * 
     * @param prefixFilter
     *            if empty no filter is used.
     * @since 0.8.0
     */
    public static String requiredDocs(String prefixFilter) {

        checkNotNull(prefixFilter, "Invalid prefix filter! If you don't want to use it use empty string instead!");

        StringBuilder sb = new StringBuilder();
        for (String s : REQUIRED_DOCS) {
            if (TodUtils.isNotEmpty(prefixFilter)) {
                if (s.startsWith(prefixFilter)) {
                    sb.append(s + "\n");
                }
            } else {
                sb.append(s + "\n");
            }
        }
        return sb.toString();
    }

    /**
     * Reading file with Jgit:
     * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/
     * dstadler/jgit/api/ReadFileFromCommit.java
     */
    /**
     * Fetches all tags from a github repository. Beware of API limits of 60
     * requests per hour
     * 
     * @throws JosmanIoException
     */
    public static ImmutableList<RepositoryTag> fetchTags(String organization, String repoName) {
        TodUtils.checkNotEmpty(organization, "Invalid organization!");
        TodUtils.checkNotEmpty(repoName, "Invalid repo name!");

        LOG.log(Level.FINE, "Fetching {0}/{1} tags.", new Object[] { organization, repoName });

        try {
            GitHubClient client = new GitHubClient();
            RepositoryService service = new RepositoryService(client);
            Repository repo = service.getRepository(organization, repoName);
            List<RepositoryTag> tags = service.getTags(repo);
            return ImmutableList.copyOf(tags);
        } catch (Exception ex) {
            throw new JosmanIoException(ex);
        }

    }

    /**
     * Reads a local project current branch.
     *
     * @param projectPath
     *            path to project root folder (the one that _contains_
     *            the .git folder)
     * @return the current branch of provided repo
     * 
     * @throws JosmanIoException
     */
    public static String readRepoCurrentBranch(File projectPath) {
        checkNotNull(projectPath);
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repo = builder.setGitDir(new File(projectPath, ".git"))
                                                          .readEnvironment() // scan
                                                                             // environment
                                                                             // GIT_*
                                                                             // variables
                                                          .findGitDir() // scan
                                                                        // up
                                                                        // the
                                                                        // file
                                                                        // system
                                                                        // tree
                                                          .build();
            return repo.getBranch();
        } catch (IOException ex) {
            throw new JosmanIoException("Couldn't read current branch from " + projectPath.getAbsolutePath());
        }
    }

    /**
     * Returns major.minor as string
     */
    public static String majorMinor(SemVersion version) {
        return version.getMajor() + "." + version.getMinor();
    }

    /**
     * Constructs a SemVersion out of a release tag, like i.e. josman-1.2.3
     */
    public static SemVersion version(String repoName, String releaseTag) {
        String versionString = releaseTag.replace(repoName + "-", "");
        return SemVersion.of(versionString);
    }

    /**
     * Returns a tag with given major and minor in provided list of git
     * repository tags
     * 
     * @param repoName
     *            i.e. "josman"
     * @throws JosmanNotFoundException
     *             if tag is not found
     */
    static RepositoryTag find(String repoName, int major, int minor, Iterable<RepositoryTag> tags) {
        for (RepositoryTag tag : tags) {
            SemVersion tagVersion = version(repoName, tag.getName());
            if (tagVersion.getMajor() == major
                    && tagVersion.getMinor() == minor) {
                return tag;
            }
        }
        throw new JosmanNotFoundException("Couldn't find any tag matching " + major + "." + minor + " pattern");
    }

    /**
     * Returns a semantic version from branch name in the format "branch-x.y".
     * Result patch number will be 0.
     *
     * @param branchName
     *            Must be in format "branch-x.y"
     * @throws IllegalArguemntException
     *             if branchname is not in the expected
     *             format.
     */
    public static SemVersion versionFromBranchName(String branchName) {
        checkNotNull(branchName);

        if (!branchName.startsWith("branch-")) {
            throw new IllegalArgumentException("Tried to extract version from branch name '" + branchName
                    + "', but it does not start with 'branch-'  !!!");
        }

        try {
            SemVersion ret = SemVersion.of(branchName.replace("branch-", "")
                                                     .concat(".0"));
            return ret;
        } catch (Throwable tr) {
            throw new IllegalArgumentException("Error while extracting version from branch name " + branchName, tr);
        }

    }

    /**
     * Adds the candidate tag to the provided tags if no other tag has same
     * major and minor or if its patch is greater.
     */
    @Nullable
    private static void updateTags(String repoName, RepositoryTag candidateTag, List<RepositoryTag> tags) {

        SemVersion candidateSemVarsion = version(repoName, candidateTag.getName());

        for (int i = 0; i < tags.size(); i++) {
            RepositoryTag tag = tags.get(i);
            SemVersion semVersion = version(repoName, tag.getName());
            if (candidateSemVarsion.getMajor() == semVersion.getMajor()
                    && candidateSemVarsion.getMinor() == semVersion.getMinor()) {
                if (candidateSemVarsion.getPatch() > semVersion.getPatch()) {
                    tags.set(i, candidateTag);
                }
                return;
            }
        }
        tags.add(candidateTag);
    }

    /**
     * Returns a new list with given relpaths ordered by importance, the first
     * being the most important.
     */
    static List<String> orderRelpaths(List<String> relpaths) {
        List<String> ret = new ArrayList(relpaths);

        String readme = DOCS_FOLDER + "/" + README_MD;
        String changes = DOCS_FOLDER + "/" + CHANGES_MD;

        List<String> toSkip = ImmutableList.of(readme, changes);

        ret.removeAll(toSkip);

        if (relpaths.contains(readme)) {
            ret.add(0, readme);
        }

        if (relpaths.contains(changes)) {
            ret.add(ret.size(), changes);
        }
        return ImmutableList.copyOf(ret);
    }

    /**
     * Returns new sorted map of only version tags of the format repoName-x.y.z
     * filtered tags, the latter having the highest version.
     *
     * @param repoName
     *            the github repository name i.e. josman
     * @param tags
     *            a list of tags from the repository
     * @return map of version as string and correspondig RepositoryTag
     */
    public static SortedMap<String, RepositoryTag> versionTags(String repoName,
            @Nullable List<RepositoryTag> tags) {

        if (tags == null) {
            return new TreeMap<>();
        }
        List<RepositoryTag> ret = new ArrayList();
        for (RepositoryTag candidateTag : tags) {
            if (candidateTag.getName()
                            .startsWith(repoName + "-")) {
                updateTags(repoName, candidateTag, ret);
            }
        }

        TreeMap<String, RepositoryTag> map = new TreeMap<>();
        for (RepositoryTag tag : ret) {
            map.put(tag.getName(), tag);
        }
        return map;
    }

    /**
     * Returns new sorted map of only version tags to be processed of the format
     * repoName-x.y.z filtered tags, the latter having the highest version.
     *
     * @param repoName
     *            the github repository name i.e. josman
     * @param tags
     *            a list of tags from the repository
     * @param ignoredVersions
     *            These versions will be filtered in the output.
     * @return map of version as string and corresponding RepositoryTag
     */
    public static SortedMap<String, RepositoryTag> versionTagsToProcess(
            String repoName,
            @Nullable List<RepositoryTag> tags,
            List<SemVersion> ignoredVersions) {
        if (tags == null) {
            return new TreeMap<>();
        }
        SortedMap<String, RepositoryTag> map = versionTags(repoName, tags);
        for (SemVersion versionToSkip : ignoredVersions) {
            String tag = releaseTag(repoName, versionToSkip);
            if (map.containsKey(tag)) {
                map.remove(tag);
            }
        }
        return map;
    }

    /**
     * Returns the release tag formed by inserting a minus between the repoName
     * and the version
     *
     * @param repoName
     *            i.e. josman
     * @param version
     *            i.e. 1.2.3
     * @return i.e. josman-1.2.3
     */
    public static String releaseTag(String repoName, SemVersion version) {
        return repoName + "-" + version;
    }

    /**
     * Returns the github repo url, i.e.
     * https://github.com/opendatatrentino/josman
     *
     * @param organization
     *            i.e. opendatatrentino
     * @param name
     *            i.e. josman
     */
    public static String repoUrl(String organization, String name) {
        return "https://github.com/" + organization + "/" + name;
    }

    /**
     * Returns the github release code url, i.e.
     * https://github.com/opendatatrentino/josman/blob/todo-releaseTag
     *
     * @param repoName
     *            i.e. josman
     * @param version
     *            i.e. 1.2.3
     */
    public static String repoRelease(String organization, String repoName, SemVersion version) {
        return repoUrl(organization, repoName) + "/blob/" + releaseTag(repoName, version);
    }

    /**
     * Returns the github wiki url, i.e.
     * https://github.com/opendatatrentino/josman/wiki
     *
     * @param organization
     *            i.e. opendatatrentino
     * @param repoName
     *            i.e. josman
     */
    public static String repoWiki(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/wiki";
    }

    /**
     * Returns the github issues url, i.e.
     * https://github.com/opendatatrentino/josman/issues
     *
     * @param organization
     *            i.e. opendatatrentino
     * @param repoName
     *            i.e. josman
     */
    public static String repoIssues(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/issues";
    }

    /**
     * Returns the github milestones url, i.e.
     * https://github.com/opendatatrentino/josman/milestones
     *
     * @param organization
     *            i.e. opendatatrentino
     * @param repoName
     *            i.e. josman
     */
    public static String repoMilestones(String organization, String repoName) {
        return repoUrl(organization, repoName) + "/milestones";
    }

    /**
     * Returns the github wiki url, i.e.
     *
     * @param organization
     *            i.e. opendatatrentino
     * @param repoName
     *            i.e. josman
     */
    public static String repoWebsite(String organization, String repoName) {
        return "https://" + organization + ".github.io/" + repoName;
    }

    public static SemVersion latestVersion(String repoName, List<RepositoryTag> tags) {
        TodUtils.checkNotEmpty(tags, "Invalid repository tags!");
        SortedMap<String, RepositoryTag> filteredTags = Josmans.versionTags(repoName, tags);
        if (filteredTags.isEmpty()) {
            throw new JosmanNotFoundException("Couldn't find any released version!");
        }
        return Josmans.version(repoName, filteredTags.lastKey());
    }

    /**
     * Returns a new url friendly and normalized path.
     *
     * @param path
     *            a path that may contain .md files and can have end like urls
     *            like my/path/to/file.md?query#fragment
     */
    public static String htmlizePath(String path) {
        checkNotEmpty(path, "Invalid path!");
        String slashPath = path.replace("\\", "/");

        URI uri;
        try {
            uri = new URI(slashPath);
        } catch (URISyntaxException e) {
            throw new JosmanException("Invalid path!", e);
        }

        String newPath;
        String normalizedPath = TodUtils.removeTrailingSlash(uri.getPath());
        if (normalizedPath.endsWith(README_MD)) {
            newPath = uri.getPath()
                         .replace(README_MD, "index.html");
        } else if (normalizedPath.endsWith(".md")) {
            newPath = normalizedPath.substring(0, normalizedPath.length() - 3) + ".html";
        } else if (normalizedPath.isEmpty()) {
            newPath = "/";
        } else {
            newPath = normalizedPath;
        }

        StringBuilder ret = new StringBuilder();
        if (TodUtils.isNotEmpty(uri.getScheme())) {
            ret.append(uri.getScheme() + "://");
        }
        if (TodUtils.isNotEmpty(uri.getAuthority())) {
            ret.append(uri.getAuthority());
        }
        ret.append(newPath);

        if (TodUtils.isNotEmpty(uri.getQuery())) {
            ret.append("?" + uri.getQuery());
        }
        if (TodUtils.isNotEmpty(uri.getFragment())) {
            ret.append("#" + uri.getFragment());
        }

        return ret.toString();

    }

    /**
     * Checks if the input string contains only spaces, tabs, etc
     * 
     * @return non-null input string
     * @param string
     * @throws IllegalArgumentException
     *             on empty string
     */
    public static String checkNotMeaningful(@Nullable String string, @Nullable Object prependedErrorMessage) {
        TodUtils.checkNotEmpty(string, prependedErrorMessage);
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != '\n' && string.charAt(i) != '\t' && string.charAt(i) != ' ') {
                return string;
            }
        }
        throw new IllegalArgumentException(String.valueOf(prependedErrorMessage)
                + " -- Reason: String contains only empty spaces/tabs/carriage returns!");
    }

    /**
     * Returns the maven style javadoc file name (i.e.
     * my-prog-1.2.3-javadoc.jar)
     */
    public static String javadocJarName(String artifactId, SemVersion version) {
        checkNotEmpty(artifactId, "Invalid artifactId!");
        checkNotNull(version);
        return artifactId + "-" + version + "-javadoc.jar";
    }

    /**
     * Fetches Javadoc of released artifact and writes it into {@code destFile}
     * 
     * @throws JosmanIoException
     * @throws Josman
     */
    public static File fetchJavadoc(String groupId, String artifactId, SemVersion version) {
        checkNotEmpty(groupId, "Invalid groupId!");
        checkNotEmpty(artifactId, "Invalid artifactId!");
        checkNotNull(version);

        File destFile;

        try {
            destFile = File.createTempFile(groupId + "-" + artifactId + "-javadoc", ".jar");
            destFile.deleteOnExit();
        } catch (IOException ex) {
            throw new JosmanIoException("Couldn't create target javadoc file!", ex);
        }

        URL url;
        try {
            url = new URL("http://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/"
                    + version + "/" + javadocJarName(artifactId, version));
        } catch (MalformedURLException ex) {
            throw new JosmanException("Error while forming javadoc URL!", ex);
        }
        LOG.log(Level.INFO, "Fetching javadoc from {0} into {1} ...", new Object[] { url, destFile.getAbsolutePath() });
        try {
            FileUtils.copyURLToFile(url, destFile, CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
            LOG.log(Level.INFO, "Done copying javadoc.");
        } catch (IOException ex) {
            throw new JosmanIoException("Error while fetch-and-write javadoc for " + groupId + "/" + artifactId + "-"
                    + version + " into file " + destFile.getAbsoluteFile(), ex);
        }
        return destFile;
    }

    /**
     * Extracts the directory at resource path to target directory. First
     * directory is searched in local "src/main/resources" so the thing also
     * works when developing in the IDE. If not found then searches in jar file.
     * 
     * @throws JosmanIoException
     */
    public static void copyDirFromResource(Class clazz, String dirPath, File destDir) {
        String sep = File.separator;
        File sourceDir = new File("src" + sep + "main" + sep + "resources", dirPath);

        if (sourceDir.exists()) {
            LOG.log(Level.INFO, "Copying directory from {0} to {1}  ...",
                    new Object[] { sourceDir.getAbsolutePath(), destDir.getAbsolutePath() });
            try {
                FileUtils.copyDirectory(sourceDir, destDir);
                LOG.log(Level.INFO, "Done copying directory");
            } catch (IOException ex) {
                throw new JosmanIoException("Couldn't copy the directory!", ex);
            }
        } else {

            File jarFile = new File(clazz.getProtectionDomain()
                                         .getCodeSource()
                                         .getLocation()
                                         .getPath());
            if (jarFile.isDirectory() && jarFile.getAbsolutePath()
                                                .endsWith("target" + File.separator + "classes")) {
                LOG.info("Seems like you have Josman sources, will take resources from there");
                try {
                    FileUtils.copyDirectory(new File(jarFile.getAbsolutePath() + "/../../src/main/resources", dirPath),
                            destDir);
                    LOG.log(Level.INFO, "Done copying directory");
                } catch (IOException ex) {
                    throw new JosmanIoException("Couldn't copy the directory!", ex);
                }
            } else {
                LOG.log(Level.INFO, "Extracting jar {0} to {1}",
                        new Object[] { jarFile.getAbsolutePath(), destDir.getAbsolutePath() });
                copyDirFromJar(jarFile, destDir, dirPath);
                LOG.log(Level.INFO, "Done copying directory from JAR.");
            }

        }
    }

    /**
     *
     * Extracts the files starting with dirPath from {@code file} to
     * {@code destDir}
     *
     * @param dirPath
     *            the prefix used for filtering. If empty the whole jar
     *            content is extracted.
     * 
     * @throws JosmanIoException
     */
    public static void copyDirFromJar(File jarFile, File destDir, String dirPath) {
        checkNotNull(jarFile);
        checkNotNull(destDir);
        checkNotNull(dirPath);

        String normalizedDirPath;
        if (dirPath.startsWith("/")) {
            normalizedDirPath = dirPath.substring(1);
        } else {
            normalizedDirPath = dirPath;
        }

        try {
            JarFile jar = new JarFile(jarFile);
            java.util.Enumeration enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumEntries.nextElement();
                if (jarEntry.getName()
                            .startsWith(normalizedDirPath)) {
                    File f = new File(
                            destDir
                                    + File.separator
                                    + jarEntry
                                              .getName()
                                              .substring(normalizedDirPath.length()));

                    if (jarEntry.isDirectory()) { // if its a directory, create
                                                  // it
                        f.mkdirs();
                        continue;
                    } else {
                        f.getParentFile()
                         .mkdirs();
                    }

                    InputStream is = jar.getInputStream(jarEntry); // get the
                                                                   // input
                                                                   // stream
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtils.copy(is, fos);
                    fos.close();
                    is.close();
                }

            }
        } catch (Exception ex) {
            throw new JosmanIoException("Error while extracting jar file! Jar source: " + jarFile.getAbsolutePath()
                    + " destDir = " + destDir.getAbsolutePath(), ex);
        }
    }

    /**
     * Searches resource indicated by path first in src/main/resources (so it
     * works even when developing), then in proper classpath resources. If
     * resource is found it is returned as input stream, otherwise an exception
     * is thrown.
     *
     * @throws JosmanNotFoundException
     *             if path can't be found.
     */
    public static InputStream findResourceStream(String path) {

        checkNotNull(path, "invalid path!");

        String localPath = "src/main/resources" + path;

        try {
            return new FileInputStream(localPath);
        } catch (FileNotFoundException ex) {
        }

        LOG.log(Level.INFO, "Can''t find file {0}", new File(localPath).getAbsolutePath());

        try {
            URL url = JosmanProject.class.getResource(path);
            LOG.log(Level.INFO, "Found file in {0}", url);
            InputStream ret = JosmanProject.class.getResourceAsStream(path);
            return ret;
        } catch (Exception ex) {
            throw new JosmanNotFoundException("Can't load file in resources! " + path);
        }

    }

    /**
     * Returns the name displayed on the website as menu item for a given page.
     *
     * @param relPath
     *            path relative to the {@link JosmanProject#sourceRepoDir()}
     *            (i.e.
     *            LICENSE.txt or docs/README.md)
     */
    public static String targetName(String relPath) {
        String htmlizedPath = htmlizePath(relPath);
        if (htmlizedPath.endsWith("/index.html")) {
            return "Usage";
        }
        if (htmlizedPath.endsWith("/CHANGES.html")) {
            return "Release notes";
        }
        String withoutFiletype = htmlizedPath.replace(".html", "");
        int lastSlash = withoutFiletype.lastIndexOf("/");
        String fileName = withoutFiletype;
        if (lastSlash != -1) {
            fileName = withoutFiletype.substring(lastSlash + 1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(fileName.charAt(0)));

        int i = 1;
        while (i < fileName.length()) {
            char ch = fileName.charAt(i);
            if (i + 1 < fileName.length()) {
                char nextCh = fileName.charAt(i + 1);
                if (Character.isLowerCase(ch)
                        && Character.isUpperCase(nextCh)) {
                    sb.append(ch + " ");
                    i += 1;
                    continue;
                } else {
                    if (i + 2 < fileName.length()) {
                        char nextNextCh = fileName.charAt(i + 2);
                        if (Character.isUpperCase(ch)
                                && Character.isUpperCase(nextCh)
                                && Character.isLowerCase(nextNextCh)) {
                            sb.append(ch);
                            sb.append(" ");
                            sb.append(Character.toLowerCase(nextCh));
                            i += 2;
                            continue;
                        } else {
                            if (Character.isUpperCase(ch)
                                    && Character.isUpperCase(nextCh)) {
                                sb.append(ch);
                                i += 1;
                                continue;
                            }
                        }
                    }
                }
            }

            sb.append(Character.toLowerCase(ch));
            i += 1;
        }
        return sb.toString();
    }

    /**
     * Returns the target file where a source path should be transfered into.
     *
     * @param relPath
     *            path relative to the {@link #sourceRepoDir} (i.e.
     *            LICENSE.txt or docs/README.md)
     */
    static File targetFile(File pagesDir, String relPath, final SemVersion version) {

        if (Josmans.isRootpath(relPath)) {
            return new File(
                    pagesDir,
                    Josmans.htmlizePath(relPath));
        } else {
            return new File(
                    pagesDir,
                    Josmans.majorMinor(version)
                            + File.separator
                            + Josmans.htmlizePath(relPath.substring((DOCS_FOLDER + "/").length())));
        }

    }

    /**
     * Returns either "../" or "" according to {@code relPath}
     *
     * @param relPath
     *            may start with "docs"
     */
    static String prependedPath(String relPath) {
        checkNotNull(relPath);
        // todo it handles only one level....
        if (isRootpath(relPath)) {
            return "";
        } else {
            return "../";

        }
    }

    /**
     * Returns true if provided {@code relPath} is a website root path
     *
     * @param relPath
     *            the website path, i.e. README.md or docs/CHANGES.md or
     *            docs\CHANGES.md
     */
    static boolean isRootpath(String relPath) {
        checkNotNull(relPath);
        return !(relPath.equals(DOCS_FOLDER)
                || relPath.startsWith(DOCS_FOLDER + "/")
                || relPath.startsWith(DOCS_FOLDER + "\\"));
    }

    /**
     * Returns a string representation of the provided git file mode
     */
    static String gitFileModeToString(FileMode fileMode) {
        if (fileMode.equals(FileMode.EXECUTABLE_FILE)) {
            return "Executable File";
        } else if (fileMode.equals(FileMode.REGULAR_FILE)) {
            return "Normal File";
        } else if (fileMode.equals(FileMode.TREE)) {
            return "Directory";
        } else if (fileMode.equals(FileMode.SYMLINK)) {
            return "Symlink";
        } else if (fileMode.equals(FileMode.GITLINK)) {
            return "submodule link";
        } else {
            return fileMode.toString();
        }

    }

    /**
     * Returns the organization given a project url, so from i.e.
     * <a href="https://github.com/opendatatrentino/tod-commons">https://github.
     * com/opendatatrentino/tod-commons
     * </a> will extract '{@code tod-commons}'
     * 
     * @since 0.8.0
     */
    public static String organization(String projectUrl) {

        checkNotEmpty(projectUrl, "project url is invalid!");

        String stripGithub = projectUrl.substring("https://github.com/".length());

        return stripGithub.substring(0, stripGithub.indexOf("/"));

    }

    /**
     * 
     * Processes input string by replacing commands with their execution, and 
     * then returns the resulting expanded string. 
     * 
     * <b>Supported strings</b>:
     * 
     * <ul>
     * <li>methods: $exec{my.package.MyClass.myMethod()}</li>
     * <li>fields: $exec{my.package.MyClass.myField}</li>
     * <li>Spaces inside the parenthesis: $exec{ my.package.MyClass.myField }
     * <li>Escape with '$_': $_exec{something} will produce $exec{something} without trying to execute anything</li>
     * </ul>
     * 
     * <b>Unsupported</b>:
     * <ul>
     * <li>method with parameters: $exec{my.package.MyClass.myMethod("bla bla")}
     * </li>
     * <li>method chains: $exec{my.package.MyClass.myMethod().anotherMethod()}
     * </li>
     * <li>classes: $exec{my.package.MyClass}</li>
     * <li>unqualified classes: $exec{MyClass.myMethod()}</li>
     * <li>new instances: $exec{new my.package.MyClass()}</li>
     * </ul>
     * 
     * 
     * @since 0.8.0
     */
    public static String execCmds(String input, ClassLoader classLoader) {
        checkNotNull(input);
        checkNotNull(classLoader);
        
        int i = 0;

        Pattern pattern = Pattern.compile("\\$exec\\{\\s*([\\w|\\.]*(\\((.*)\\))?)\\s*\\}");
        Matcher matcher = pattern.matcher(input);
        List<Integer> execStarts = new ArrayList<>();
        List<Integer> execEnds = new ArrayList<>();
        List<String> results = new ArrayList<>();

        boolean foundInvalid = false;

        while (matcher.find()) {

            execStarts.add(matcher.start(0));
            execEnds.add(matcher.end(0));

            String cmd = matcher.group(1);

            if (matcher.group(3) != null && TodUtils.isNotEmpty(matcher.group(3)
                                                                       .trim())) {
                foundInvalid = true;
                LOG.severe("Commands with parameters are currently not supported: " + matcher.group(2));
            }

            String[] names = cmd.split("\\.");

            String methodOrFieldName;
            if (matcher.group(3) == null) { // field
                methodOrFieldName = names[names.length - 1];
            } else { // method
                methodOrFieldName = names[names.length - 1].substring(0, names[names.length - 1].indexOf('('));
            }

            String className = names[0];
            for (int j = 1; j < names.length - 1; j++) {
                className += "." + names[j];
            }
            Class clazz;
            try {
                clazz = Class.forName(className, true, classLoader);
            } catch (Exception ex) {
                throw new JosmanException("Error while loading class for calling method " + cmd, ex);
            }
            String stringRes;
            try {

                java.lang.reflect.Method method;
                try {
                    method = clazz.getMethod(methodOrFieldName);
                    if (!Modifier.isStatic(method.getModifiers())){
                        throw new JosmanException("Non-static methods are not supported!");
                    }
                    stringRes = String.valueOf(method.invoke(null));
                } catch (NoSuchMethodException e) {
                    Field field = clazz.getField(methodOrFieldName);
                    stringRes = String.valueOf(field.get(null));
                }

            } catch (Exception ex) {
                throw new JosmanException("Error processing " + cmd, ex);
            }

            results.add(stringRes);
            

        }

        if (foundInvalid) {
            throw new JosmanException("Found invalid command(s)! See log for errors.");
        }

        StringBuilder sb = new StringBuilder();

        int lastIndex = 0;
        for (int k = 0; k < execStarts.size(); k++) {
            int start = execStarts.get(k);
            int end = execEnds.get(k);
            String res = results.get(k);

            sb.append(input.substring(lastIndex, start));
            sb.append(res);
            lastIndex = end;
        }
        if (lastIndex < input.length()) {
            sb.append(input.substring(lastIndex, input.length()));
        }
        
        
        return sb.toString().replace("$_exec{", "$exec{");
    }

}
