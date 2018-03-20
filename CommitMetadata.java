import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.io.File;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class CommitMetadata implements Serializable {
    
    HashMap<File, Integer> fileLookups;
    Calendar commitTime;
    GreeNode myGreeNode;
    
    
    public CommitMetadata (GreeNode greeNode) {
        fileLookups = new HashMap<File, Integer>();
        commitTime = Calendar.getInstance();
        myGreeNode = greeNode;
    }
    
    public CommitMetadata (CommitMetadata m, GreeNode greeNode) {
        commitTime = Calendar.getInstance();
        fileLookups = m.fileLookups;
        myGreeNode = greeNode;
    }
    
    public CommitMetadata (CommitMetadata m, ArrayList<File> old, ArrayList<File> newly, 
        ArrayList<File> toRemove, int currentNumber, GreeNode greeNode) {
        fileLookups = new HashMap<File, Integer>();
        commitTime = Calendar.getInstance();
        for (File f : newly) {
            fileLookups.put(f, currentNumber);
        } for (File g : old) {
            if (!toRemove.contains(g) && !newly.contains(g)) {
                fileLookups.put(g, m.getPreviousNumber(g));
            }
        }
        myGreeNode = greeNode;
    }
    
    int getPreviousNumber(File filename) {
        return fileLookups.get(filename);
    }      
    
    public String getDate () {
        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        return dateFormat.format(commitTime.getTime());
    }
    
    public void changeLookup(File f, Integer i) {
        fileLookups.put(f, i);
    }

}
