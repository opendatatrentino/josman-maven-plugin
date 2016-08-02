package eu.trentorise.opendata.josman.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
     * @throws IOException 
     * @since 0.8.0
     */
    @Test
    public void testExec() throws IOException {
        
        MavenProject mvnPrj = createMinimalProject();
                
        
        File sourceRepo = createMinimalRepo();
        
        final String EXEC_TEST = "ExecTest";
        
        File f = new File(sourceRepo, "docs/"+EXEC_TEST + ".md");
        f.createNewFile();
            
        
        try(  PrintWriter out = new PrintWriter( f)  ){
            //out.println( "$exec{" + this.getClass().getCanonicalName() + ".testString()}" );           
                                                          
            out.println( "$exec{java.lang.System.out} AND $exec{java.lang.System.getProperties()}" );
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
                new File(pagesDirPath, Josmans.majorMinor(SemVersion.of(mvnPrj.getVersion())) + "/"+EXEC_TEST+".html"), "UTF-8");
        
        assertTrue(output.contains("java.runtime.name"));
        
        assertTrue(output.contains(System.out.toString()));
        
    }
    
}
