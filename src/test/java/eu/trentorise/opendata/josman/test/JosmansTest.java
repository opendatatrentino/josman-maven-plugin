package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.josman.Josmans;
import eu.trentorise.opendata.josman.exceptions.ExprNotFoundException;
import eu.trentorise.opendata.josman.exceptions.JosmanException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

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

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    
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

    /**
     * @since 0.8.0
     */
    @Test
    public void testHtmlizeRelativePathDocs(){
        assertEquals("../c.html", Josmans.htmlizeRelativePath("docs/a/b.md", "../c.md"));
        assertEquals("../c.html", Josmans.htmlizeRelativePath("docs/b.md", "../c.md"));
        // shows we can escape before root, in this case no particular rewriting happens
        assertEquals("../../c.html", Josmans.htmlizeRelativePath("docs/b.md", "../../c.md"));        
        assertEquals("../../jackan/0.3/c.html", Josmans.htmlizeRelativePath("docs/b.md", "../../../../jackan/blob/branch-0.3/docs/c.md"));                       
    }

    /**
     * @since 0.8.0
     */
    @Test
    public void testHtmlizeRelativePathDocsDepthOne(){

        assertEquals("../../jackan/0.3/c.html", Josmans.htmlizeRelativePath("docs/a/b.md", "../../../../../jackan/blob/branch-0.3/docs/c.md"));
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testHtmlizeRelativePathRoot(){
        assertEquals("../jackan/index.html", 
                Josmans.htmlizeRelativePath("a.md", "../../../jackan/blob/branch-0.3/README.md"));
        assertEquals("../jackan/LICENSE.txt", 
                Josmans.htmlizeRelativePath("a.md", "../../../jackan/blob/branch-0.3/LICENSE.txt"));
        
        // if we point to stuff outside 'docs/', they are reported as is.
        assertEquals("../../../jackan/blob/branch-0.3/a/b.html", 
                Josmans.htmlizeRelativePath("a.md", "../../../jackan/blob/branch-0.3/a/b.md"));
    }

    /**
     * @since 0.8.0
     */
    @Test
    public void testHtmlizeRelativePathRootDepthOne(){

        assertEquals("../../jackan/index.html", 
            Josmans.htmlizeRelativePath("docs/b.md", "../../../../jackan/blob/branch-0.3/README.md"));
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
    public void testEval(){
        String output = Josmans.expandExprs("$eval{a.b}"
                + "-$eval{c.d()}" ,
                ImmutableMap.of("a.b","1", "c.d()","2"),
                "test-path",
                this.getClass().getClassLoader(), 
                false);
        
        assertEquals("1-2", output);
    }    

    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalMissingExpr(){
        
        try {
        Josmans.expandExprs("$eval{a.b}}" ,
                Collections.EMPTY_MAP,
                "test-path",                
                this.getClass().getClassLoader(),
                false);
        Assert.fail("Shouldn't arrive here!");
        } catch (ExprNotFoundException ex){
            
        }        
    }    
    
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalNow(){
        String output = Josmans.expandExprs("$evalNow{"+this.getClass().getCanonicalName() + ".number()}"
                + "-$evalNow{"+this.getClass().getCanonicalName()+".TEST_STRING}" ,
                Collections.EMPTY_MAP,
                "test-path",                
                this.getClass().getClassLoader(),
                false);
        
        assertEquals("3-"+TEST_STRING, output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalNowSpaces(){
        String output = Josmans.expandExprs("a$evalNow{ "+this.getClass().getCanonicalName() + ".number() }b",
                Collections.EMPTY_MAP,
                "test-path",                
                this.getClass().getClassLoader(),
                false);        
        
        assertEquals("a3b", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalEmpty(){
        String output = Josmans.expandExprs("",
                Collections.EMPTY_MAP,
                "test-path",                
                this.getClass().getClassLoader(),
                false);        
        
        assertEquals("", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalParameters(){
        
        try {
            Josmans.expandExprs("a$evalNow{ "+this.getClass().getCanonicalName() + ".withParams(4) }b",
                    Collections.EMPTY_MAP,
                    "test-path",                    
                    this.getClass().getClassLoader(),
                    false);
            Assert.fail("Params shouldn't be supported!");
        } catch (JosmanException ex){
            
        }
        
        //assertEquals("4", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testEvalIgnoreErrors(){
                
        String output = Josmans.expandExprs("a$evalNow{b666}c",
                Collections.EMPTY_MAP,
                "test-path",                    
                this.getClass().getClassLoader(),
                true);
        
        
        assertEquals("a$eval{b666}c", output);
    }

    /**
     * @since 0.8.0
     */
    @Test
    public void testVerbatimEvalNow(){
        String output = Josmans.expandExprs("$'evalNow{a}",
                Collections.EMPTY_MAP,
                "test-path",                
                this.getClass().getClassLoader(),
                false);
        assertEquals("$evalNow{a}", output);
    }
    
    /**
     * @since 0.8.0
     */
    @Test
    public void testBastardCsv() throws IOException{
        HashMap<String,String> evals = new HashMap();
        String BASTARD_STRING1 = "nasty 1 \"\'\n\n ( < >\r \t";
        String BASTARD_STRING2 = "nasty 2 \"\'\n( < >\r \t";
        evals.put("k1", BASTARD_STRING1);
        evals.put("k2", BASTARD_STRING2);
        File file = folder.newFile("eval.csv");
        Josmans.saveEvalMap(evals, file);
        
        Map<String, String> readMap = Josmans.loadEvalMap(file);
        assertEquals(evals.size(), readMap.size());
        assertEquals(evals.get("k1"), readMap.get("k1"));
        assertEquals(evals.get("k2"), readMap.get("k2"));        
    }
    
    // For testing doc expressions
    public static String calcDate(){
        return new Date().toString();
    }

    // For testing doc expressions
    public static String sayHello(){
        return "Hello!";
    }


    /**
     * @since 0.8.0
     */
    @Test
    public void testCheckExpr(){
        
        try {
            Josmans.checkExpr("");
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex){
            
        }
        
        try {
            Josmans.checkExpr(" ");
            Assert.fail("Shouldn't arrive here!");
        } catch (IllegalArgumentException ex){
            
        }
                
        Josmans.checkExpr("a");
        Josmans.checkExpr("123");
        
    }

}
