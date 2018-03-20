import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException; 
import java.io.Serializable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption; //Learned how to copy files from javapractices.com 

public class Gitlet implements Serializable {
    
    HashMap<Integer, CommitMetadata> commitMetadata;// .gitlet/commits
    GreeNode currentCommit; // .gitlet/currentCommit.ser
    ArrayList<File> newlyStagedFiles; 
    ArrayList<File> oldStagedFiles;
    ArrayList<File> toRemoveFiles; 
    HashMap<Integer, String> messages;
    HashMap<String, ArrayList<Integer>> commitByMessage;
    Integer curNum;
    boolean initialized = false;
    HashMap<String, GreeNode> branches;
    String currentBranch;
    
    
    public Gitlet () {
        //System.out.println("Here's init!");
        File dotGitlet = new File(".gitlet" + File.separator);
        dotGitlet.mkdirs();
        File commits = new File(".gitlet" + File.separator +"commits");
        commits.mkdirs();
        newlyStagedFiles = new ArrayList<File>();
        oldStagedFiles = new ArrayList<File>();
        toRemoveFiles = new ArrayList<File>();
        messages = new HashMap<Integer, String>();
        messages.put(0, "initial commit");
        currentCommit = new GreeNode(0);
        commitMetadata = new HashMap<Integer, CommitMetadata>();
        commitMetadata.put(0, new CommitMetadata(currentCommit));
        curNum = 0;
        initialized = true;
        commitByMessage = new HashMap<String, ArrayList<Integer>>();
        branches = new HashMap<String, GreeNode>();
        branches.put("master", currentCommit);
        currentBranch = "master";
    }
    
    public static void main(String[] args) {
        Gitlet myGitlet = tryLoadingMyGitlet();
        String command = args[0];
        switch (command) {
            case "init":
                if (myGitlet == null) {
                    myGitlet = new Gitlet();
                } else if (myGitlet.initialized) {
                    String msg = "A gitlet version control system already exists in the current directory.";
                    System.out.println(msg);
                } else {
                    myGitlet = new Gitlet();
                }
                break;
            case "add": 
                String addFilename = args[1];
                myGitlet.add(addFilename);
                break;
            case "commit":
                String commitMessage = args[1];
                for (int i = 2; i < args.length; i++) {
                    commitMessage = commitMessage + " " + args[i];
                }
                myGitlet.commit(commitMessage);
                break;
            case "remove":
                String removeFilename = args[1];
                myGitlet.remove(removeFilename);
                break;
            case "log":
                myGitlet.log();
                break;
            case "global-log":
                myGitlet.globalLog();
                break;
            case "find":
                String findMessage = args[1];
                for (int i = 2; i < args.length; i++) {
                    findMessage = findMessage + " " + args[i];
                }
                myGitlet.find(findMessage);
                break;
            case "status":
                myGitlet.status();
                break;
            case "checkout":
                boolean checkoutConfirmation = myGitlet.printDangerMessage();
                if (!checkoutConfirmation) {
                    break;
                }
                if (args.length == 3) {
                    int commitID = Integer.parseInt(args[1]);
                    String filename = args[2];
                    myGitlet.checkout(commitID, filename);
                } else {
                    String fileOrBranch = args[1];
                    myGitlet.checkout(fileOrBranch);
                }
                break;
            case "branch":
                String branchName = args[1];
                myGitlet.branch(branchName);
                break;
            case "rm-branch": 
                String rmBranchName = args[1];
                myGitlet.removeBranch(rmBranchName);
                break;
            case "reset":
                boolean resetConfirmation = myGitlet.printDangerMessage();
                if (!resetConfirmation) {
                    break;
                }
                Integer resetCommitID = Integer.parseInt(args[1]);
                myGitlet.reset(resetCommitID);
                break;
            case "merge":
                boolean mergeConfirmation = myGitlet.printDangerMessage();
                if (!mergeConfirmation) {
                    break;
                }
                String mergeBranch = args[1];
                myGitlet.merge(mergeBranch);
                break;
            case "rebase": 
                boolean rebaseConfirmation = myGitlet.printDangerMessage();
                if (!rebaseConfirmation) {
                    break;
                }
                String rbBranchName = args[1];
                myGitlet.rebase(rbBranchName);
                break;
            case "i-rebase":
                boolean iRebaseConfirmation = myGitlet.printDangerMessage();
                if (!iRebaseConfirmation) {
                    break;
                }
                String irbBranchName = args[1];
                myGitlet.interactiveRebase(irbBranchName);
                break;
            default:
                System.out.println("That's not a valid command!");
                break;
        }
        saveMyGitlet(myGitlet);
    }
                    
    private void add(String filename) {            
        File newFile = new File(filename);
        if (!newFile.exists() || newFile.isDirectory()) {
            System.out.println("File does not exist or is a directory.");
        } else if (toRemoveFiles.contains(newFile)) {
            toRemoveFiles.remove(newFile);
        } else { /*Learned how to convert file to byte[] from mkyong.com*/
            if (oldStagedFiles.contains(newFile)) {
                File oldFile = getLatest(newFile, currentCommit);
                if (oldFile == null) {
                    newlyStagedFiles.add(newFile);
                    return;
                } 
                if (!isModified(newFile, oldFile)) {
                    System.out.println("File has not been modified since the last commit.");
                } else {
                    newlyStagedFiles.add(newFile);
                }
            } else {
                newlyStagedFiles.add(newFile);
            }
        }
    }
    
    private boolean isModified(File newFile, File oldFile) { //learned how to convert file to byte[] from mkyong.com
        try {
            FileInputStream newStream = new FileInputStream(newFile);
            byte[] newBytes = new byte[(int) newFile.length()];
            newStream.read(newBytes);
            FileInputStream oldStream = new FileInputStream(oldFile);
            byte[] oldBytes = new byte[(int) oldFile.length()];
            oldStream.read(oldBytes);
            newStream.close();
            oldStream.close();
            if (checkEqualBytes(newBytes, oldBytes)) {
                return false;
            } else {
                return true;
            }
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException while adding");
        } catch (IOException e) {
            System.out.println("IOException while adding");
        }
        return false;
    }
    
    private void commit(String message) {
        if (newlyStagedFiles.size() == 0 && toRemoveFiles.size() == 0) {
            System.out.println("No changes added to the commit.");
        } else if (message == null) {
            System.out.println("Please enter a commit message.");
        } else {
            int parentCommitNum = currentCommit.identity;
            GreeNode newTree = new GreeNode(curNum + 1, currentCommit);
            currentCommit = newTree;
            branches.put(currentBranch, currentCommit);
            
            CommitMetadata previousCommit = commitMetadata.get(parentCommitNum);
            
            CommitMetadata newCommit = new CommitMetadata(previousCommit, oldStagedFiles, newlyStagedFiles, 
                toRemoveFiles, curNum + 1, currentCommit);
            commitMetadata.put(curNum + 1, newCommit);
            
            messages.put(curNum + 1, message);
            if (commitByMessage.containsKey(message)) {
                ArrayList<Integer> array = commitByMessage.get(message);
                array.add(curNum + 1);
            } else {
                ArrayList<Integer> array = new ArrayList<Integer>();
                array.add(curNum + 1);
                commitByMessage.put(message, array);
            }
            
            curNum += 1; //finished setting up tree
            
            //setting up copies of files
            for (File f : newlyStagedFiles) {
                String destination = f.getPath();
                String filenameOnly = f.getName();
                File checkDirectory = new File(".gitlet" + File.separator + "commits" 
                    + File.separator + destination + File.separator);
                if (!checkDirectory.isDirectory()) {
                    checkDirectory.mkdirs();
                }
                destination = ".gitlet" + File.separator + "commits" + File.separator
                    + destination + File.separator + + curNum + filenameOnly;
                copyTo(f.getPath(), destination);
                
                //Make sure they go into oldStagedFiles
                if (!oldStagedFiles.contains(f)) {
                    oldStagedFiles.add(f);
                }
            }
            
            for (File g : toRemoveFiles) {
                if (oldStagedFiles.contains(g)) {
                    oldStagedFiles.remove(g);
                }
            }
            
            toRemoveFiles = new ArrayList<File>();
            newlyStagedFiles = new ArrayList<File>();
        }
    }
    
    private void remove(String filename) {
        File removeMe = new File(filename);
        if (!oldStagedFiles.contains(filename) && !newlyStagedFiles.contains(removeMe)) {
            System.out.println("No reason to remove the file.");
        }
        if (newlyStagedFiles.contains(removeMe)) {
            newlyStagedFiles.remove(removeMe);
        } else if (oldStagedFiles.contains(removeMe)) {
            toRemoveFiles.add(removeMe);
        }
    }
    
    private void log() {
        GreeNode pointer = currentCommit;
        while (pointer != null) {
            int pointerNum = pointer.identity;
            System.out.println("====");
            System.out.println("Commit " + pointerNum + ".");
            System.out.println(commitMetadata.get(pointerNum).getDate());
            System.out.println(messages.get(pointerNum));
            System.out.println("");
            pointer = pointer.parent;
        }
    }
    
    private void globalLog() {
        for (int i = curNum; i > -1; i--) {
            System.out.println("====");
            System.out.println("Commit " + i + ".");
            System.out.println(commitMetadata.get(i).getDate());
            System.out.println(messages.get(i));
            System.out.println("");
        }
    }
    
    private void find(String message) {
        if (!commitByMessage.containsKey(message)) {
            System.out.println("Found no commit with that message.");
        } else {
            ArrayList<Integer> answers = commitByMessage.get(message);
            for (int i : answers){
                System.out.println(i);
            }
        }
    }
        
    private void status() {
        System.out.println("=== Branches ===");
        Set<String> branchNames = branches.keySet();
        for (String s : branchNames) {
            if (s.equals(currentBranch)) {
                System.out.println("*" + s);
            } else {
                System.out.println(s);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        for (File f : newlyStagedFiles) {
            System.out.println(f.getPath());
        }
        System.out.println("");
        System.out.println("=== Files Marked for Removal ===");
        for (File g : toRemoveFiles) {
            System.out.println(g.getPath());
        }
    }
     
    private void checkout(int commitID, String filename) {
        if (!commitMetadata.containsKey(commitID)) {
            System.out.println("No commit with that ID exists.");
        } else {
            CommitMetadata data = commitMetadata.get(commitID);
            File file = new File(filename);
            if (!data.fileLookups.containsKey(file)) {
                System.out.println("File does not exist in that commit.");
            } else {
                int latestCommitID = data.fileLookups.get(file); 
                String committedFilename = ".gitlet" + File.separator + "commits"  + File.separator
                + file.getPath() + File.separator + latestCommitID + file.getName();
                File restoreFile = new File(committedFilename);
                copyTo(restoreFile.getPath(), filename);
            }
        }
    }
    
    private void checkout(String fileOrBranch) {
        if (branches.containsKey(fileOrBranch)) {
            if (currentBranch.equals(fileOrBranch)) {
                System.out.println("No need to checkout the current branch.");
            } else {
                oldStagedFiles = new ArrayList<File>();
                currentCommit = branches.get(fileOrBranch);
                currentBranch = fileOrBranch;
                CommitMetadata data = commitMetadata.get(currentCommit.identity);
                Set<File> keys = data.fileLookups.keySet();
                for (File f : keys) {
                    String originalLocation = ".gitlet" + File.separator + "commits" + 
                        File.separator + f.getPath() + File.separator + data.fileLookups.get(f) + f.getName();
                    String copyToLocation = f.getPath();
                    oldStagedFiles.add(f);
                    copyTo(originalLocation, copyToLocation);
                }
            }
        } else {
            CommitMetadata data = commitMetadata.get(currentCommit.identity);
            File file = new File(fileOrBranch);
            if (!data.fileLookups.containsKey(file)) {
                System.out.println("File does not exist in the most recent commit, or no such branch exists.");
            } else {
                int latestCommitID = data.fileLookups.get(file); 
                String committedFilename = ".gitlet" + File.separator + "commits"  + File.separator
                + file.getPath() + File.separator + latestCommitID + file.getName();
                File restoreFile = new File(committedFilename);
                copyTo(restoreFile.getPath(), fileOrBranch);
            }
        }
    }
    
    private void branch (String branchName) {
        branches.put(branchName, currentCommit);
    }
    
    private void removeBranch (String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else { 
            branches.remove(branchName);
        }
    }
    
    private void reset (Integer commitID) {
        if (!commitMetadata.containsKey(commitID)) {
            System.out.println("No commit with that ID exists.");
        } else {
            CommitMetadata data = commitMetadata.get(commitID);
            Set<File> keys = data.fileLookups.keySet();
            for (File f : keys) {
                checkout(commitID, f.getPath());
            }
            branches.put(currentBranch, data.myGreeNode);
            currentCommit = data.myGreeNode;
        }
    }
         
    private void merge(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
        } else {
            GreeNode otherGreeNode = branches.get(branchName);
            GreeNode commonAncestor = currentCommit.commonAncestor(otherGreeNode);
            CommitMetadata otherData = commitMetadata.get(otherGreeNode.identity);
            CommitMetadata oldData = commitMetadata.get(commonAncestor.identity);
            CommitMetadata myData = commitMetadata.get(currentCommit.identity);
            Set<File> otherKeys = otherData.fileLookups.keySet();
            for (File f : otherKeys) {
                Integer oldestChange = oldData.fileLookups.get(f);
                if (oldestChange == null) { //f is a new file added since ancestor
                    String storedFile = ".gitlet" + File.separator + "commits" + File.separator +
                        f.getPath() + File.separator + otherData.fileLookups.get(f) + f.getName();
                    if (myData.fileLookups.containsKey(f) && !myData.fileLookups.get(f).equals(oldestChange)) { //created a file with the same name, also we're not the same exact commit
                            String destination = f.getPath() + ".conflicted";
                            copyTo(storedFile, destination);
                    } else { //added only in given branch
                        String destination = f.getPath();
                        copyTo(storedFile, destination);
                    }
                } else { //f was contained in ancestor
                    if (!oldestChange.equals(otherData.fileLookups.get(f))) { //been changed in given branch
                        String storedFile = ".gitlet" + File.separator + "commits" + File.separator +
                            f.getPath() + File.separator + otherData.fileLookups.get(f) + f.getName();
                        if (myData.fileLookups.containsKey(f) && 
                                !myData.fileLookups.get(f).equals(oldestChange)) { //changed in current branch
                            String destination = f.getPath() + ".conflicted";
                            copyTo(storedFile, destination);
                        } else { //Changed only in given branch
                            String destination = f.getPath();
                            copyTo(storedFile, destination);
                        }
                    }
                }
            }       
        }
    }
     
    private void rebase(String rebaseBranch) {
        if (!branches.containsKey(rebaseBranch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(rebaseBranch)) {
            System.out.println("Cannot rebase a branch onto itself.");
        } else {
            GreeNode otherGreeNode = branches.get(rebaseBranch);
            GreeNode commonAncestor = currentCommit.commonAncestor(otherGreeNode);
            if (commonAncestor.equals(otherGreeNode)) {
                System.out.println("Already up-to-date.");
                return;
            } else if (commonAncestor.equals(currentCommit)) { //move branch pointer to head
                currentCommit = otherGreeNode;
                branches.put(currentBranch, currentCommit);
                String tempBranch = currentBranch;
                checkout(rebaseBranch);
                checkout(tempBranch);
            } else {
                ArrayList<Integer> commitsToReplay = new ArrayList<Integer>();
                GreeNode pointer = currentCommit; //where old branch pointer was
                GreeNode pointer2 = currentCommit;
                int firstNewID = curNum + 1;
                currentCommit = otherGreeNode;
                while (pointer != commonAncestor) { //put all relationships in place
                    commitsToReplay.add(pointer.identity);
                    currentCommit = new GreeNode(curNum + 1, currentCommit);
                    curNum += 1;
                    pointer = pointer.parent;
                }
                int lastNewID = curNum;
                int currentAssociatingID = lastNewID;
                GreeNode pointer3 = currentCommit; //points to new branch, but for iterating
                
                /*for (int a = commitsToReplay.size() - 1; a > -1; a--) {
                    Integer currentlyReplaying = commitsToReplay.get(a);
                    CommitMetadata oldHead = commitMetadata.get(currentlyReplaying);
                    CommitMetadata newData = new commitMetadata(oldHead, pointer3);
                    commitMetadata.put(currentAssociatingID, newData);
                    currentAssociatingID += 1;
                    pointer3 = pointer3.parent;
                */
                
                while (pointer2 != commonAncestor) { //associate metadata with commitIDs
                    CommitMetadata oldHead = commitMetadata.get(pointer2.identity);
                    CommitMetadata newData = new CommitMetadata(oldHead, pointer3);
                    commitMetadata.put(currentAssociatingID, newData);
                    currentAssociatingID -= 1;
                    pointer2 = pointer2.parent;
                    pointer3 = pointer3.parent;
                }
                //now need to make changes go through every new commit
                CommitMetadata rebaseData = commitMetadata.get(otherGreeNode.identity);
                CommitMetadata ancestorData = commitMetadata.get(commonAncestor.identity);
                Set<File> rebaseKeys = rebaseData.fileLookups.keySet();
                
                for (File f : rebaseKeys) {
                    Integer oldestChange = ancestorData.fileLookups.get(f);
                    Integer newestChange = rebaseData.fileLookups.get(f);
                    if (oldestChange == null) { // f is a new file added since ancestor
                        for (int i = firstNewID; i < lastNewID + 1; i++) {
                            CommitMetadata splayedData = commitMetadata.get(i);
                            if (splayedData == null) {
                                System.out.println("omg splayed is null");
                            } else if (!splayedData.fileLookups.containsKey(f)) {
                                splayedData.changeLookup(f, newestChange);
                            }
                        }
                    } else { //ancestor contains f
                        if (!oldestChange.equals(newestChange)) { //was included in ancestor, and changed
                            for (int j = firstNewID; j < lastNewID + 1; j++) {
                                CommitMetadata splayedData = commitMetadata.get(j);
                                if (!splayedData.fileLookups.containsKey(f)) { //it must have removed it
                                    j = lastNewID;
                                } else if (splayedData.fileLookups.get(f).equals(oldestChange)) {
                                    splayedData.changeLookup(f, newestChange);
                                }
                            }
                        }
                    }          
                } 
                int counter = firstNewID;
                for (int p = commitsToReplay.size() - 1; p > -1; p--) {
                    Integer q = commitsToReplay.get(p);
                    String oldMessage = messages.get(q);
                    messages.put(counter, oldMessage);
                    addMessage(oldMessage, counter);
                    counter += 1;
                }
                branches.put(currentBranch, currentCommit);
            }
        }
    }
    
    private void interactiveRebase(String rebaseBranch) {
        if (!branches.containsKey(rebaseBranch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(rebaseBranch)) {
            System.out.println("Cannot rebase a branch onto itself.");
        } else {
            GreeNode otherGreeNode = branches.get(rebaseBranch);
            GreeNode commonAncestor = currentCommit.commonAncestor(otherGreeNode);
            if (commonAncestor.equals(otherGreeNode)) {
                System.out.println("Already up-to-date.");
                return;
            } else if (commonAncestor.equals(currentCommit)) { //move branch pointer to head
                currentCommit = otherGreeNode;
                branches.put(currentBranch, currentCommit);
                String tempBranch = currentBranch;
                checkout(rebaseBranch);
                checkout(tempBranch);
            } else { //hard stuf
                ArrayList<Integer> commitsToReplay = new ArrayList<Integer>();
                int firstNewID = curNum + 1;
                int currentCommitID = firstNewID;
                GreeNode pointer = currentCommit; //where old branch pointer was
                GreeNode pointer2 = currentCommit;
                while (pointer != commonAncestor) {
                    commitsToReplay.add(pointer.identity);
                    pointer = pointer.parent;
                }
                Scanner input = new Scanner(System.in);
                for (int i = commitsToReplay.size() - 1; i > -1; i--) {
                    Integer commitID = commitsToReplay.get(i);
                    CommitMetadata currentlyReplaying = commitMetadata.get(commitID);
                    System.out.println("Currently replaying: CommitID: " + commitID);
                    System.out.println(currentlyReplaying.getDate());
                    System.out.println(messages.get(commitID));
                    System.out.print("Would you like to (c)ontinue, (s)kip this commit, or change ");
                    System.out.println("this commit's (m)essage?");
                    boolean validCommandReceived = false;
                    while (!validCommandReceived) {
                        String command = input.nextLine();
                        switch (command) {
                            case "c":
                                validCommandReceived = true;
                                currentCommit = new GreeNode(curNum + 1, currentCommit);
                                curNum += 1;
                                CommitMetadata newOldCommit = new CommitMetadata(currentlyReplaying, currentCommit);
                                commitMetadata.put(curNum, newOldCommit);
                                String oldMessage = messages.get(commitID);
                                messages.put(curNum, oldMessage);
                                addMessage(oldMessage, curNum);
                                break;
                            case "s":
                                if (i == commitsToReplay.size() || i == 0) {
                                    System.out.println("Cannot skip the first or the last commit.");
                                } else {
                                    validCommandReceived = true;
                                }
                                break;
                            case "m":
                                System.out.println("Please enter a new message for this commit.");
                                String message = input.nextLine();
                                validCommandReceived = true;
                                currentCommit = new GreeNode(curNum + 1, currentCommit);
                                curNum += 1;
                                CommitMetadata msgNewOldCommit = new CommitMetadata(currentlyReplaying, currentCommit);
                                commitMetadata.put(curNum, msgNewOldCommit);
                                messages.put(curNum, message);
                                addMessage(message, curNum);
                                break;
                            default: 
                                System.out.print("Invalid command. Would you like to (c)ontinue, (s)kip");
                                System.out.println(" this commit, or change this commit's (m)essage?");
                        }
                    }                  
                }             
            }
        }
    }
                   
    private void addMessage(String message, Integer num) {
        if (!commitByMessage.containsKey(message)) {
            ArrayList<Integer> newList = new ArrayList<Integer>();
            commitByMessage.put(message, newList);
        } else {
            ArrayList<Integer> oldList = commitByMessage.get(message);
            oldList.add(num);
        }
    }
    
    private void copyTo(String original, String copy) {
        //System.out.println("Original file: " + original);
        //SysSystem.out.println("Destination: " + copy);
        Path from = Paths.get(original);
        Path to = Paths.get(copy);
        File testDirectory = new File(copy);
        testDirectory.mkdirs();
        CopyOption[] options = new CopyOption[] {
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        };
        try {
            Files.copy(from, to, options);
        } catch (IOException e) {
            System.out.println("IOException while copying files");
        }
    }
    
    private File getLatest(File file, GreeNode pointer) {
        int commitNumber = pointer.identity;
        //System.out.println("commitNumber = " + pointer.identity);
        CommitMetadata data = commitMetadata.get(commitNumber);
        if (!data.fileLookups.containsKey(file)) {
            return null;
        }
        int lastCommitNumber = data.fileLookups.get(file);
        String latest = ".gitlet" + File.separator + "commits" + File.separator 
            + file.getPath() + File.separator + lastCommitNumber + file.getName();
        File latestCommit = new File(latest);
        return latestCommit;
    }   
        
    private boolean checkEqualBytes (byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {  
            if (array1[i] != array2[i]) { 
                return false;   
            }           
        }       
        return true;
    }   
    
    private boolean printDangerMessage() {
        System.out.print("Warning: The command you entered may alter ");
        System.out.print("the files in your working directory. Uncommitted ");
        System.out.println("changes may be lost. Are you sure you want to continue? (yes/no)");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        //scanner.close();
        if (input.equals("yes")) {
            return true;
        } else {
            return false;
        }
    }
    
    private static Gitlet tryLoadingMyGitlet() {
        Gitlet myGitlet = null;
        File myGitletFile = new File(".gitlet" + File.separator + "myGitlet.ser");
        if (myGitletFile.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(myGitletFile);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                myGitlet = (Gitlet) objectIn.readObject();
                fileIn.close();
                objectIn.close();
            } catch (IOException e) {
                System.out.println("IOException while loading myGitlet");
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException loading myGitlet");
            }
        }
        return myGitlet;
    }
    
    private static void saveMyGitlet(Gitlet myGitlet) {
        if (myGitlet == null) {
            return;
        }
        try {
            File myGitletFile = new File(".gitlet" + File.separator + "myGitlet.ser");
            FileOutputStream fileOut = new FileOutputStream(myGitletFile);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(myGitlet);
            fileOut.close();
            objectOut.close();
        } catch (IOException e) {
            String msg = "IOException while saving myGitlet.";
            System.out.println(msg);
        }
    }
            
    
}