import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileSyncTest {
    public static int key = 0;
    private static int validFileCounter=0;
    private static int dirCounter=0;

    public static void main(String[] args) {
        String pattren = "TestWork";
        String extension = "txt";
        String destDir = "C:\\Temp\\CopyHere";
        String rootDir = "C:\\Temp\\OS3Test";
        String numOfCopier = "8";
        String numOfSearcher = "8";

        File fatherDestDir = new File(destDir);
        File fatherTestDir = new File(rootDir);
        if (!fatherTestDir.exists()) {
            if (!fatherTestDir.mkdir()) {
                System.out.println("Cant create father dir on temp folder");
                System.exit(1);
            }
        }
        for (int i = 1; i < 18; i++) {
            validFileCounter=0;
            File currentTreeDir=createTreeDir(fatherTestDir,i);
            deleteDestDir(fatherDestDir);
            startTest(currentTreeDir, i,0);
            executeCommand(pattren, extension, currentTreeDir.getAbsolutePath(), destDir, numOfSearcher, numOfCopier);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            checkIfCopyRightData(pattren, extension, fatherDestDir, i);
        }
        System.out.println("Finish to create tree of folders ");
    }

    private static void startTest(File fatherTestDir, int amountOfFolders, int key) {
        File fileToCreate = null;
        for (int i = 0; i < amountOfFolders; i++) {
            File currentDir = new File(fatherTestDir.getAbsolutePath() + "\\" + i);
            if (!currentDir.mkdir()) {
                System.out.println("Cant create subfather dir on " + fatherTestDir.getAbsolutePath());
                System.exit(1);
            }else {
                dirCounter++;
            }
            if (i % 3 == 0) {
                fileToCreate = new File(fatherTestDir.getAbsolutePath() + "\\" + i + "\\" + "ImTestFileWithWrongExtAndPattren" + key + ".pdf");
            }
            if (i % 3 == 1) {
                fileToCreate = new File(fatherTestDir.getAbsolutePath() + "\\" + i + "\\" + "ImTestWorkFileWithWrongExt" + key + ".pdf");
            }
            if (i % 3 == 2) {
                fileToCreate = new File(fatherTestDir.getAbsolutePath() + "\\" + i + "\\" + "ImTestWorkFileWithRightExt" + key +"-"+validFileCounter+ ".txt");
                validFileCounter++;
            }
            key++;
            try {
                if (!fileToCreate.createNewFile()) {
                    System.out.println("Can't create file");
                }else {
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(fileToCreate);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        writer.write("Test data");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            startTest(currentDir, i++,key);
        }
    }

    private static void executeCommand(String pattren, String extension, String rootDir, String destDir, String numOfSearcher, String numOfCopier) {
        String[] mainArgsDiskSearcher = { pattren, extension, rootDir, destDir, numOfSearcher, numOfCopier };
        DiskSearcher.main(mainArgsDiskSearcher);
    }

    private static void checkIfCopyRightData(String pattren, String extension, File destDir, int i) {
        String currentFileName;
        if (destDir.listFiles().length != validFileCounter) {
            System.out.println("Failed on dir tree of size " + i);
            System.out.println("Expected "+validFileCounter+ " right files to copy but got only "+destDir.listFiles().length);
            return;
        }
        for (File currentFile : destDir.listFiles()) {
            currentFileName = currentFile.getName();
            if (!(currentFileName.contains(pattren)) || !(currentFileName.substring(currentFileName.lastIndexOf('.') + 1)).equals(extension) || !(currentFile.isFile())) {
                System.out.println("################################ Failed on dir tree of size " + i + " ######################################");
                return;
            }
        }
        System.out.println("############################ Finished successfully on dir tree of size " + i+" ##################################");
    }
    
    private static void deleteDestDir(File fatherDestDir) {
        if (fatherDestDir.exists()) {
            String[] entries = fatherDestDir.list();
            for (String s : entries) {
                File currentFile = new File(fatherDestDir.getPath(), s);
                currentFile.delete();
            }
            fatherDestDir.delete();
        }
    }
    
    private static File createTreeDir(File fatherTestDir,int i) {
        File currentDir = new File(fatherTestDir.getAbsolutePath()+"\\FatherTreeDir" +i);
        if (!currentDir.mkdir()) {
            System.out.println("Cant create subfather dir on " + fatherTestDir.getAbsolutePath() +" for "+ i +" tree size");
            System.exit(1);
        }
        return currentDir;
    }
}
