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


class Crime { 
	String type;
	String date; 
	String time; 
	String location; 
	String summary; 
	Crime(String type, String date, String time, String location, String summary) {
		this.type = type; 
		this.date = date; 
		this.time = time; 
		this.location = location;
		this.summary = summary; 
	}
}

public class Scraper {

	static final String DEFAULT_URL = "https://police.wustl.edu/clerylogsandreports/Daily-Crime-Log/Documents/";
	private static WebClient client;

	public static String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	public static String[] monthsAb = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
	
	

	public static List<Crime> scrape(int month, int year) {
		client = new WebClient();

		String searchUrl = DEFAULT_URL + months[month] + year + ".html";

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
			for (int i = 2; i < lines.length; i++) { 
				if (lines[i].indexOf('?') == -1 && !lines[i].isEmpty()) {
					filteredList.add(lines[i]);
				}
			}
			
			LinkedList<String> splitList = new LinkedList<String>(); 
			int begin = 0; 
			
			for (int i = 0; i < filteredList.size(); i++) { 
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
						splitList.add(combined);
						begin = i; 
					}
				}
			}
			
			List<Crime> crimes = new LinkedList<Crime>(); 
			
			for (int i = 0; i < splitList.size(); i++) { 
				String paragraph = splitList.get(i);
				String type = paragraph.substring(0, paragraph.indexOf(" ")); 
				String date = paragraph.substring(paragraph.indexOf(monthsAb[month]), paragraph.indexOf(""+year)+4);
				String afterYear = paragraph.substring(paragraph.indexOf(year+""));
				String time = afterYear.substring(afterYear.indexOf(":")-2, afterYear.indexOf(":") + 3);
				String afterLocation = paragraph.substring(paragraph.indexOf("Location: ")+10); 
				String location;
				if (afterLocation.contains("(")) { 
					location = afterLocation.substring(0, afterLocation.indexOf("("));
				} else { 
					location = afterLocation.substring(0, afterLocation.indexOf("Summary"));
				}
				
				String summary = paragraph.substring(paragraph.indexOf("Summary: ") + 9);
				Crime crime = new Crime(type, date, time, location, summary); 
				crimes.add(crime);
			}
			
			return crimes; 
			
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
		return null;

	}

	public static void recordCrimes(List<Crime> crimes) { 
		for (int i = 0; i < crimes.size(); i++) { 
			
			
		}
	}
	

	public static void main(String[] args) { 
		
		
		
		scrape(4, 2003); // month is index 1 down. Ex: March is denoted as 2 
	}


}