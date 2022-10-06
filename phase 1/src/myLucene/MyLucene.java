package myLucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import utils.IO;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class MyLucene {
    //pre-process
    public MyLucene(int k, String title, Analyzer analyzer, Similarity similarity) throws Exception {

        String txtfile = "docs//IR2022//documents.txt";
        String qfile = "docs//IR2022//queries.txt";
        String indexLocation = ("index"); //define were to store the index


        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexLocation + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexLocation));

            // configure IndexWriter
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setSimilarity(similarity);

            // Create a new index in the directory, removing any
            // previously indexed documents:
            iwc.setOpenMode(OpenMode.CREATE);

            // create the IndexWriter with the configuration as above
            IndexWriter indexWriter = new IndexWriter(dir, iwc);

            // parse txt document using TXT parser and index it
            List<MyDoc> docs = TxtParser.parse(txtfile);
            for (MyDoc doc : docs) {
                indexDoc(indexWriter, doc);
            }

            indexWriter.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

            List<MyDoc> queries = TxtParser.parse(qfile);

            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
            IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.

            IO.CreateFile("myresults"+k+title+".txt");

            TopDocs results;
            ScoreDoc[] hits;
            long numTotalHits;

            String qstring;
            Query q;
            int qnum = 0;
            while (queries.size() > qnum) {
                qstring = queries.get(qnum).getContent().replaceAll("\n", " ") //turn eol into spaces
                        .replaceAll("\\\\n", " ") //to replace the "\n" (not end of line character)
                        .replaceAll("\t", " ") //replace tabs with spaces
                        .replaceAll(" {2,}", " "); //turn two or more consecutive spaces into one

                //System.out.println("q" + (qnum+1) + "= " + qstring); //print query for reference

                q = new QueryParser("contents", analyzer).parse(QueryParser.escape(qstring));
                System.out.println("Searching for: " + q.toString("contents"));

                // 3. search
                results = indexSearcher.search(q, k+1);
                hits = results.scoreDocs;
                numTotalHits = results.totalHits;
                System.out.println(numTotalHits + " total matching documents");


                // 4. display results
                System.out.println("Found " + hits.length + " hits.");
                try {
                    FileWriter myWriter = new FileWriter("docs//trec_eval//myresults"+k+title+".txt",true);
                    //display results
                    for (int i = 1; i < hits.length; i++) { //ignore first hit
                        Document hitDoc = indexSearcher.doc(hits[i].doc);

                        //print scores
                        //System.out.println("\tScore " + hits[i].score + "\tdocid=" + hitDoc.get("docid") + "\ttitle=" + hitDoc.get("title"));

                        //write results in file
                        //guery_id    Q0    doc_id    rank    score    ir_method
                        myWriter.write(queries.get(qnum).getDocid().toUpperCase()+"\tQ0\t" + hitDoc.get("docid") + "\t0\t" + hits[i].score + "\tIR"+title+"\n");
                    }
                    myWriter.close();
                } catch (IOException e) {
                    System.out.println("An error occurred while trying to write file.");
                    e.printStackTrace();
                }

                qnum++; //next query
            }

            // searcher can only be closed when there
            // is no need to access the documents any more.
            //Close indexReader
            indexReader.close();

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    //indexer
    private void indexDoc(IndexWriter indexWriter, MyDoc mydoc) {

        try {

            // make a new, empty document
            Document doc = new Document();

            // create the fields of the document and add them to the document
            StoredField docid = new StoredField("docid", mydoc.getDocid());
            doc.add(docid);
            StoredField title = new StoredField("title", mydoc.getTitle());
            doc.add(title);
            StoredField content = new StoredField("content", mydoc.getContent());
            doc.add(content);
            String fullSearchableText = mydoc.getTitle() + " " + mydoc.getContent();
            TextField contents = new TextField("contents", fullSearchableText, Field.Store.NO);
            doc.add(contents);

            if (indexWriter.getConfig().getOpenMode() == OpenMode.CREATE) {
                // New index, so we just add the document (no old document can be there):
                //System.out.println("adding " + mydoc);
                indexWriter.addDocument(doc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //initialize preprocessor and indexer
    public static void main(String[] args) {
        try {
            // define which analyzer to use for the normalization of documents
            Analyzer analyzer;
            // define retrieval model
            Similarity similarity;
            String name;

            analyzer = new EnglishAnalyzer();
            similarity = new BM25Similarity();
            name = "bm25en";
            MyLucene myLucene20 = new MyLucene(20, name ,analyzer, similarity);
            MyLucene myLucene30 = new MyLucene(30, name ,analyzer, similarity);
            MyLucene myLucene50 = new MyLucene(50, name ,analyzer, similarity);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
