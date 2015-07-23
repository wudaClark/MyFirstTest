
public class Title {
	
	public int lineNo;
	public String rawText;
	public String linkTitle;
	public String title;
	public double similarity;
	
	Title() {
		this.lineNo = -1;
		this.title = null;
		this.linkTitle = null;
		this.rawText = null;
		this.similarity = -1;
	}
	
	public String toString() {
		return lineNo + ": " + title + " <= " + rawText + " linkTitle: " + linkTitle + (this.similarity >= 0 ? " similarity: "+this.similarity : "");
	}
}
