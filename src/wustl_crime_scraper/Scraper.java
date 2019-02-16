package wustl_crime_scraper;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.Vector;

import javax.xml.stream.events.Characters;


public class Scraper {

	static final String DEFAULT_URL = "https://police.wustl.edu/clerylogsandreports/Daily-Crime-Log/Documents/August2002.html";
	private static WebClient client;


	public static void scrape() {
		client = new WebClient();

		String searchUrl = DEFAULT_URL;

		HtmlPage page;
		try {
			page = client.getPage(searchUrl);

			Iterator<DomNode> it = page.getChildNodes().get(1).getDescendants().iterator(); 
			int count = 0; 
			String targetText = ""; 
			while(it.hasNext()) { 
				DomNode node = it.next(); 
				if (count == 4) { // should always be four? 
					targetText = node.asText();
					break;
				}
				count++;
			}
			
			String lines[] = targetText.split("\\r?\\n");
			LinkedList<String> filteredList = new LinkedList<String>(); 
			for (int i = 3; i < lines.length; i++) { 
				if (lines[i].indexOf('?') == -1 && !lines[i].isEmpty()) {
					filteredList.add(lines[i]);
				}
			}
			
			LinkedList<String> splitList = new LinkedList<String>(); 
			int begin = 0; 
			
			for (int i = 0; i < filteredList.size(); i++) { 
				System.out.println(filteredList.get(i));
				String line = filteredList.get(i); 
				if (Character.isUpperCase(line.charAt(0)) && Character.isUpperCase(line.charAt(1))) {
					if (i != 0) { 
						String combined = ""; 
						//System.out.println("BEGIN: " + begin);
						for (int j = begin; j < i; j++) { 
							//System.out.println(lines[j]);
							combined += (filteredList.get(j) + " ");
						}
						//System.out.println("END: " + (i-1));
						System.out.println("COMBINED: " + combined);
						splitList.add(combined);
						begin = i; 
					}
				}
			}
			
			for (int i = 0; i < splitList.size(); i++) { 
				System.out.println("BREAK : " + splitList.get(i));
			}
			

		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static void main(String[] args) { 
		scrape();
	}


}