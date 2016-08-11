package eu.trentorise.opendata.josman;

import static com.google.common.base.Preconditions.checkNotNull;
import static eu.trentorise.opendata.commons.TodUtils.checkNotEmpty;
import eu.trentorise.opendata.commons.SemVersion;
import eu.trentorise.opendata.commons.validation.Preconditions;
import eu.trentorise.opendata.josman.exceptions.JosmanException;
import eu.trentorise.opendata.josman.exceptions.JosmanIoException;
import eu.trentorise.opendata.josman.exceptions.JosmanNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.DirectoryWalker;

/**
 * Copies directory to destination and converts md files to html.
 * @author David Leoni
 */
@Immutable
public class DirWalker extends DirectoryWalker {
    private static final Logger LOG = Logger.getLogger(DirWalker.class.getName());

    
    
    private File sourceRoot;
    private File destinationRoot;
    private JosmanProject project;
    private SemVersion version;
    private List<String> relPaths;
    private Map<String, String> evals;

    /**
     * @throws JosmanNotFoundException if source root doesn't exists    
     */
    public DirWalker(
                    File sourceRoot, 
                    File destinationRoot, 
                    JosmanProject josman, 
                    SemVersion version,
                    List<String> relPaths,
                    Map<String, String> evals
    ) {
        super();
        checkNotNull(sourceRoot);
        if (!sourceRoot.exists()) {
            throw new JosmanNotFoundException("source root does not exists: " + sourceRoot.getAbsolutePath());
        }
        checkNotNull(destinationRoot);
        if (destinationRoot.exists()) {
            throw new JosmanNotFoundException("destination directory does already exists: " + destinationRoot.getAbsolutePath());
        }
        checkNotNull(josman);
        checkNotNull(version);
        checkNotNull(relPaths, "Invalid relative paths!");
        checkNotNull(evals, "Invalid evals!");
        
        if (relPaths.isEmpty()){
            throw new JosmanIoException("Docs directory is empty: " + sourceRoot.getAbsolutePath() 
            + "\n Required files: " + Josmans.requiredDocs(""));
        }
        this.sourceRoot = sourceRoot;
        this.destinationRoot = destinationRoot;
        this.project = josman;
        this.version = version;
        this.relPaths = relPaths;
        this.evals = evals;
    }

    /**
     * @throws JosmanIoException
     */
    public void process() {
        try {
            walk(sourceRoot, new ArrayList());
        } catch (IOException ex) {
            throw new JosmanIoException("Error while copying root " + sourceRoot.getAbsolutePath(), ex);
        }
    }   
    
    /**
     * Copies directory content to destination and adds processed directory File to results.
     * @param depth
     * @param results ignored
     * @throws JosmanIoException
     */
    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) {
        LOG.log(Level.INFO, "Processing directory {0}", directory.getAbsolutePath());
        File target = new File(destinationRoot, directory.getAbsolutePath().replace(sourceRoot.getAbsolutePath(), ""));
        if (target.exists()) {
            throw new JosmanIoException("Target directory already exists!! " + target.getAbsolutePath());
        }
        LOG.log(Level.INFO, "Creating target    directory {0}", target.getAbsolutePath());
        if (!target.mkdirs()) {
            throw new JosmanIoException("Couldn't create directory!! " + target.getAbsolutePath());
        }
        results.add(target);
        return true;
    }

    /**
     * Converts .md to .html and README.md to index.html . Other files are just
     * copied. Processed File is added to results.
     */
    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {       
        String targetRelPath = file
                            .getAbsolutePath()
                            .replace(sourceRoot.getAbsolutePath(), "")
                            .substring(1); // so we get rid of "\" at the beginning
        
        results.add(project.copyStream(
                            new FileInputStream(file), 
                            JosmanProject.DOCS_FOLDER + "/" + targetRelPath, 
                            version,
                            relPaths,
                            evals));
    }

    public File getSourceRoot() {
        return sourceRoot;
    }

    public File getDestinationRoot() {
        return destinationRoot;
    }

    public JosmanProject getProject() {
        return project;
    }

    public SemVersion getVersion() {
        return version;
    }

}
