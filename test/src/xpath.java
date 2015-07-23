import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;


public class xpath {

	public static void main(String [] args) throws IOException {
		//http://roll.news.sina.com.cn/news/gnxw/gdxw1/index.shtml
		//http://tieba.baidu.com/f?kw=4&ie=utf-8
		URL listUrl = new URL("http://tieba.baidu.com/f?kw=4&ie=utf-8");
		cmp(listUrl, null, null, null);
	}
	public static void cmp(URL url, String txt, URL url2, String txt2) throws IOException{
		Document doc = Jsoup.parse(url, 8000);
		Document doc2 = null;
		if (url2 == null) {
			String sUrl = nextPageUrl(doc);
			URL url3 = new URL(sUrl);
			doc2 = Jsoup.parse(url3, 8000);
		} else {
			doc2 = Jsoup.parse(url2, 8000);
		}
		Element e = doc.body();
		Element e2 = doc2.body();
		e.getElementsByTag("script").remove();
		e.getElementsByTag("style").remove();
		e.getElementsByTag("link").remove();
		e2.getElementsByTag("script").remove();
		e2.getElementsByTag("style").remove();
		e2.getElementsByTag("link").remove();
		Elements links = cmpList(e, e2);
		if (url2 == null) {
			if (links.size() >= 2) {
				Element link = links.get(0);
				Element link2 = links.get(1);
				URL cUrl = new URL(link.attr("abs:href"));
				URL cUrl2 = new URL(link2.attr("abs:href"));
				cmp(cUrl, link.text().trim(), cUrl2, link2.text().trim());
			}
		} else {
			parseContent(e, url, txt, e2, url2, txt2);
		}
	}
	public static String nextPageUrl(Document doc) {
		System.out.println("-------------------nextPageUrl-------------------");
		Elements links = doc.getElementsByTag("a");
		int i = 0;
		for (Element a : links) {
			String txt = a.text().trim();
			if (txt.length()<=5)
				System.out.println(i++ +" "+ txt+" "+a.attr("abs:href"));
			if (txt.equalsIgnoreCase("2") || txt.equalsIgnoreCase("下一页")) {
				System.out.println("=========================");
				return a.attr("abs:href");
			}
		}
		return null;
	}
	public static Elements cmpList(Element body, Element body2) {
//        Matcher matcher = pattern.matcher(console.readLine("Enter input string to search: "));  
//        boolean found = false;  
//        while (matcher.find()) {
//            matcher.group(), matcher.start(), matcher.end();  
//            found = true;
//        }
        
		Elements links = body.getElementsByTag("a");
		Elements links2 = body2.getElementsByTag("a");

		links = cleanLinks(links);
		links2 = cleanLinks(links2);
		
		removeDupLinks(links, links2);
		
		HashMap<String, Elements> pathElements = new HashMap<String, Elements>();
		HashMap<String, Integer> pathScore = new HashMap<String, Integer>();
		for (Element link : links) {
			String path = tagPath(link);
			String txt = link.text().trim();
			int len = txt.length();
			Integer score = len > 8 ? 2 : (len >= 5 ? 1 : 0);
			if (pathElements.containsKey(path)) {
				pathElements.get(path).add(link);
				pathScore.put(path, pathScore.get(path)+score);
			} else {
				Elements es = new Elements();
				es.add(link);
				pathElements.put(path, es);
				pathScore.put(path, score);
			}
		}
		String pathWithMaxScore = null;
		Integer maxScore = 0;
		System.out.println("-------------------path score -------------------");
		for (Map.Entry<String, Integer> e : pathScore.entrySet()) {
			System.out.println(e.getKey()+": "+e.getValue());
			if (e.getValue() > maxScore) {
				maxScore = e.getValue();
				pathWithMaxScore = e.getKey();
			}
		}
		System.out.println("-------------------links -------------------");
		int i = 0;
		if (pathWithMaxScore != null) {
			for (Element link : pathElements.get(pathWithMaxScore)) {
				System.out.println(i++ +" "+link.text()+" "+link.attr("abs:href"));
			}
			return pathElements.get(pathWithMaxScore);
		}
		return null;
	}
	static Pattern patternNav = Pattern.compile("^.{0,2}[上下前后][一0-9]*页.{0,2}$");
	public static Elements cleanLinks(Elements links) {
		for (int i=0; i<links.size(); ++i) {
			Element link = links.get(i);
			String txt = link.text().trim();
			if (txt.length() <= 2 || patternNav.matcher(txt).find()) {
				links.get(i).remove();
				links.remove(i--);
				continue;
			}
			String url = link.attr("abs:href");
			boolean found = false;
			for (int j=i+1; j<links.size(); ++j) {
				Element linkj = links.get(j);
//				String txtj = linkj.text().trim();
				String urlj = linkj.attr("abs:href");
				if (url.equalsIgnoreCase(urlj)) {
					links.get(j).remove();
					links.remove(j--);
					found = true;
				}
			}
			if (found) {
				links.get(i).remove();
				links.remove(i--);
			}
		}
		return links;
	}
	public static boolean isEqualTrees(Element e1, Element e2) {
		Elements c1 = e1.children();
		Elements c2 = e2.children();
		if (c1.size() != c2.size()) {
			return false;
		}
		for (int i=0; i<c1.size(); ++i) {
			if (isEqualTrees(c1.get(i), c2.get(i))) {
				c1.remove(i);
				c2.remove(i);
				continue;
			} else {
				return false;
			}
		}
		if (e1.tag() != e2.tag()) {
			return false;
		}
		String tagName = e1.tagName();
		if (tagName.equalsIgnoreCase("a")) {
			if (e1.attr("abs:href").equalsIgnoreCase(e2.attr("abs:href"))) {
				return true;
			} else {
				return false;
			}
		} else if (! e1.text().equals(e2.text())) {
			return false;
		}
		return true;
	}
	public static String tagPath(Element e) {
		Elements parents = e.parents();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<parents.size(); ++i) {
			Element p = parents.get(i);
			String tn = p.tagName();
			if (tn.equalsIgnoreCase("body")) {
				break;
			}
			sb.insert(0, sb.length()==0 ? tn : tn+" > ");
		}
		return sb.toString();
	}
	public static String xPath(Element e) {
		Elements parents = e.parents();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<parents.size(); ++i) {
			Element p = parents.get(i);
			String tn = p.tagName();
			if (tn.equalsIgnoreCase("body")) {
				break;
			}
			Set<String> classNames = p.classNames();
			String sel = null;
			if (classNames.size() == 1) {
				sel = tn+"."+classNames.toArray()[0];
			} else {
				sel = tn;
			}
			sb.insert(0, sb.length()==0 ? sel : sel+" > ");
		}
		return sb.toString();
	}
	public static Elements removeDupLinks(Elements links, Elements links2) {
		int i = 0, j = 0;
		for (i=0; i<links.size(); ++i) {
			Element link = links.get(i);
			String url = link.attr("abs:href");
			boolean found = false;
			for (j=0; j<links2.size(); ++j) {
				Element link2 = links2.get(j);
				String url2 = link2.attr("abs:href");
				if (url.equalsIgnoreCase(url2)) {
					links2.get(j).remove();
					links2.remove(j--);
					found = true;
					continue;
				}
			}
			if (found) {
				links.get(i).remove();
				links.remove(i--);
				for (int k=i+1; k<links.size(); ++k) {
					if (links.get(k).attr("abs:href").equalsIgnoreCase(url)) {
						links.get(k).remove();
						links.remove(k--);
					}
				}
			}
		}
		System.out.println("-------------------links-------------------");
		for(i=0; i<links.size(); ++i) {
			Element link = links.get(i);
			System.out.println(i+" "+link.text()+" "+link.attr("abs:href") + "\n" + tagPath(link));
		}
		System.out.println("-------------------links2-------------------");
		for(i=0; i<links2.size(); ++i) {
			Element link = links2.get(i);
			System.out.println(i+" "+link.text()+" "+link.attr("abs:href"));
		}
		return links;
	}
	public static HashMap<String, String> parseContent(
			Element body, URL url, String linkTitle, 
			Element body2, URL url2, String linkTitle2) {

		Elements links = body.getElementsByTag("a");
		Elements links2 = body2.getElementsByTag("a");

//		links = cleanLinks(links);
//		links2 = cleanLinks(links2);
		
		removeDupLinks(links, links2);

		System.out.println("-------------------body-------------------");
		ArrayList<String> lines = textLines(body);
		Title title = parseTitle(null, linkTitle, lines);
		System.out.println("Title: "+title);
		for (int i=0; i<lines.size(); ++i) {
			String line = lines.get(i);
			System.out.println(i + ": " + line + ": " + cn.com.deepdata.deepradar.client.StringUtil.EDSimilarity(linkTitle, line));
		}
		
		System.out.println("-------------------body2-------------------");
		ArrayList<String> lines2 = textLines(body2);
		Title title2 = parseTitle(null, linkTitle2, lines2);
		System.out.println("Title: "+title2);
		for (int i=0; i<lines2.size(); ++i) {
			String line = lines2.get(i);
			System.out.println(i + ": " + line + ": " + cn.com.deepdata.deepradar.client.StringUtil.EDSimilarity(linkTitle2, line));
		}
		return null;
	}
	public static Title parseTitle(String metaTitle, String linkTitle, ArrayList<String> lines) {
		Title title = new Title();
		for(int i=0; i<lines.size(); ++i) {
			String line = lines.get(i);
			double s1 = cn.com.deepdata.deepradar.client.StringUtil.EDSimilarity(linkTitle, line);
			//double s2 = cn.com.deepdata.deepradar.client.StringUtil.EDSimilarity(metaTitle, line);
			if (title.lineNo == -1 || title.similarity < s1) {
				title.lineNo = i;
				title.rawText = line;
				title.linkTitle = linkTitle;
				title.title = linkTitle;
				title.similarity = s1;
			}
		}
		return title;
	}
	public static String brPlaceholder = "```BR```";
	public static ArrayList<String> textLines(Element body) {
		Pattern patternBR = Pattern.compile("<\\s*[bB][rR]\\s*/?\\s*>", 0);
		String html = body.html();
		Matcher m = patternBR.matcher(html);
		if (m.find()) {
			body.html(m.replaceAll(brPlaceholder));
		}
		
		Elements es = body.select("div, p, h, dir");
		for (Element e : es) {
			e.appendText(brPlaceholder);
		}
		String bodyText = body.text();
		String[] rawLines = bodyText.split(brPlaceholder);
		ArrayList<String> lines = new ArrayList<String>();
		for(String line : rawLines) {
			String s = line.trim();
			if (s.length() > 0) {
				lines.add(s);
			}
		}
		return lines;
	}
}
