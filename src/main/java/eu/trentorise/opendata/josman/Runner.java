package eu.trentorise.opendata.josman;

import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.System.exit;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author David Leoni
 */
public class Runner {
    private static final Logger LOG = Logger.getLogger(Runner.class.getName());
    
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String ORG = "org";
    public static final String PATH = "path";
    public static final String OUT = "out";
    public static final String IGNORE = "ignore";
    public static final String SNAPSHOT = "snapshot";
    
    
     public static void main(String[] args) throws IOException, URISyntaxException {

            
        String repoName = null;
        String repoTitle = null;
        String repoOrg = null;
        
        String repoPath = null;
        String outPath = null;
        List<SemVersion> ignoredVersions = null;
        boolean snapshot = true;
        String sep = File.separator;
        
         
        // create Options object
        Options options = new Options();

        options.addOption(NAME, true, "repository name i.e. josman");
        options.addOption(TITLE, true, "repository title i.e. Josman");
        options.addOption(ORG, true, "GitHub organization i.e. opendatatrentino");
        options.addOption(PATH, true, "repository directory");
        options.addOption(OUT, true, "output directory");
        options.addOption(IGNORE, true, "comma separated list of versions to ignore (i.e. 0.3.1,0.2.3)");
        options.addOption(SNAPSHOT, false, "only processes latest snapshot of the repository");         

        CommandLine cmd = null;
        CommandLineParser parser = new PosixParser();
         try {
             cmd = parser.parse( options, args);
             
         }
         catch (ParseException ex) {
             LOG.log(Level.SEVERE, "Error while parsing arguments!", ex);
             exit(1);
         }
        
        if (cmd.hasOption(NAME)){
            repoName = cmd.getOptionValue(NAME);
        } else {
            LOG.severe("Repository name is required!");
            exit(1);
        }
        
        if (cmd.hasOption(TITLE)){
            repoTitle = cmd.getOptionValue(TITLE);
        } else {
            LOG.severe("Repository title is required!");
            exit(1);
        }
        
        if (cmd.hasOption(ORG)){
            repoOrg = cmd.getOptionValue(ORG);
        } else {
            LOG.severe("Repository organization is required!");
            exit(1);
        }
        
        if (cmd.hasOption(PATH)){
            repoPath = cmd.getOptionValue(PATH);
        } else {
            LOG.severe("Path to local repository is required!");
            exit(1);
        }
        
        if (cmd.hasOption(OUT)){
            outPath = cmd.getOptionValue(OUT);
        } else {            
            outPath = repoPath + sep +"target" + sep + "site";
        }
        
        if (cmd.hasOption(IGNORE)){
            repoTitle = cmd.getOptionValue(IGNORE);
        } else {
            LOG.severe("Repository title is required!");
            exit(1);
        }
        
        LOG.severe("TODO Runner is not fully implemented yet!!");
        exit(1);
     
        // NOTE: Reading a pom file does not involve interpolation of data such 
        // as variables, inherited settings from parents (and their proto-parents) and so on. 
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            File pomFile = new File(repoPath + File.separator + "pom.xml");
            reader = new FileReader(pomFile);
            model = mavenreader.read(reader);
            model.setPomFile(pomFile);
        }catch(Exception ex){}
        MavenProject mvnPrj = new MavenProject(model);
        
        
        
        JosmanProject josman = new JosmanProject(
                mvnPrj,
                JosmanConfig.builder()
                .setSourceRepoDir(repoPath)  // ".."+ sep + ".." + sep + repoName + sep + "prj", // todo fixed path!
                .setPagesDir(outPath)        // ".."+ sep + ".." + sep + repoName + sep + "prj" + sep +"target" + sep + "site", // todo fixed path!
                .setIgnoredVersions(ignoredVersions) //ImmutableList.of(SemVersion.of("0.1.0")),
                .setSnapshot(snapshot)  //true
                .build()
        );

        josman.generateSite();
    }   
}
