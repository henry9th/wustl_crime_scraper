package wustl_crime_scraper;

import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DynamicScraper {

	static final String DEFAULT_URL = "https://police.wustl.edu/Pages/Daily-Crime-Log-Archive.aspx";
	private static WebClient client;
	static List<Crime> gCrimes = new LinkedList<Crime>();
	
	public static void scrape(HtmlPage page, int year) {
		System.out.print("NEW LIST: " );
		for (int i = 0; i < gCrimes.size(); i++) { 
			System.out.print(gCrimes.get(i).date + "|" );
		}
		System.out.println(); 
		String searchTag = ""; 
		if (year == 2019) { 
			searchTag = "WebPartWPQ3"; 
		} else if (year == 2018) { 
			searchTag = "WebPartWPQ4"; 
		} else if (year == 2017) { 
			searchTag = "WebPartWPQ5"; 
		} else if (year == 2016) { 
			searchTag = "WebPartWPQ6"; 
		} else if (year == 2015) { 
			searchTag = "WebPartWPQ7"; 
		} else if (year == 2014) { 
			searchTag = "WebPartWPQ8"; 
		} else if (year == 2013) { 
			searchTag = "WebPartWPQ9"; 
		}


		LinkedList<Crime> crimes = new LinkedList<Crime>(); 

		//client.setAjaxController(new NicelyResynchronizingAjaxController());
		client.setAjaxController(new AjaxController(){
			@Override
			public boolean processSynchron(HtmlPage page, WebRequest request, boolean async)
			{
				return true;
			}
		});

		final HtmlDivision div = page.getHtmlElementById(searchTag);
		final DomNode inside = div.getChildNodes().get(0); 
		final List<DomNode> cases = inside.getChildNodes();
		boolean keepGoing = false; 
		for (int i = 0; i < cases.size(); i++) { 
			DomNodeList<DomNode> specifications = cases.get(i).getFirstChild().getChildNodes();
			String details = cases.get(i).getTextContent();
			// ORDER: date, time, type, loc
			int orderCount = 0; 
			String time = ""; 
			String date = ""; 
			String type = ""; 
			String location = ""; 
			String summary = ""; 

			if (details.indexOf("Occurred:") != -1) { 
				String afterOccured = details.substring(details.indexOf("Occurred:") + 9);
				int indexA = afterOccured.indexOf("S");
				int indexB = afterOccured.indexOf(" ");
				if (indexA < indexB && indexA >=0) { 
					date = afterOccured.substring(0, indexA);
				} else if (indexB <= indexA && indexB >= 0) { 
					date = afterOccured.substring(0, indexB);
				}
			}


			String afterSynopsis = details.substring(details.indexOf("Synopsis:")+9);
			summary = afterSynopsis.substring(0, afterSynopsis.indexOf("Disposition"));
			// System.out.println(details); 

			for (int j = 0; j < specifications.size(); j++) { 
				DomNode spec = specifications.get(j);

				if (!spec.asText().isEmpty()) { 
					if (orderCount == 1) { 
						time = spec.asText();
					} else if (orderCount == 2) { 
						type = spec.asText();
					} else if (orderCount == 3) { 
						location = spec.asText();
					}
					orderCount++; 
				}
			}
			Crime crime = new Crime(type, date, time, location, summary); 
			System.out.println("crime: " +crime.date + " : " + crime.type + " : " + crime.location + " : " +crime.time + " : " + crime.summary);

			if (gCrimes.contains(crime)) { 
				System.out.println("REPEATED");
			} else { 
				gCrimes.add(crime);
				keepGoing = true; 
			}
		}

		if (keepGoing == true) { 
			// Recursive call for next page after javascript action
			List<?> anchors = page.getByXPath("//*[@id='" + searchTag + "']/table/tbody/tr/td/a");
			for (int i = 0; i < anchors.size(); i++) { 
				HtmlAnchor anchor = (HtmlAnchor) anchors.get(i);
				List<HtmlImage> anchorImages = (List<HtmlImage>) anchor.getByXPath("//*[@id=\"" + searchTag + "\"]/table/tbody/tr/td/a/img");

				for (int j = 0; j < anchorImages.size(); j++) { 
					HtmlImage anchorImage = anchorImages.get(j);
					if (anchorImage.getSrcAttribute().contains("next.gif")) { 
						System.out.println("SHIT");
						HtmlPage nextPage; 
						try {
							HtmlAnchor nextAnchor = (HtmlAnchor) anchors.get(anchors.size()-1);
							nextPage = nextAnchor.click();
							scrape(nextPage, year);
							break;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
				}


			}
		}
	}

	public static void main(String[] args) {
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

		HtmlPage page;
		client = new WebClient();
		client.setThrowExceptionOnFailingStatusCode(false);
		client.setThrowExceptionOnScriptError(false);

		try {
			page = client.getPage(DEFAULT_URL);
			for (int i = 2013; i <= 2019; i++) { 
				scrape(page, i);
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

		for (int i = 0; i < gCrimes.size(); i++) { 
			System.out.println(i + " : " + gCrimes.get(i).date);
			Crime crime = gCrimes.get(i);
			row = sheet.createRow(rowCount); 
			rowCount++;
			row.createCell(0).setCellValue(crime.date);
			row.createCell(1).setCellValue(crime.time);
			row.createCell(2).setCellValue(crime.location);
			row.createCell(3).setCellValue(crime.type);
			row.createCell(4).setCellValue(crime.summary);
			row.createCell(5).setCellValue("false");
		}

		try { 
			// this Writes the workbook gfgcontribute 
			FileOutputStream out = new FileOutputStream(new File("newCrimes.xlsx")); 
			workbook.write(out); 
			out.close(); 
			System.out.println("newCrimes.xlsx written successfully on disk.");
		} 
		catch (Exception e) { 
			e.printStackTrace(); 
		} 

	}

}
