package webimagecrawler;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ThreadedCrawler implements Runnable{

	//Static variables; single version for all threads
	final static int THREAD_LIMIT = 250;
	private static int thread_count = 1;
	private static boolean searchComplete = false;
	public static String searchKeyword;
	public static long startTime=0;

	public static ArrayList<Website> searchResults = new ArrayList<Website>();
	public static ArrayList<String> toCrawlURL = new ArrayList<String>();
	public static ArrayList<String> crawledURL = new ArrayList<String>();
	public static ArrayList<String> crawlingURL = new ArrayList<String>();
	public static Thread []thread_objects = new Thread[THREAD_LIMIT+1]; //0th element is for class object in main

	//Variables owned by each thread
	private int threadId;

	//	public SynchManager synchMngr;

	private static synchronized int update_return_thread_size(int choice){
		if(choice == 1){
			if(thread_count <= THREAD_LIMIT)
				++thread_count;
			return 1;
		}
		else if(choice == 2){
			--thread_count;
			return 1;
		}
		else
			return thread_count;
	}

	//To create new threads provided THREAD_LIMIT has not been reached
	private static synchronized void create_thread(){
		if(update_return_thread_size(0) < THREAD_LIMIT ){
			update_return_thread_size(1);
			//Thread continue_thread = new Thread(new ThreadedCrawler(update_return_thread_size(0)));
			//continue_thread.start();
			thread_objects[update_return_thread_size(0)] = new Thread(new ThreadedCrawler(update_return_thread_size(0)));
			thread_objects[update_return_thread_size(0)].start();
		}
	}


	private void log(String message) {
		System.out.println("Message from " + threadId + ": " + message);
	}

	private void printResults() {

		for(Website tag : searchResults) {
			System.out.println(tag.getName().attr("alt") + " " + tag.getName().attr("abs:src"));
		}
	}

	// Helper function to Verify URL format.
	private URL verifyUrl(String url) 
	{
		// Only allow HTTP URLs.
		if (!(url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")))
			return null;

		URL verifiedUrl = null;
		try {
			verifiedUrl = new URL(url);
		} catch (Exception e) {

			return null;
		}
		return verifiedUrl;
	}

	private boolean verifyImageUrl(String url) 
	{
		//		boolean imageExists = false;
		//		try {
		//			HttpURLConnection.setFollowRedirects(false);
		//			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		//			con.setRequestMethod("HEAD");
		//			if(con.getResponseCode() == HttpURLConnection.HTTP_OK)
		//				if(con.getContentType().toString().toLowerCase().contains("image/")){
		//					imageExists = true;
		//					//System.out.println(con.getContentType().toString().toLowerCase());
		//				}
		//
		//			return imageExists;
		//
		//		}
		//		catch (Exception e) {
		//			//e.printStackTrace();
		//			return imageExists;
		//		}
		boolean imageExists = false;
		String[] imageTypes = {"ANI","BMP","CAL","FAX","GIF","IMG","JBG","JPE","JPEG","JPG","MAC","PBM","PCD","PCX","PCT","PGM","PNG","PPM","PSD","RAS","TGA","TIFF","WMF",};

		for(int i = 0; i< imageTypes.length; ++i)
			if(url.toUpperCase().endsWith(imageTypes[i])){
				imageExists = true;
				break;
			}
		return imageExists;
	}

	//Loads intial set of URLs from file to ArratList and notes the search keyword
	public void loadState(String searchWord) throws FileNotFoundException{

		searchComplete = false;
		thread_count = 1;
		//searchResults.clear();
		toCrawlURL.clear();
		crawledURL.clear();
		crawlingURL.clear();
		searchKeyword = searchWord;

		File loadUrls = new File("loadURLS.txt");
		if(loadUrls.exists()){
			Scanner urlSet = new Scanner(loadUrls);
			urlSet.useDelimiter("/n");
			while (urlSet.hasNext()){
				String url = urlSet.nextLine().toString().toLowerCase();
				if(!(verifyUrl(url) == null))
					SynchManager.toCrawlList_add(toCrawlURL,url);
			}
			urlSet.close();
		}
		else
			System.out.println("Cannot find loadURLS file.");

		int arr_size = SynchManager.arrayList_size(toCrawlURL);
		for(int i = 0; i < arr_size ; i++)
			System.out.println(toCrawlURL.get( i ));
	}

	//ThreadedCrawler ctor
	public ThreadedCrawler(int tId){
		threadId = tId;
		//synchMngr = new SynchManager();
		//	log("Thread created");
	}

	//	public void searchImg(String keyword, Document parseHtml, ArrayList<String> thread_tocrawlURL) {
	//
	//		String ss[] = keyword.toLowerCase().split(" ");			//in case its multiple search word
	//		Elements img = parseHtml.getElementsByTag("img");
	//		Elements a_href = parseHtml.getElementsByTag("a");
	//
	//		for (Element el : img) 
	//		{
	//			String imgSrc = el.attr("abs:src");
	//			boolean isImage = verifyImageUrl(imgSrc);
	//
	//			if(isImage == true)
	//			{
	//				int cnt = 0;
	//				String arr[] = el.attr("alt").toLowerCase().split(" ");
	//				for (String k : ss)                                                                //iterate through each keyword
	//				{
	//					for (String st : arr)
	//					{
	//						if(st.equals(k))
	//						{
	//							cnt++;                                                                                //if found
	//						}
	//					}
	//				}
	//				if(cnt > 0)											//Save to searchResults
	//				{
	//					//Search in img[src]
	//                	String urlPath[] = imgSrc.toLowerCase().split("/");
	//                	if (urlPath[urlPath.length-1].indexOf(".") > 0)
	//                	{
	//                		String fileName = urlPath[urlPath.length-1].substring(0, urlPath[urlPath.length-1].lastIndexOf("."));
	//                		Set<String>source = new HashSet<String>(Arrays.asList(fileName.split("-")));
	//                		for (String k : ss)                                                                //iterate through each keyword
	//                        {
	//                			if(source.contains(k))
	//                        		cnt++;                                                                     //if found
	//                        }
	//                	}
	//                	
	//					Website relevant = new Website();
	//					relevant.setName(el);
	//					relevant.setWeight(cnt);
	//					SynchManager.imageList_add(searchResults, relevant);
	//				}
	//			}
	//		}
	//
	//		int i=0;
	//		for (Element al : a_href)								//Save to urlLinks for future access
	//		{
	//			if(i>0){
	//				//synchMngr.toCrawlList_add(toCrawlURL, al.attr("abs:href").toString());
	//				if(!al.attr("href").toString().equals(null))
	//					thread_tocrawlURL.add(al.attr("abs:href").toString());
	//			}
	//			i++;
	//		}
	//	}


	public void searchImg(String keyword, Document parseHtml, ArrayList<String> thread_tocrawlURL) {

		int titleWeight = 0;
		String ss[] = keyword.toLowerCase().split(" ");										//in case its multiple search word

		Elements img = parseHtml.getElementsByTag("img");
		Elements a_href = parseHtml.getElementsByTag("a");
		String titleText = parseHtml.title();

		//Search in webpage title
		Set<String>title = new HashSet<String>(Arrays.asList(titleText.split(" ")));
		for (String k : ss)                                                                //iterate through each keyword
		{
			if(title.contains(k))
				titleWeight++;                                                                     //if found
		}

		for (Element el : img)
		{
			String imgSrc = el.attr("abs:src");
			boolean isImage = verifyImageUrl(imgSrc);
			//			boolean isImage = false;
			//			try{
			//				BufferedImage bi=ImageIO.read(new URL(imgSrc));
			//				if (bi!=null)
			//				{
			//					if(bi.getWidth() > 1 && bi.getHeight() > 1)
			//						isImage=true;
			//				}
			//			}catch(Exception e){
			//				{
			//					isImage=false;
			//				}
			//			}

			if(isImage == true)
			{   
				//Search in img[alt]
				int cnt = 0;
				Set<String> alt = new HashSet<String>(Arrays.asList(el.attr("alt").toLowerCase().split(" ")));
				
				for (String k : ss)                                                                //iterate through each keyword
				{
					if(alt.contains(k))
						cnt++;                                                                     //if found
				}
				
				if(el.attr("alt").toLowerCase().contains(keyword.toLowerCase()))
						++cnt;
				
				if(cnt > 0)                                                                                        //Save to searchResults
				{
					//Search in img[src]
					String urlPath[] = imgSrc.toLowerCase().split("/");
					if (urlPath[urlPath.length-1].indexOf(".") > 0)
					{
						String fileName = urlPath[urlPath.length-1].substring(0, urlPath[urlPath.length-1].lastIndexOf("."));
						Set<String>source = new HashSet<String>(Arrays.asList(fileName.split("-")));
						source.addAll(new HashSet<String>(Arrays.asList(fileName.split("_"))));
						for (String k : ss)                                                                //iterate through each keyword
						{
							if(source.contains(k))
								cnt++;                                                                     //if found
						}
					}
					cnt = cnt + titleWeight;

					Website relevant = new Website();
					relevant.setName(el);
					relevant.setWeight(cnt);
					SynchManager.imageList_add(searchResults, relevant);
				}
			}
		}

		int i=0;
		for (Element al : a_href)                                                                //Save to urlLinks for future access
		{
			if(i>0){
				//synchMngr.toCrawlList_add(toCrawlURL, al.attr("abs:href").toString());
				if(thread_tocrawlURL.size() < 2500)
				if(!al.attr("href").toString().equals(null))
						thread_tocrawlURL.add(al.attr("abs:href").toString());
			}
			i++;
		}
	}

	//Function dealing with crawling
	public void process_thread() throws Exception
	{
		ArrayList<String> toCrawlURL_thread = new ArrayList<String>();
		if(SynchManager.arrayList_size(toCrawlURL) > 0){
			String getUrl= SynchManager.retrieve_URL_toCrawl(toCrawlURL);	//Retrieves and removes the url from toCrawlURL list
			toCrawlURL_thread.add(getUrl);
		}
		while(SynchManager.arrayList_size(searchResults) < 10 && toCrawlURL_thread.size() > 0)
		{

			String getUrl = toCrawlURL_thread.get(0);
			toCrawlURL_thread.remove(0);

			boolean urlUnique = SynchManager.crawlingList_add_update(crawlingURL,crawledURL,getUrl,1);

			if(urlUnique && verifyUrl(getUrl)!= null)			//If url is unique i.e not present in crawling or crawled Lists then continue processing else retrieve new url
			{
				Document doc = null;
				Connection.Response response = null;
				startTime = System.currentTimeMillis();		//start timer for server response time
				try {						
					//test the connection first
					response = Jsoup.connect(getUrl).ignoreContentType(true)	
							.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
							.timeout(10000)
							.execute();
					doc = response.parse();
					searchImg(searchKeyword, doc, toCrawlURL_thread);//begin searching through html code				

				}
				catch (IOException e) {
					System.out.println("received error code : " + e);		
				}

				SynchManager.crawlingList_add_update(crawlingURL,crawledURL,getUrl,0); //Removes url from crawlingList and adds it to crawledList

			}

			if(SynchManager.arrayList_size(toCrawlURL) == 0 && !toCrawlURL_thread.isEmpty()){
				String thread_getUrl = (String) toCrawlURL_thread.get(0);
				toCrawlURL_thread.remove(0);
				SynchManager.toCrawlList_add(toCrawlURL, thread_getUrl);
			}

			//log("Create Call Invoked");
			create_thread();	
			Thread.sleep(100); //what is recommended time interval ?
		}
		//log("exiting thread"+threadId);
		update_return_thread_size(2); //Reduce thread_count to free space for generating new threads
		Collections.sort(searchResults, new CustomComparator());
		
		if(SynchManager.arrayList_size(searchResults) >= 10)
			searchComplete = true;

		FileWriter outFile = new FileWriter(searchKeyword+".txt", false);  
		BufferedWriter outFile_stream = new BufferedWriter(outFile); 
		for (int k = 0; k < searchResults.size(); k++)  
			outFile_stream.write(searchResults.get(k).weight+searchResults.get(k).imgUrl+"\r\n");
		outFile_stream.close();
		outFile.close();

		//		printResults();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			process_thread();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static class MyHandler implements HttpHandler {
		private static ThreadedCrawler initiateCrawler = new ThreadedCrawler(1);

		public void start_crawler(String keyword) throws Exception{
			Thread start_thread = new Thread(initiateCrawler);
			initiateCrawler.loadState(keyword);
			start_thread.start();
		}

		@Override
		public void handle(HttpExchange httpObject) throws IOException {
			// TODO Auto-generated method stub

			try {
				StringBuilder browserUrl = new StringBuilder(httpObject.getRequestURI().toString().replaceAll("\\s+","").toLowerCase());//.replace("/guess/","").replaceAll("\\s+","").toLowerCase();
				
				System.out.println("url is "+ browserUrl);

				File htmlFile = new File("htmlLinksFile.html");

				if(htmlFile.exists()){
					SynchManager.reset_TimeVar();
					byte[] bytes = Files.readAllBytes(htmlFile.toPath());
					String searchWord = browserUrl.toString().replace("/search/", "").toString().toLowerCase().replaceAll("%20", " ");
					System.out.println(searchWord);
					if(browserUrl.toString().startsWith("/search/")){
						if(!browserUrl.toString().replace("/search/", "").toString().equals("tf-search-icon.png")){

							try (BufferedReader br = new BufferedReader(new FileReader(searchWord+".txt")))
							{
								searchResults.clear();
								String sCurrentLine;
								initiateCrawler.loadState(searchWord);

								while ((sCurrentLine = br.readLine()) != null) {
									Website url = new Website();
									url.weight = Integer.parseInt(sCurrentLine.substring(0, 1));
									url.imgUrl = sCurrentLine.substring(1);
									//if(!(initiateCrawler.verifyUrl(url.imgUrl) == null))
									if(initiateCrawler.verifyImageUrl(url.imgUrl))
										searchResults.add(url);
								}

								if(searchResults.size() < 10){
									start_crawler(searchWord);
									while(!searchComplete){
									}
								}

							}

							catch (IOException e) {
								searchResults.clear();
								start_crawler(searchWord);
								while(!searchComplete){

								}
								//	e.printStackTrace();
							} 
						}


						StringBuilder addImageLinks = new StringBuilder("");
						addImageLinks.append("<br>");
						for(int i=searchResults.size()-1; i>=0; --i)
							//addImageLinks.append("<a href='"+  searchResults.get(i).getName().attr("abs:src") + "' target='_blank'>" + "<img class='thumbnail' src='" + searchResults.get(i).getName().attr("abs:src") + "' width='150' height='150'>" + "</a>&nbsp&nbsp");
							addImageLinks.append("<a href='"+  searchResults.get(i).imgUrl + "' target='_blank'>" + "<img class='thumbnail' src='" + searchResults.get(i).imgUrl + "' width='150' height='150'>" + "</a>&nbsp&nbsp");

						String htmlContent = (new String(bytes,"UTF-8")).replace("<div id='img_links'>", "<div id='img_links'>"+addImageLinks.toString());
						httpObject.sendResponseHeaders(200, htmlContent.length());     

						OutputStream os = httpObject.getResponseBody();
						os.write(htmlContent.getBytes());
						os.close();	
					}
					else{

						String htmlContent = new String(bytes,"UTF-8");
						httpObject.sendResponseHeaders(200, htmlContent.length());     

						OutputStream os = httpObject.getResponseBody();
						os.write(htmlContent.getBytes());
						os.close();			
					}
				}

				else{
					String htmlContent = new String("htmlLinksFile is missing");
					httpObject.sendResponseHeaders(404, htmlContent.length());     

					OutputStream os = httpObject.getResponseBody();
					os.write(htmlContent.getBytes());
					os.close();			
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
		System.out.println("Web Server started....");
	}
}