package webimagecrawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

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
	private static String searchKeyword;

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

	//Loads intial set of URLs from file to ArratList and notes the search keyword
	public void loadState(String searchWord) throws FileNotFoundException{

		searchComplete = false;
		thread_count = 1;
		searchResults.clear();
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

	public void searchImg(String keyword, Document parseHtml, ArrayList<String> thread_tocrawlURL) {

		String ss[] = keyword.toLowerCase().split(" ");			//in case its multiple search word
		Elements img = parseHtml.getElementsByTag("img");
		Elements a_href = parseHtml.getElementsByTag("a");

		for (Element el : img) 
		{
			int cnt = 0;
			String arr[] = el.attr("alt").toLowerCase().split(" ");
			for (String k : ss)                                                                //iterate through each keyword
			{
				for (String st : arr)
				{
					if(st.equals(k))
					{
						cnt++;                                                                                //if found
					}
				}
			}
			if(cnt > 0)											//Save to searchResults
			{
				Website relevant = new Website();
				relevant.setName(el);
				relevant.setWeight(cnt);
				SynchManager.imageList_add(searchResults, relevant);
			}
		}

		int i=0;
		for (Element al : a_href)								//Save to urlLinks for future access
		{
			if(i>0){
				//synchMngr.toCrawlList_add(toCrawlURL, al.attr("abs:href").toString());
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

		FileWriter outFile = new FileWriter("ImageLinks.txt", false);  
		BufferedWriter outFile_stream = new BufferedWriter(outFile); 
		for (int k = 0; k < searchResults.size(); k++)  
			outFile_stream.write(searchResults.get(k).getName().attr("abs:src")+"\r\n");
		outFile_stream.close();
		outFile.close();

		FileWriter links_list = new FileWriter("traversed_links.txt",false);
		BufferedWriter links_list_stream = new BufferedWriter(links_list);
		//		links_list_stream.write("To crawl list \r\n");
		//		for (int k = 0; k < toCrawlURL.size(); k++)  
		//			links_list_stream.write(toCrawlURL.get(k)+"\r\n");
		links_list_stream.write("Crawled list \r\n");
		for (int k = 0; k < crawledURL.size(); k++)  
			links_list_stream.write(crawledURL.get(k)+"\r\n");       
		links_list_stream.close(); 
		links_list.close();

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
			//			System.out.print("Search Keyword: ");
			//			String keyword;
			//			Scanner searchWord = new Scanner(System.in);
			//			while(true)
			//			{
			//				keyword = searchWord.nextLine().toString().toLowerCase();
			//				if(!keyword.isEmpty())
			//					break;
			//
			//			}
			//			initiateCrawler.loadState(searchWord.nextLine().toString().toLowerCase());
			//			searchWord.close();
			initiateCrawler.loadState(keyword);
			start_thread.start();
		}

		@Override
		public void handle(HttpExchange httpObject) throws IOException {
			// TODO Auto-generated method stub

			try {
				StringBuffer browserUrl = new StringBuffer(httpObject.getRequestURI().toString().replaceAll("\\s+","").toLowerCase());//.replace("/guess/","").replaceAll("\\s+","").toLowerCase();

				System.out.println("url is "+ browserUrl);
				
				File htmlFile = new File("htmlLinksFile.html");
				byte[] bytes = Files.readAllBytes(htmlFile.toPath());

				
				if(browserUrl.toString().startsWith("/search/")){
					if(!browserUrl.toString().replace("/search/", "").toString().equals("tf-search-icon.png")){
						start_crawler(browserUrl.toString().replace("/search/", "").toString());

						while(!searchComplete){

						}
					}

					StringBuffer addImageLinks = new StringBuffer("");
					
					addImageLinks.append("<br>");
					for(int i=0;i<searchResults.size();i++)
						addImageLinks.append("<a href='"+  searchResults.get(i).getName().attr("abs:src") + "' target='_blank'>" + "<img class='thumbnail' src='" + searchResults.get(i).getName().attr("abs:src") + "' width='150' height='150'>" + "</a>&nbsp&nbsp"); 
					
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

				//	start_crawler();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		//			ThreadedCrawler initiateCrawler =  new ThreadedCrawler(1);
		//			Thread start_thread = new Thread(initiateCrawler);
		//
		//			System.out.print("Search Keyword: ");
		//			Scanner searchWord = new Scanner(System.in); 
		//			initiateCrawler.loadState(searchWord.nextLine().toString().toLowerCase());
		//			searchWord.close();
		//
		//			start_thread.start();

		HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}
}
