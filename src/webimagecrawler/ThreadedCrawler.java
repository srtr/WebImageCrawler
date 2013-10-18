package webimagecrawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ThreadedCrawler implements Runnable {
	//Static variables; single version for all threads
	final static int THREAD_LIMIT = 50;
	private static int thread_count = 1;
	private static String searchKeyword;
	//public static ArrayList crawledURL = new ArrayList();
	//public static ArrayList toCrawlURL = new ArrayList();
	//public static ArrayList imageURL = new ArrayList();

	public static ArrayList<Website> searchResults = new ArrayList<Website>();
	public static ArrayList<String> toCrawlURL = new ArrayList<String>();
	public static ArrayList<String> crawledURL = new ArrayList<String>();
	public static ArrayList<String> crawlingURL = new ArrayList<String>();

	//Variables owned by each thread
	private int threadId;
	SynchManager synchMngr;


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
			Thread continue_thread = new Thread(new ThreadedCrawler(update_return_thread_size(0)));
			continue_thread.start();
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
		searchKeyword = searchWord;

		File loadUrls = new File("loadURLS.txt");
		if(loadUrls.exists()){
			Scanner urlSet = new Scanner(loadUrls);
			urlSet.useDelimiter("/n");
			while (urlSet.hasNext()){
				String url = urlSet.nextLine().toString().toLowerCase();
				if(!(verifyUrl(url) == null))
					synchMngr.toCrawlList_add(toCrawlURL,url);
			}
			urlSet.close();
		}
		else
			System.out.println("Cannot find loadURLS file.");

		int n = toCrawlURL.size();
		for(int i = 0; i < n ; i++)
			System.out.println(toCrawlURL.get( i ));

	}

	//ThreadedCrawler ctor
	public ThreadedCrawler(int tId){
		threadId = tId;
		synchMngr = new SynchManager();
		log("Thread created");
	}

	public void searchImg(String keyword, Document parseHtml) {

		String ss[] = keyword.toLowerCase().split(" ");			//in case its multiple search word
		Elements img = parseHtml.getElementsByTag("img");
		Elements a_href = parseHtml.getElementsByTag("a");

		for (Element el : img) 
		{
			int cnt = 0, i = 0;
			while(i < ss.length)								//iterate through each keyword
			{
				if(el.attr("alt").toLowerCase().contains(ss[i]))
				{
					cnt++;										//if found
				}
				i++;
			}
			if(cnt > 0)											//Save to searchResults
			{
				Website relevant = new Website();
				relevant.setName(el);
				relevant.setWeight(cnt);
				synchMngr.imageList_add(searchResults, relevant);
			}
		}

		int i=0;
		for (Element al : a_href)								//Save to urlLinks for future access
		{
			if(i>0){
				synchMngr.toCrawlList_add(toCrawlURL, al.attr("abs:href").toString());
			}
			i++;
		}
	}


	//Function dealing with crawling
	public void process_thread() throws Exception
	{
		while(synchMngr.arrayList_size(searchResults) <= 10 && synchMngr.arrayList_size(toCrawlURL) > 0)
		{
			String getUrl = synchMngr.retrieve_URL_toCrawl(toCrawlURL);	//Retrieves and removes the url from toCrawlURL list

			boolean urlUnique = synchMngr.crawlingList_add_update(crawlingURL,crawledURL,getUrl,1);

			if(urlUnique)			//If url is unique i.e not present in crawling or crawled Lists then continue processing else retrieve new url
			{
				Document doc = null;
				Connection.Response response = null;
				try {						
					//test the connection first
					response = Jsoup.connect(getUrl)	
							.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
							.timeout(10000)
							.execute();
					doc = response.parse();
					synchMngr.crawlingList_add_update(crawlingURL,crawledURL,getUrl,0); //Removes url from crawlinList and adds it to crawledList
					searchImg(searchKeyword, doc);//begin searching through html code
				}
				catch (IOException e) {
					System.out.println("received error code : " + e);		
				}
			}

		//	log("Create Call Invoked");
			create_thread();	
		}
		//log("exiting thread"+threadId);
		update_return_thread_size(2);
		Collections.sort(searchResults, new CustomComparator());
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

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		ThreadedCrawler initiateCrawler =  new ThreadedCrawler(1);
		Thread start_thread = new Thread(initiateCrawler);

		System.out.println("Search Keyword: ");
		Scanner searchWord = new Scanner(System.in); 
		initiateCrawler.loadState(searchWord.nextLine().toString().toLowerCase());
		searchWord.close();

		start_thread.start();
	}
}