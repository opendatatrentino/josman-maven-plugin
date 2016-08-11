package eu.trentorise.opendata.josman.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.trentorise.opendata.commons.SemVersion;
import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.josman.JosmanProject;
import eu.trentorise.opendata.josman.Josmans;

/**
 * @since 0.8.0
 */
public class JosmanTest {

    private static final Logger LOG = Logger.getLogger(JosmanTest.class.getName());    
    
    public static final String MINIMAL_REPO_PATH = "src/test/resources/minimal-repo";

    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    
    @BeforeClass
    public static void beforeClass() {                
        TodConfig.init(JosmanTest.class);
    }
    
    
    
    /**
     * 
     * 
     * @since 0.8.0
     */
    @Test
    public void testMinimalProject() throws IOException {
        MavenProject mvnPrj = createMinimalProject();
        
        String sourceRepoDirPath = MINIMAL_REPO_PATH;
        String pagesDirPath = folder.newFolder("site")
                .getAbsolutePath();
        List<SemVersion> ignoredVersions;
        boolean snapshotMode = true;

        JosmanProject prj = new JosmanProject(mvnPrj,
                sourceRepoDirPath,
                pagesDirPath,
                new ArrayList(),
                snapshotMode);

        prj.generateSite();
    }
    
    /**
     * @since 0.8.0
     */    
    private MavenProject createMinimalProject() {
        MavenProject mvnPrj = new MavenProject();
        
        Build build = new Build();
        try {
            build.setOutputDirectory(folder.newFolder().getAbsolutePath());
            build.setTestOutputDirectory(folder.newFolder().getAbsolutePath());
        } catch (IOException ex) {
            throw new Error("Couldn't create output directories!", ex);
        }
        
        mvnPrj.setBuild(build);
       
        mvnPrj.setArtifactId("my-artifact-id");
        mvnPrj.setGroupId("my-group-id");
        //NOTE: repo is fictional and but it can still work if we use snapshotMode=true
        mvnPrj.setUrl("https://github.com/my-test-org/my-test-repo");
        mvnPrj.setVersion("0.0.0");
        return mvnPrj;
    }

    /**
     * 
     * @throws Error
     * 
     * @since 0.8.0
     */
    private File createMinimalRepo(){
        try {
            File ret = folder.newFolder();
            FileUtils.copyDirectory(new File(MINIMAL_REPO_PATH), ret);
            return ret;
        } catch (IOException ex) {        
            throw new Error("Couldn't copy minimal repo!", ex);
        }
    }
    
    /** 
     * @since 0.8.0
     */
    @Test
    public void testEval() throws IOException {
        
        MavenProject mvnPrj = createMinimalProject();
                
        
        File sourceRepo = createMinimalRepo();
        
        final String EVAL_TEST = "EvalTest";
        
        File f = new File(sourceRepo, "docs/"+ EVAL_TEST + ".md");
        f.createNewFile();
            
        
        try(  PrintWriter out = new PrintWriter( f)  ){
            out.println( "$eval{java.lang.System.in} AND $evalNow{java.lang.System.out} AND $evalNow{java.lang.System.getProperties()}" );
            out.close();
        }
        
        Map<String, String> evals = new HashMap<>();
        evals.put("java.lang.System.in", java.lang.System.in.toString());
        
        Josmans.saveEvalMap(evals, new File(sourceRepo, "target/apidocs/" + JosmanProject.RELATIVE_EVAL_FILEPATH));
        
        String sourceRepoDirPath = sourceRepo.getAbsolutePath();
        String pagesDirPath = folder.newFolder("site")
                .getAbsolutePath();
        List<SemVersion> ignoredVersions = new ArrayList<>();
        boolean snapshotMode = true;

        JosmanProject prj = new JosmanProject(mvnPrj,
                sourceRepoDirPath,
                pagesDirPath,
                ignoredVersions,
                snapshotMode);

        prj.generateSite();
        
        String output = FileUtils.readFileToString(
                new File(pagesDirPath, Josmans.majorMinor(SemVersion.of(mvnPrj.getVersion())) + "/"+EVAL_TEST+".html"), "UTF-8");
        
        assertTrue(output.contains(System.in.toString()));
        assertTrue(output.contains("java.runtime.name"));        
        assertTrue(output.contains(System.out.toString()));
        
        
    }

    /** 
     * @since 0.8.0
     */
    @Test
    public void testVariables() throws IOException {
        
        MavenProject mvnPrj = createMinimalProject();
                        
        File sourceRepo = createMinimalRepo();
        
        final String EVAL_TEST = "EvalTest";
        
        File f = new File(sourceRepo, "docs/"+ EVAL_TEST + ".md");
        f.createNewFile();
            
        
        try(  PrintWriter out = new PrintWriter( f)  ){                       
            out.println("a${project.version} b${josman.majorMinorVersion} c${josman.repoRelease}");            
            out.println("d$_{project.version} e$_{josman.majorMinorVersion} f$_{josman.repoRelease}   " );
            out.println("g#{version} h#{majorMinorVersion} i#{repoRelease} " );
            out.println("l#_{version} m#_{majorMinorVersion} n#_{repoRelease} " );            
            out.close();
        }
               
        String sourceRepoDirPath = sourceRepo.getAbsolutePath();
        String pagesDirPath = folder.newFolder("site")
                .getAbsolutePath();
        List<SemVersion> ignoredVersions = new ArrayList<>();
        boolean snapshotMode = true;

        JosmanProject prj = new JosmanProject(mvnPrj,
                sourceRepoDirPath,
                pagesDirPath,
                ignoredVersions,
                snapshotMode);

        prj.generateSite();
        
        String output = FileUtils.readFileToString(
                new File(pagesDirPath, Josmans.majorMinor(SemVersion.of(mvnPrj.getVersion())) + "/"+EVAL_TEST+".html"), "UTF-8");
        
        LOG.fine(output);
        
        assertTrue(output.contains("a"+mvnPrj.getVersion()));
        assertTrue(output.contains("b"+Josmans.majorMinor(SemVersion.of(mvnPrj.getVersion()))));        
        assertTrue(output.contains("c"+Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()),
                mvnPrj.getArtifactId(), SemVersion.of(mvnPrj.getVersion()))));


        assertTrue(output.contains("d${project.version}"));        
        assertTrue(output.contains("e${josman.majorMinorVersion}"));
        assertTrue(output.contains("f${josman.repoRelease}"));

        assertTrue(output.contains("g"+mvnPrj.getVersion()));
        assertTrue(output.contains("h"+Josmans.majorMinor(SemVersion.of(mvnPrj.getVersion()))));        
        assertTrue(output.contains("i"+Josmans.repoRelease(Josmans.organization(mvnPrj.getUrl()),
                mvnPrj.getArtifactId(), SemVersion.of(mvnPrj.getVersion()))));               
                
        assertTrue(output.contains("l#{version}"));        
        assertTrue(output.contains("m#{majorMinorVersion}"));
        assertTrue(output.contains("n#{repoRelease}"));
        
    }
    
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalDocs() throws IOException {
        MavenProject mvnPrj = createMinimalProject();
                        
        File sourceRepo = createMinimalRepo();
        
        final String EVAL_TEST = "EvalTest";
        
        File f = new File(sourceRepo, "docs/"+ EVAL_TEST + ".md");
        f.createNewFile();
            
        
        try(  PrintWriter out = new PrintWriter( f)  ){                                                          
            out.println( "$eval{java.lang.System.in} AND $evalNow{java.lang.System.out}" );
            out.close();
        }
               
        String sourceRepoDirPath = sourceRepo.getAbsolutePath();
        String pagesDirPath = folder.newFolder("site")
                .getAbsolutePath();
        List<SemVersion> ignoredVersions = new ArrayList<>();
        boolean snapshotMode = true;

        JosmanProject prj = new JosmanProject(mvnPrj,
                sourceRepoDirPath,
                pagesDirPath,
                ignoredVersions,
                snapshotMode);

        prj.evalDocs();
        
        prj.generateSite();
    }
    
}
