package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.TodConfig;
import eu.trentorise.opendata.josman.Josmans;
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
}
