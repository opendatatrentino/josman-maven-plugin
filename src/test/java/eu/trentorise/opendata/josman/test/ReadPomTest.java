package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.commons.TodUtils;
import eu.trentorise.opendata.commons.validation.Preconditions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class ReadPomTest {
    private static final Logger LOG = Logger.getLogger(ReadPomTest.class.getName());    

    @BeforeClass
    public static void beforeClass() {
        TodConfig.init(ReadPomTest.class);
    }  
    
    @Test
    public void testReadPom() throws FileNotFoundException, IOException, XmlPullParserException{
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream("pom.xml"));        
        LOG.log(Level.FINE, "artifactId = {0}", model.getArtifactId());
        Preconditions.checkNotEmpty(model.getArtifactId(), "Invalid artifact id!");
        LOG.log(Level.FINE, "version = {0}", model.getVersion());
        Preconditions.checkNotEmpty(model.getVersion(), "Invalid version id!");
    }
}
