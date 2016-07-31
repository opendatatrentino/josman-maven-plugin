package eu.trentorise.opendata.josman.test;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import eu.trentorise.opendata.commons.TodConfig;
import static eu.trentorise.opendata.commons.TodUtils.checkNotEmpty;
import eu.trentorise.opendata.josman.Josmans;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class GitTest {

    private static final Logger LOG = Logger.getLogger(GitTest.class.getName());
    private static final int DEPTH = 8000;

    @BeforeClass
    public static void beforeClass() {
        TodConfig.init(GitTest.class);
    }

    @Test
    public void testEgit() throws IOException {
        List<RepositoryTag> tags = Josmans.fetchTags("opendatatrentino", "josman");
        SortedMap<String, RepositoryTag> filteredTags = Josmans.versionTags("josman", tags);
        for (String tagName : filteredTags.keySet()) {
            RepositoryTag tag = filteredTags.get(tagName);
            LOG.info(tag.getName());
            LOG.info(tag.getCommit().getSha());
        }
    }

    @Test
    public void testReadRepo() throws IOException, GitAPIException {
        File repoFile = createSampleGitRepo();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(repoFile)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        LOG.log(Level.INFO, "directory: {0}", repo.getDirectory().getAbsolutePath());

        LOG.log(Level.INFO, "Having repository: {0}", repo.getDirectory().getAbsolutePath());

        LOG.log(Level.INFO, "current branch: {0}", repo.getBranch());
    }

    @Test
    public void testWalkRepo() throws IOException {
        File repoFile = createJosmanSampleRepo();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(repoFile)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();        

        //printFile(repo, tree, "*a.*");
        printDirectory(repo, "master", "src");

        // there is also FileMode.SYMLINK for symbolic links, but this is not handled here yet
        repo.close();
    }

    private static RevTree getTree(Repository repository) throws AmbiguousObjectException, IncorrectObjectTypeException,
            IOException, MissingObjectException {
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);

        // a RevWalk allows to walk over commits based on some filtering
        RevWalk revWalk = new RevWalk(repository, DEPTH);
        RevCommit commit = revWalk.parseCommit(lastCommitId);

        System.out.println("Time of commit (seconds since epoch): " + commit.getCommitTime());

        // and using commit's tree find the path
        RevTree tree = commit.getTree();
        System.out.println("Having tree: " + tree);
        return tree;
    }

    private static void printFile(Repository repository, RevTree tree, String filter) {
        try {
            // now try to find a specific file
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            treeWalk.setFilter(PathFilter.create(filter));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file " + filter);
            }

            // FileMode specifies the type of file, FileMode.REGULAR_FILE for normal file, FileMode.EXECUTABLE_FILE for executable bit
// set
            FileMode fileMode = treeWalk.getFileMode(0);
            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            System.out.println(treeWalk.getPathString() + ": " + getFileMode(fileMode) + ", type: " + fileMode.getObjectType() + ", mode: " + fileMode
                    + " size: " + loader.getSize());
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while walkinf files", ex);
        }
    }

    private static void printDirectory(Repository repo, String revStr, String prefix) {
        checkNotNull(repo);
        checkNotNull(prefix);
        checkNotEmpty(revStr, "invalid revisiong string!");
        try {
            // find the HEAD
            ObjectId lastCommitId = repo.resolve(revStr);

            // a RevWalk allows to walk over commits based on some filtering that is defined
            RevWalk revWalk = new RevWalk(repo, DEPTH);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // and using commit's tree find the path
            RevTree tree = commit.getTree();
            LOG.log(Level.INFO, "Having tree: {0}", tree);
            // look at directory, this has FileMode.TREE
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);

            treeWalk.setFilter(PathFilter.create(prefix));// careful this looks for *exact* dir name (i.e. 'docs' will work for docs/a.txt but 'doc' won't work) 
            
            while (treeWalk.next()) {
                // FileMode now indicates that this is a directory, i.e. FileMode.TREE.equals(fileMode) holds true
                String pathString = treeWalk.getPathString();

                if (pathString.startsWith(prefix)) {
                    FileMode fileMode = treeWalk.getFileMode(0);
                    System.out.println(pathString + ":  " + getFileMode(fileMode) + ", type: " + fileMode.getObjectType() + ", mode: " + fileMode);

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repo.open(objectId);
                    
                    InputStream stream = loader.openStream();

                    File outputFile = File.createTempFile("bla", "bla");
                    FileUtils.copyInputStreamToFile(stream, outputFile);
                    stream.close();

                    System.out.println("content:\n" + FileUtils.readFileToString(outputFile));

                }
            }
        }

        catch (Exception ex) {
            throw new RuntimeException("Error while walking directory!", ex);
        }
    }

    private static String getFileMode(FileMode fileMode) {
        if (fileMode.equals(FileMode.EXECUTABLE_FILE)) {
            return "Executable File";
        } else if (fileMode.equals(FileMode.REGULAR_FILE)) {
            return "Normal File";
        } else if (fileMode.equals(FileMode.TREE)) {
            return "Directory";
        } else if (fileMode.equals(FileMode.SYMLINK)) {
            return "Symlink";
        } else {
            // there are a few others, see FileMode javadoc for details
            throw new IllegalArgumentException("Unknown type of file encountered: " + fileMode);
        }
    }

    private static File createFile(Repository repository, String filePath) {

        try {
            // create the file

            File targetFile = new File(repository.getDirectory().getParent(), filePath);
            File parent = targetFile.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }

            targetFile.createNewFile();

            PrintWriter pw = new PrintWriter(targetFile);
            pw.println("hello");
            pw.close();

            // run the add-call
            new Git(repository).add()
                    .addFilepattern(filePath)
                    .call();

            return targetFile;

        }
        catch (Exception ex) {
            throw new RuntimeException("Error while creating file!", ex);
        }
    }

    private static ImmutableList<File> createFiles(Repository repository, String... filePaths) {
        ImmutableList.Builder<File> retb = ImmutableList.builder();
        for (String filePath : filePaths) {
            retb.add(createFile(repository, filePath));
        }
        return retb.build();
    }

    /**
     * Creates a sample josman repo
     *
     *
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    private static File createJosmanSampleRepo() {
        Repository repository;
        try {
            repository = CookbookHelper.createNewRepository();

            LOG.log(Level.INFO, "Temporary repository at {0}", repository.getDirectory());

            createFiles(repository, "docs/README.md",
                    "docs/CHANGES.md",
                    "docs/img/a.jpg",
                    "src/main/java/a.java",
                    "src/main/java/b.java",
                    "README.md",
                    "LICENSE.txt"
            );

            // and then commit the changes
            new Git(repository).commit()
                    .setMessage("Added test files")
                    .call();

            File dir = repository.getDirectory();

            repository.close();

            return dir;
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while creating new repo!", ex);
        }

    }

    private static File createSampleGitRepo() throws IOException, GitAPIException {
        Repository repository = CookbookHelper.createNewRepository();

        System.out.println("Temporary repository at " + repository.getDirectory());

        // create the file
        File myfile = new File(repository.getDirectory().getParent(), "testfile");
        myfile.createNewFile();

        // run the add-call
        new Git(repository).add()
                .addFilepattern("testfile")
                .call();

        // and then commit the changes
        new Git(repository).commit()
                .setMessage("Added testfile")
                .call();

        LOG.info("Added file " + myfile + " to repository at " + repository.getDirectory());

        File dir = repository.getDirectory();

        repository.close();

        return dir;

    }

    private static class CookbookHelper {

        public static Repository openJGitCookbookRepository() throws IOException {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();
            return repository;
        }

        public static Repository createNewRepository() throws IOException {
            // prepare a new folder
            File localPath = File.createTempFile("TestGitRepository", "");
            localPath.delete();

            // create the directory
            Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
            repository.create();

            return repository;
        }
    }

}
