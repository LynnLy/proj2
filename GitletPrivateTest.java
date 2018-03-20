import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 * 
 */
public class GitletPrivateTest {
    private static final String GITLET_DIR = ".gitlet/";
    private static final String TESTING_DIR = "test_files/";

    /* matches either unix/mac or windows line separators */
    private static final String LINE_SEPARATOR = "\r\n|[\r\n]";

    /**
     * Deletes existing gitlet system, resets the folder that stores files used
     * in testing.
     * 
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(GITLET_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f = new File(TESTING_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f.mkdirs();
    }

    /**
     * Tests that init creates a .gitlet directory. Does NOT test that init
     * creates an initial commit, which is the other functionality of init.
     */
    @Test
    public void testBasicInitialize() {
        gitlet("init");
        File f = new File(GITLET_DIR);
        assertTrue(f.exists());
    }

    /**
     * Tests that checking out a file name will restore the version of the file
     * from the previous commit. Involves init, add, commit, and checkout.
     */
    @Test
    public void testBasicCheckout() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("checkout", wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }

    /**
     * Tests that log prints out commit messages in the right order. Involves
     * init, add, commit, and log.
     */
    @Test
    public void testBasicLog() {
        gitlet("init");
        String commitMessage1 = "initial commit";

        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("add", wugFileName);
        String commitMessage2 = "added wug";
        gitlet("commit", commitMessage2);

        String logContent = gitlet("log");
        assertArrayEquals(new String[] { commitMessage2, commitMessage1 },
                extractCommitMessages(logContent));
    }
    
    @Test
    public void testCheckoutViaID() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        String commitID = "1";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "Ur mum's a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added ur mum");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("checkout", commitID, wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }
    
    @Test
    public void testBranchCheckout() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "1";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added 1");
        writeFile(wugFileName, "2");
        gitlet("add", wugFileName);
        gitlet("commit", "added 2");
        gitlet("branch", "otherBranch");
        gitlet("checkout", "otherBranch");
        writeFile(wugFileName, "3");
        gitlet("add", wugFileName);
        gitlet("commit", "added 3");
        assertEquals("3", getText(wugFileName));
        gitlet("checkout", "master");
        assertEquals("2", getText(wugFileName));
    }
    
    @Test
    public void testBasicMerge() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "1";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added 1");
        writeFile(wugFileName, "2");
        gitlet("add", wugFileName);
        gitlet("commit", "added 2");
        gitlet("branch", "otherBranch");
        gitlet("checkout", "otherBranch");
        writeFile(wugFileName, "3");
        gitlet("add", wugFileName);
        gitlet("commit", "added 3");
        assertEquals("3", getText(wugFileName));
        gitlet("checkout", "master");
        assertEquals("2", getText(wugFileName));
        gitlet("merge", "otherBranch");
        assertEquals("3", getText(wugFileName));
    }
    
    @Test
    public void testMerge() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "wug1"; //Will change in other
        String hugFileName = TESTING_DIR + "hug.txt";
        String hugText = "hug1"; //Will change in master
        String slugFileName = TESTING_DIR + "slug.txt";
        String slugText = "slug1"; //Will change in both
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        createFile(slugFileName, slugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("add", hugFileName);
        gitlet("add", slugFileName);
        gitlet("commit", "added wug1 hug1 slug1");
        gitlet("branch", "otherBranch");
        gitlet("checkout", "otherBranch"); //IN OTHER
        writeFile(wugFileName, "wug2");
        writeFile(slugFileName, "slug2");
        gitlet("add", wugFileName);
        gitlet("add", slugFileName);
        gitlet("commit", "added wug2 slug2");
        gitlet("checkout", "master"); //IN MASTER
        writeFile(hugFileName, "hug3");
        writeFile(slugFileName, "slug3");
        gitlet("add", hugFileName);
        gitlet("add", slugFileName);
        gitlet("commit", "added hug3 slug3");
        gitlet("merge", "otherBranch");
        assertEquals("wug2", getText(wugFileName));
        assertEquals("hug3", getText(hugFileName));
        assertEquals("slug3", getText(slugFileName));
        File f = new File(slugFileName + ".conflicted");
        assertTrue(f.exists());
        File g = new File(hugFileName + ".conflicted");
        assertTrue(!g.exists());
        File h = new File(wugFileName + ".conflicted");
        assertTrue(!h.exists());
    }
    
    @Test
    public void testRebase() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "wug1";
        String hugFileName = TESTING_DIR + "hug.txt";
        String hugText = "hug2";
        String slugFileName = TESTING_DIR + "slug.txt";
        String slugText = "slug4";
        createFile(wugFileName, wugText);
        createFile(slugFileName, slugText);
        createFile(hugFileName, hugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug1");
        gitlet("branch", "other");
        gitlet("checkout", "other");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug2");
        writeFile(wugFileName, "wug3");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug3");
        gitlet("checkout", "master");
        gitlet("add", slugFileName);
        gitlet("commit", "added slug4");
        writeFile(wugFileName, "wug5");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug5");
        gitlet("checkout", "other");
        gitlet("rebase", "master");
        assertEquals("wug3", getText(wugFileName));
        assertEquals("slug4", getText(slugFileName));
        assertEquals("hug2", getText(hugFileName));
        gitlet("reset", "6");
        assertEquals("wug5", getText(wugFileName)); 
    }
    
    @Test
    public void essentialTest() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "1";
        String hugFileName = TESTING_DIR + "hug.txt";
        String hugText = "1";
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug1");
        gitlet("branch", "other");
        gitlet("checkout", "other");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug");
        gitlet("checkout", "master");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug to master");
        gitlet("log");
    }
    
    @Test
    public void testReset() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "1";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added 1");
        writeFile(wugFileName, "2");
        gitlet("add", wugFileName);
        gitlet("commit", "added 2");
        writeFile(wugFileName, "3");
        gitlet("add", wugFileName);
        gitlet("commit", "added 3");
        assertEquals("3", getText(wugFileName));
        gitlet("reset", "1");
        assertEquals("1", getText(wugFileName));
    }

    /**
     * Convenience method for calling Gitlet's main. Anything that is printed
     * out during this call to main will NOT actually be printed out, but will
     * instead be returned as a string from this method.
     * 
     * Prepares a 'yes' answer on System.in so as to automatically pass through
     * dangerous commands.
     * 
     * The '...' syntax allows you to pass in an arbitrary number of String
     * arguments, which are packaged into a String[].
     */
    private static String gitlet(String... args) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
        try {
            /*
             * Below we change System.out, so that when you call
             * System.out.println(), it won't print to the screen, but will
             * instead be added to the printingResults object.
             */
            System.setOut(new PrintStream(printingResults));

            /*
             * Prepares the answer "yes" on System.In, to pretend as if a user
             * will type "yes". You won't be able to take user input during this
             * time.
             */
            String answer = "yes";
            InputStream is = new ByteArrayInputStream(answer.getBytes());
            System.setIn(is);

            /* Calls the main method using the input arguments. */
            Gitlet.main(args);

        } finally {
            /*
             * Restores System.out and System.in (So you can print normally and
             * take user input normally again).
             */
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return printingResults.toString();
    }

    /**
     * Returns the text from a standard text file (won't work with special
     * characters).
     */
    private static String getText(String fileName) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Creates a new file with the given fileName and gives it the text
     * fileText.
     */
    private static void createFile(String fileName, String fileText) {
        File f = new File(fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(fileName, fileText);
    }

    /**
     * Replaces all text in the existing file with the given text.
     */
    private static void writeFile(String fileName, String fileText) {
        FileWriter fw = null;
        try {
            File f = new File(fileName);
            fw = new FileWriter(f, false);
            fw.write(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file and all files inside it, if it is a directory.
     */
    private static void recursiveDelete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                recursiveDelete(f);
            }
        }
        d.delete();
    }

    /**
     * Returns an array of commit messages associated with what log has printed
     * out.
     */
    private static String[] extractCommitMessages(String logOutput) {
        String[] logChunks = logOutput.split("====");
        int numMessages = logChunks.length - 1;
        String[] messages = new String[numMessages];
        for (int i = 0; i < numMessages; i++) {
            System.out.println(logChunks[i + 1]);
            String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
            messages[i] = logLines[3];
        }
        return messages;
    }
    
    public static void main(String... args) {
        jh61b.junit.textui.runClasses(GitletPrivateTest.class);
    }       
}
