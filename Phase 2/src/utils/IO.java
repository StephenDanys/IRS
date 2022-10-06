package utils;

import java.io.*;
import java.util.Scanner;

/**
 * @author Tonia Kyriakopoulou
 */
public class IO {

    /**
     * List all the files under a directory
     *
     * @param directoryName to be listed
     * @return File[]
     */
    public static File[] listFiles(String directoryName) {

        File directory = new File(directoryName);

        //get all the files from a directory
        File[] fList = directory.listFiles();

        return fList;
    }

    /**
     * Reads entire file into a string
     *
     * @param file to be read
     * @return String
     */
    public static String ReadEntireFileIntoAString(String file) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(file));
        scanner.useDelimiter("\\A"); //\\A stands for :start of a string
        String entireFileText = scanner.next();
        return entireFileText;
    }

    /**
     * Reads file line by line
     *
     * @param file to be read
     * @return StringBuffer
     */
    public static StringBuffer ReadFileIntoAStringLineByLine(String file) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        StringBuffer stringBuffer = new StringBuffer();
        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuffer.append(line).append("\n");
        }
        return stringBuffer;
    }

    /**
     * Reads file char by char
     *
     * @param file to be read
     * @return String
     */
    public String ReadEntireFileIntoAStringCharByChar(String file) throws IOException {

        FileReader fileReader = new FileReader(file);

        String fileContents = "";

        int i;

        while ((i = fileReader.read()) != -1) {
            char ch = (char) i;

            fileContents = fileContents + ch;
        }
        return fileContents;
    }

    public static void CreateFile(String filename) {
        try {
            File file = new File("docs//trec_eval//" + filename);
            if (file.exists()) {
                //try delete existing file
                if (file.delete()) {
                    //try create new file in its place
                    if (file.createNewFile()) {
                        System.out.println("File overwrite: " + file.getName());
                    } else {
                        System.out.println("Could not overwrite existing file");
                    }
                } else {
                    System.out.println("Could not delete existing file");
                }
            } else {
                //try create new file
                if (file.createNewFile()) {
                    System.out.println("File created: " + file.getName());
                } else {
                    System.out.println("Could not create file");
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred creating the file.");
            e.printStackTrace();
        }
    }

}