package myLucene;

import utils.IO;

import java.util.ArrayList;
import java.util.List;

public class TxtParser {

    public static List<MyDoc> parse(String file) throws Exception {
        try {
            //Parse txt file
            String txt_file = IO.ReadEntireFileIntoAString(file);
            String txt_str = txt_file.replaceAll("\r\n", "\n");
            String[] docs = txt_str.split(" /// \n"); //split on each ' /// \n'
            System.out.println("Read: " + docs.length + " docs from "+file);


            //Parse each document from the txt file
            List<MyDoc> parsed_docs = new ArrayList<MyDoc>();
            for (String doc : docs) {
                //System.out.println("query: "+doc); //print query for reference
                String[] adoc = doc.split("\n", 2); //split in two on the first change of line
                MyDoc mydoc;

                if (file.contains("queries")) {
                    if (adoc[0].length() < 3) { //if q_id less than 3 characters
                        adoc[0] = adoc[0].replace("Q", "Q0"); //add 0 between Q and digit
                    }
                    mydoc = new MyDoc(adoc[0], "", adoc[1]); //(docid, title, content)
                }
                else {
                    String[] cont = adoc[1].split(":", 2); //split in two on the first ':'
                    mydoc = new MyDoc(adoc[0].toUpperCase(),cont[0].toLowerCase(),cont[1].toLowerCase()); //(docid, title, content)
                }

                parsed_docs.add(mydoc);
            }

            return parsed_docs;
        } catch (Throwable err) {
            err.printStackTrace();
            return null;
        }

    }

}