package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.josman.Josmans;
import eu.trentorise.opendata.josman.exceptions.JosmanException;

import java.util.logging.Logger;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class JosmansTest {

    private static final Logger LOG = Logger.getLogger(JosmansTest.class.getName());

    /**
     * @since 0.8.0
     */
    public static final String TEST_STRING = "test string";

    
    @BeforeClass
    public static void beforeClass() {        
        
        TodConfig.init(JosmansTest.class);
    }

    @Test
    public void testNotMeaningfulString() {

        Josmans.checkNotMeaningful(" a", "");
        Josmans.checkNotMeaningful(" a\n", "");
        Josmans.checkNotMeaningful(" a\t", "");

        try {
            Josmans.checkNotMeaningful("", "");
        }
        catch (IllegalArgumentException ex) {

        }

        try {
            Josmans.checkNotMeaningful(" ", "");
        }
        catch (IllegalArgumentException ex) {

        }
        try {
            Josmans.checkNotMeaningful("\\t", "");
        }
        catch (IllegalArgumentException ex) {

        }

        try {
            Josmans.checkNotMeaningful("\\n", "");
        }
        catch (IllegalArgumentException ex) {

        }

    }

    @Test
    public void testHtmlizePath() {
        assertEquals("docs/BLA.html", Josmans.htmlizePath("docs\\BLA.md"));
        
        assertEquals("a.html#f", Josmans.htmlizePath("a.html#f"));
        assertEquals("a.html#f", Josmans.htmlizePath("a.md#f"));
                
        assertEquals("a.html?q", Josmans.htmlizePath("a.html?q"));
        assertEquals("a.html?q#f", Josmans.htmlizePath("a.html?q#f"));
        
        assertEquals("a.html?q", Josmans.htmlizePath("a.md?q"));
        assertEquals("a.html?q#f", Josmans.htmlizePath("a.md?q#f"));
        
        assertEquals("some/Path", Josmans.htmlizePath("some/Path/"));
        assertEquals("/", Josmans.htmlizePath("\\"));

        try {
            Josmans.htmlizePath("");
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }
    }

    @Test
    public void testTargetName() {
        try {
            Josmans.targetName("");
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }

        assertEquals("Usage", Josmans.targetName("docs/README.md"));
        assertEquals("Release notes", Josmans.targetName("/CHANGES.md"));
        assertEquals("Hello world", Josmans.targetName("HelloWorld.html"));

        assertEquals("Ab CD ef", Josmans.targetName("AbCDEf.html"));

        assertEquals("Ab C de", Josmans.targetName("AbCDe.html"));
    }
    
        
    
    /**
     * @since 0.8.0
     */
    public static int number(){
        return 3;
    }
    
    /**
     * @since 0.8.0
     */
    public static int withParams(int i){
        return i;
    }
    

    /**
     * @since 0.8.0
     */
    @Test
    public void testExec(){
        String output = Josmans.execCmds("$exec{"+this.getClass().getCanonicalName() + ".number()}"
                + "-$exec{"+this.getClass().getCanonicalName()+".TEST_STRING}" ,
                        this.getClass().getClassLoader());
        
        assertEquals("3-"+TEST_STRING, output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testExecSpaces(){
        String output = Josmans.execCmds("a$exec{ "+this.getClass().getCanonicalName() + ".number() }b",
                this.getClass().getClassLoader());        
        
        assertEquals("a3b", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testExecEmpty(){
        String output = Josmans.execCmds("",
                this.getClass().getClassLoader());        
        
        assertEquals("", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testExecParameters(){
        
        try {
            Josmans.execCmds("a$exec{ "+this.getClass().getCanonicalName() + ".withParams(4) }b",
                    this.getClass().getClassLoader());
            Assert.fail("Params shouldn't be supported!");
        } catch (JosmanException ex){
            
        }
        
        //assertEquals("4", output);
    }

    /**
     * @since 0.8.0
     */
    @Test
    public void testVerbatimExec(){
        String output = Josmans.execCmds("$_exec{a}", this.getClass().getClassLoader());
        assertEquals("$exec{a}", output);
    }
}
