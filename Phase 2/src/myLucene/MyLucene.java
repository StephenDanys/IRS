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
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import utils.IO;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

public class MyLucene {
    //pre-process

    /**
     * Initializer
     * @param title out file title
     * @param analyzer
     * @param similarity
     * @throws Exception
     */
    public MyLucene(String title, Analyzer analyzer, Similarity similarity) throws Exception {

        String indexLocation = ("index"); //define where to store the index
        makeIndex(analyzer, similarity, indexLocation);

        List<MyDoc> queries = TxtParser.parse("docs//IR2022//queries.txt");

        if (queries != null) {
            //2nd phase search
            searchMLT(indexLocation, queries, similarity, analyzer, 20);
            searchMLT(indexLocation, queries, similarity, analyzer, 30);
            searchMLT(indexLocation, queries, similarity, analyzer, 50);


            List<TopDocs> results20= new LinkedList<TopDocs>();
            List<TopDocs> results30= new LinkedList<TopDocs>();
            List<TopDocs> results50= new LinkedList<TopDocs>();

            List<TopDocs> temp20, temp30, temp50;
            TopDocs hits20, hits30, hits50, ftd20, ftd30, ftd50;

            for (MyDoc q:queries){
                TopDocs td20, td30, td50;
                hits20= searchSingleQuery(20, indexLocation, analyzer,q);
                hits30= searchSingleQuery(30, indexLocation, analyzer,q);
                hits50= searchSingleQuery(50, indexLocation, analyzer,q);

                temp20= singleMLT(hits20, indexLocation, analyzer, similarity);
                temp30= singleMLT(hits30, indexLocation, analyzer, similarity);
                temp50= singleMLT(hits50, indexLocation, analyzer, similarity);

                td20 = TopDocs.merge(21, temp20.toArray(new TopDocs[0]));
                td30 = TopDocs.merge(31, temp30.toArray(new TopDocs[0]));
                td50 = TopDocs.merge(51, temp50.toArray(new TopDocs[0]));

                ftd20 = removeDuplicates(td20);
                ftd30 = removeDuplicates(td30);
                ftd50 = removeDuplicates(td50);

                results20.add(ftd20);
                results30.add(ftd30);
                results50.add(ftd50);
            }
            writeResults( results20, 20, title+"_alt", indexLocation, queries);
            writeResults( results30, 30, title+"_alt", indexLocation, queries);
            writeResults( results50, 50, title+"_alt", indexLocation, queries);


        } else {
            System.out.println("Null Queries");
        }

    }

    /**
     * Used to remove duplicates from a TopDocs list
     * @param td TopDocs list
     * @return
     */
    private TopDocs removeDuplicates(TopDocs td) {
        Set<ScoreDoc> scoreDocs = new TreeSet<>(new Comparator<ScoreDoc>() {
            @Override
            public int compare(ScoreDoc o1, ScoreDoc o2) {
                return Integer.compare(o1.doc, o2.doc);
            }
        });
        float maxScore = Float.MIN_VALUE;
        for (int i = 0; i < td.scoreDocs.length; ++i) {
            final ScoreDoc[] scoreDocs1 = td.scoreDocs;
            scoreDocs.add(scoreDocs1[i]);
            if (scoreDocs1[i].score > maxScore) {
                maxScore = scoreDocs1[i].score;
            }
        }
        TopDocs filteredtd = new TopDocs(scoreDocs.size(), scoreDocs.toArray(new ScoreDoc[0]), maxScore);
        return filteredtd;
    }
    //indexer
    private void makeIndex(Analyzer analyzer, Similarity similarity, String indexLocation) throws Exception {
        String txtfile = "docs//IR2022//documents.txt";

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
            if (docs != null) {
                for (MyDoc doc : docs) {
                    indexDoc(indexWriter, doc);
                }
            } else {
                System.out.println("Null Documents");
            }

            indexWriter.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

    /**
     * Index writter
     * @param indexWriter
     * @param mydoc
     */
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
            TextField contents = new TextField("contents", fullSearchableText, Field.Store.YES);
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

    /**
     * Simple index search, used for 1 querry
     * @param k number of results
     * @param query
     * @return TopDocs list of results
     * @throws Exception
     */
    private TopDocs searchSingleQuery(int k, String indexLocation, Analyzer analyzer, MyDoc query) throws Exception{

        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
        IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.
        String qstring= query.getContent();
        Query q= new QueryParser("contents", analyzer).parse(QueryParser.escape(qstring));
        TopDocs qresults = indexSearcher.search(q, k + 1);

        return qresults;
    }

    /**
     * Used in phase 1
     * Runs a query through the index, a simple search
     *
     * @param k number of expected results
     * @param title out file name
     * @param queries list of querries
     * @throws Exception
     */
    private void searchQueries(int k, String title, String indexLocation, Analyzer analyzer, List<MyDoc> queries) throws Exception {

        List<TopDocs> allresults = new LinkedList<TopDocs>();

        try {

            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
            IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.

            TopDocs qresults;

            String qstring;
            Query q;
            int qnum = 0;

            while (queries.size() > qnum) {
                qstring = queries.get(qnum).getContent();
                q = new QueryParser("contents", analyzer).parse(QueryParser.escape(qstring));
                //System.out.println("Searching for: " + q.toString("contents"));

                // 3. search
                qresults = indexSearcher.search(q, k + 1);
                allresults.add(qresults);

                qnum++;
            }
            // searcher can only be closed when there
            // is no need to access the documents any more.
            //Close indexReader
            indexReader.close();

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
        writeResults(allresults, k, title, indexLocation, queries);
    }

    /**
     * Writes a file with all results
     * @param allresults List of TopDocs to be written
     * @param k number of docs to be writen
     * @param title file title
     * @throws Exception
     */
    private void writeResults(List<TopDocs> allresults, int k, String title, String indexLocation, List<MyDoc> queries) throws Exception{
        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
        IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.

        try {

            IO.CreateFile("myresults" + k + title + ".txt");

            TopDocs results;
            ScoreDoc[] hits;
            long numTotalHits;

            int qnum = 0;

            while (queries.size() > qnum) {

                results = allresults.get(qnum);

                hits = results.scoreDocs;
                numTotalHits = results.totalHits;

                // 4. save results
                try {
                    FileWriter myWriter = new FileWriter("docs//trec_eval//myresults" + k + title + ".txt", true);
                    //display results
                    for (int i = 1; i < hits.length; i++) { //ignore first hit
                        Document hitDoc = indexSearcher.doc(hits[i].doc);

                        //print scores
                        //System.out.println("\tScore " + hits[i].score + "\tdocid=" + hitDoc.get("docid") + "\ttitle=" + hitDoc.get("title"));

                        //write results in file
                        //guery_id    Q0    doc_id    rank    score    ir_method
                        myWriter.write(queries.get(qnum).getDocid().toUpperCase() + "\tQ0\t" + hitDoc.get("docid") + "\t0\t" + hits[i].score + "\tIR" + title + "\n");
                    }
                    myWriter.close();

                } catch (IOException e) {
                    System.out.println("An error occurred while trying to write file.");
                    e.printStackTrace();
                }
                qnum++;
            }
            // searcher can only be closed when there
            // is no need to access the documents any more.
            //Close indexReader
            indexReader.close();
            System.out.println("Wrote "+k+" results for each query");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    /**
     * a method to make a query search with the most important term of each result from hits
     * @param hits list with results from simple query search
     * @return a list with the Top documents found from all the hits
     * @throws Exception
     */
    private List<TopDocs> singleMLT(TopDocs hits, String indexLocation, Analyzer analyzer, Similarity similarity) throws Exception {
        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        MoreLikeThis moreLikeThis = new MoreLikeThis(indexReader);
        moreLikeThis.setAnalyzer(analyzer);

        String fieldName = "text";

        List<TopDocs> related=new LinkedList<TopDocs>();
        List<TopDocs> all=new LinkedList<TopDocs>();
        TopDocs temp;

        for (int i = 0; i < hits.scoreDocs.length; i++){
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = indexSearcher.doc(scoreDoc.doc);

            indexSearcher.setSimilarity(similarity);
            String text = doc.get("title") + doc.get("content");
            //System.out.println("klakklak "+text);
            Query simQuery = moreLikeThis.like("contents", new StringReader(text));

            temp = indexSearcher.search(simQuery, 6);
            for (ScoreDoc s : temp.scoreDocs) {
                Document document = indexReader.document(s.doc);
            }

            related.add(temp);
        }
        //System.out.println("size: "+related.size());
        return related;
    }

    /**
     * a method to find the most important terms and search with a new querry composed by them
     * @param queries list of querries
     * @param k number of results
     */
    private void searchMLT(String indexLocation, List<MyDoc> queries, Similarity similarity, Analyzer analyzer, int k) throws Exception{
        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
        IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.

        indexSearcher.setSimilarity(similarity);
        MoreLikeThis mlt = new MoreLikeThis(indexReader);
        mlt.setAnalyzer(analyzer);

        List<TopDocs> all = new LinkedList<>();

        TopDocs related;
        Query simQuery;
        int qnum=0;

        while (qnum < queries.size()) {
            simQuery = mlt.like("contents", new StringReader(queries.get(qnum).getContent())); // find more important terms and make a querry with them

            related = indexSearcher.search(simQuery, k); // run query
            all.add(related);
            qnum++;
        }

        writeResults(all, k, "querryMLT", indexLocation, queries);
    }
    //initialize preprocessor and indexer
    public static void main(String[] args) {
        try {
            // define which analyzer to use for the normalization of documents
            Analyzer analyzer;
            analyzer = new EnglishAnalyzer();
            //analyzer = new StandardAnalyzer();
            // define retrieval model
            Similarity similarity;
            //similarity = new BM25Similarity();
            similarity = new ClassicSimilarity();
            //give method a name
            String name = "Mlt_clen";
            MyLucene myLucene = new MyLucene(name, analyzer, similarity);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
