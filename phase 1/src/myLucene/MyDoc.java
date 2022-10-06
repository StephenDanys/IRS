package myLucene;

public class MyDoc {
    private String docid; //doc number after '///\n' or start of file
    private String title; //words before ':'
    private String content; //text after first ':'

    //constructor
    public MyDoc(String docid, String title, String content) {
        this.docid = docid;
        this.title = title;
        this.content = content;
    }

    @Override
    public String toString() {
        String ret = "MyDoc{"
                + "\n\tDocid: " + docid
                + "\n\tTitle: " + title
                + "\n\tContent: " + content;
        return ret + "\n}";
    }

    //getters setters
    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
