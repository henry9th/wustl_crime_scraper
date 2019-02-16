package wustl_crime_scraper;


import java.io.File;
import java.io.FileOutputStream;
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
	@Override
    public boolean equals(Object o) { 
//		System.out.println("Crime Compare");
		Crime c = (Crime) o;
//		if (this.type != c.type) { 
//			System.out.print("type ");
//		}
//		if (this.date != c.date) { 
//			System.out.print("date ");
//		}
//		if (this.time != c.time) { 
//			System.out.print("time ");
//			
//		}
//		if (this.location != c.location) { 
//			System.out.print("location ");
//		}
//		if (this.summary != c.summary) { 
//			System.out.print("summary ");
//		}
		System.out.println(); 
		return (this.type == c.type && this.date == c.date && this.time == c.time && this.location == c.location && this.summary == c.summary); 
    } 
}

public class Scraper {

	static final String DEFAULT_URL = "https://police.wustl.edu/clerylogsandreports/Daily-Crime-Log/Documents/";
	private static WebClient client;

	public static String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	public static String[] monthsAb = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT,", "NOV", "DEC"};
	public static String[] monthsAbAlt = {"JAN.", "FEB.", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUG", "SEPT", "OCT", "NOV", "DEC"};
	public static String[] monthsAbAlt2 = {"JAN.", "FEB.", "MARCH", "APR.", "MAY", "JUNE", "JULY", "AUG", "SEPT", "Oct.", "NOV", "DEC"};

	public static int numTimeSkipped = 0;
	public static int numDateSkipped = 0;
	public static int numLocationSkipped = 0;
	public static int numSummarySkipped = 0;

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
				if (Character.isUpperCase(line.charAt(0)) && Character.isUpperCase(line.charAt(1)) && !line.contains("SUSPECT")) {
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
				String type = ""; 
				if (Character.isUpperCase(paragraph.substring(paragraph.indexOf(" ")).charAt(0)) && Character.isUpperCase(paragraph.substring(paragraph.indexOf(" ")).charAt(1))) { 
					type = paragraph.substring(0, paragraph.substring(paragraph.indexOf(" ")).indexOf(" "));
				} else {
					type = paragraph.substring(0, paragraph.indexOf(" ")); 
				}
	
				String date = ""; 
				String summary = ""; 
				String time = ""; 
				String location = ""; 
				
				if (paragraph.contains("No criminal incidents reported")  || paragraph.contains("No reports filed") || paragraph.contains("NO CRIMINAL INCIDENTS REPORTED") || paragraph.contains("No incidents reported") || paragraph.contains("No reports filled") || paragraph.contains("Drawn in error")) {
					continue;
				}
				
				try {
					if (paragraph.toLowerCase().indexOf(monthsAb[month].toLowerCase()) == -1) { 
						if (paragraph.toLowerCase().indexOf(monthsAbAlt[month].toLowerCase()) == -1) { 
							 date = paragraph.substring(paragraph.toLowerCase().indexOf(monthsAbAlt2[month].toLowerCase()), paragraph.indexOf(""+year)+4);
						}
						else {
						 date = paragraph.substring(paragraph.toLowerCase().indexOf(monthsAbAlt[month].toLowerCase()), paragraph.indexOf(""+year)+4);
						}
					} else {
						 date = paragraph.substring(paragraph.toLowerCase().indexOf(monthsAb[month].toLowerCase()), paragraph.indexOf(""+year)+4);
					}
				} catch (Exception e) { 
					System.out.println("date error");
					e.printStackTrace();
					System.out.println(paragraph.toLowerCase());
					System.out.println(monthsAb[month].toLowerCase());
					numDateSkipped++;
				}
				
				try { 
					String afterYear = paragraph.substring(paragraph.indexOf(year+""));
					time = afterYear.substring(afterYear.indexOf(":")-2, afterYear.indexOf(":") + 3);
				} catch (Exception e) { 
					System.out.println("time error");
					e.printStackTrace();
					System.out.println(paragraph);
					numTimeSkipped++;
				}
				
				try { 
					String afterLocation;
					if (paragraph.indexOf("Location: ") == -1) { 
						afterLocation = paragraph.substring(paragraph.indexOf("Location : ")+11);
					} else {
						afterLocation = paragraph.substring(paragraph.indexOf("Location: ")+10); 
					}
					if (afterLocation.contains("(")) { 
						location = afterLocation.substring(0, afterLocation.indexOf("("));
					} else { 
						if (afterLocation.indexOf("Summary") == -1){
							if (afterLocation.indexOf("Disposition") == -1) { 
								location = afterLocation.substring(0, afterLocation.indexOf("  "));
							} else {
								location = afterLocation.substring(0, afterLocation.indexOf("Disposition"));
							}
						} else {
							location = afterLocation.substring(0, afterLocation.indexOf("Summary"));
						}
					}
				} catch (Exception e) { 
					System.out.println("location error");
					e.printStackTrace();
					System.out.println(paragraph);
					numLocationSkipped++;
				}
				try { 
					summary = paragraph.substring(paragraph.indexOf("Summary: ") + 9);
				} catch (Exception e) { 
					System.out.println("summary error"); 
					System.out.println(paragraph);
					numSummarySkipped++;
				}
				
			
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


	public static void main(String[] args) { 
		XSSFWorkbook workbook = new XSSFWorkbook(); 
		XSSFSheet sheet = workbook.createSheet("crime"); 
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("date");
		row.createCell(1).setCellValue("time");
		row.createCell(2).setCellValue("location");
		row.createCell(3).setCellValue("type");
		row.createCell(4).setCellValue("description");
		row.createCell(5).setCellValue("gotLocation");

		int rowCount = 1; 

		for (int i = 6; i<12; i++) { 
			List<Crime> crimes = scrape(i, 2002);
			for (int j = 0; j < crimes.size(); j++) { 
				//System.out.println(i + " : " + j + " : " + crimes.size());
				Crime crime = crimes.get(j); 
				row = sheet.createRow(rowCount); 
				rowCount++;
				row.createCell(0).setCellValue(crime.date);
				row.createCell(1).setCellValue(crime.time);
				row.createCell(2).setCellValue(crime.location);
				row.createCell(3).setCellValue(crime.type);
				row.createCell(4).setCellValue(crime.summary);
				row.createCell(5).setCellValue("false");
			}
		}

		for (int i = 2003; i <= 2012; i++) { 
			for (int j = 0; j < 12; j++) { 
				List<Crime> crimes = scrape(j, i);
				for (int k = 0; k < crimes.size(); k++) { 
					Crime crime = crimes.get(k); 
					row = sheet.createRow(rowCount); 
					rowCount++;
					row.createCell(0).setCellValue(crime.date);
					row.createCell(1).setCellValue(crime.time);
					row.createCell(2).setCellValue(crime.location);
					row.createCell(3).setCellValue(crime.type);
					row.createCell(4).setCellValue(crime.summary);
					row.createCell(5).setCellValue("false");
				}

			}
		}

		try { 
			// this Writes the workbook gfgcontribute 
			FileOutputStream out = new FileOutputStream(new File("crimes.xlsx")); 
			workbook.write(out); 
			out.close(); 
			System.out.println("crimes.xlsx written successfully on disk. Date: " + numDateSkipped + " Time: " + numTimeSkipped + " Location: " + numLocationSkipped + " Summary: " + numSummarySkipped); 
		} 
		catch (Exception e) { 
			e.printStackTrace(); 
		} 

	}


}