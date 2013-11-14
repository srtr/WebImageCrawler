package webimagecrawler;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

// Class of SynchManager
public class SynchManager{
	final static int ARRAYLIST_SIZE = 2500;

	public static long elapsedTime=0;
	public static long[] response_time = new long[5];
	public static int time_ctr = 0;
	public static String[] image = new String[5];
	public static int image_ctr=0;

	private synchronized static boolean arrayList_contain(ArrayList<?> _arrayList, String url)
	{
		// Check if url in present in the arrayList
		return _arrayList.contains(url);
	}

	public synchronized static void reset_TimeVar(){
		image_ctr = 0;
		time_ctr = 0;
	}

	public synchronized static void imageList_add(ArrayList<Website> _imageList, Website url)
	{
		if(arrayList_size(_imageList) < 5){
			// Add url to imageList if it is not already present
			boolean present = false;
			for (int k = 0; k < _imageList.size(); k++)  {
				String parts[] = _imageList.get(k).imgUrl.split("/");
				String partsUrl[] = url.getName().attr("abs:src").split("/");

				//if( _imageList.get(k).getName().attr("abs:src").contains(url.getName().attr("abs:src"))){
				if( _imageList.get(k).imgUrl.contains(url.getName().attr("abs:src")) || parts[parts.length -1].equalsIgnoreCase(partsUrl[partsUrl.length - 1])){
					present = true;
					break;
				}
			}
			if(!present){
				url.setimgUrl(url.getName().attr("abs:src"));
				url.setpageUrl(url.pageUrl);
				_imageList.add(url);

				image[image_ctr]=url.getName().attr("abs:src");
				elapsedTime = (System.currentTimeMillis() - ThreadedCrawler.startTime);
				System.out.println("Server response time in milliseconds: " + elapsedTime);
				response_time[time_ctr]=elapsedTime;
				time_ctr++;
				image_ctr++;
				System.out.println("Added Image ("+ _imageList.size() +"): "+url.getName().attr("abs:src"));

				writeResponseTime(_imageList, image);
			}			
		}
	}

	public synchronized static String retrieve_URL_toCrawl(ArrayList<?> _toCrawlList)
	{
		if(_toCrawlList.size() > 0){
			String url = (String) _toCrawlList.get(0);
			_toCrawlList.remove(0);	
			return(url);
		}
		else
			return null;
	}

	//Add url to the toCrawl list
	public synchronized static void toCrawlList_add(ArrayList<String> _toCrawlList,String url)
	{

		if(!(arrayList_contain(_toCrawlList,url)) && _toCrawlList.size() < ARRAYLIST_SIZE)
			_toCrawlList.add(url);
	}

	//To ensure that threads process unique URL each time.
	public synchronized static boolean crawlingList_add_update(ArrayList<String> _CrawlingList,ArrayList<String> _CrawledList,String url,int mode)
	{
		boolean result = false;
		//Mode 1: add url to list
		if(mode == 1){
			if(!(arrayList_contain(_CrawledList,url) || arrayList_contain(_CrawlingList,url)) && url!= null){
				_CrawlingList.add(url);
				result = true;
			}
		}
		//Other Mode: remove url from crawlingList and add it to the crawled list
		else{
			_CrawlingList.remove(url);
			_CrawledList.add(url);
			result = true;
		}

		return result;

	}

	public synchronized static int arrayList_size(ArrayList<?> _arrayList)
	{
		return  _arrayList.size();
	}

	private static void writeResponseTime (ArrayList<Website> _imageList, String[] image)
	{
		FileWriter outFile;
		try {
			outFile = new FileWriter("Responsetime_"+ThreadedCrawler.searchKeyword+".txt", false);
			PrintWriter outFile_stream = new PrintWriter(outFile); 

			for (int k = 0; k < _imageList.size(); k++)  
			{
				outFile_stream.write("Image (" + k + "): " + image[k] + "\r\n");
				outFile_stream.print("Server response time in milliseconds: " + response_time[k] + "\r\n\r\n");
			}
			outFile_stream.close();
			outFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}


}//Class SynchManager