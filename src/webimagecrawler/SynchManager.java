package webimagecrawler;
import java.util.*;

// Class of SynchManager
public class SynchManager{
	final static int ARRAYLIST_SIZE = 250;
	private synchronized boolean arrayList_contain(ArrayList<?> _arrayList, String url)
	{
		// Check if url in present in the arrayList
		return _arrayList.contains(url);
	}
	
	public synchronized void imageList_add(ArrayList<Website> _imageList, Website url)
	{
		// Add url to imageList if it is not already present
		if(! _imageList.contains(url)){
			_imageList.add(url);
			System.out.println("added image: "+ url);
		}
	}

	public synchronized String retrieve_URL_toCrawl(ArrayList<?> _toCrawlList)
	{
		// Get URL at bottom of the list.
		String url = (String) _toCrawlList.iterator().next();

		// Remove URL from the To Crawl list.
		_toCrawlList.remove(url);

		return(url);
	}

	//Add url to the toCrawl list
	public synchronized void toCrawlList_add(ArrayList<String> _toCrawlList,String url)
	{
		
		if(!(arrayList_contain(_toCrawlList,url)) && _toCrawlList.size() < ARRAYLIST_SIZE)
			_toCrawlList.add(url);
	}

	//To ensure that threads process unique URL each time.
	public synchronized boolean crawlingList_add_update(ArrayList<String> _CrawlingList,ArrayList<String> _CrawledList,String url,int mode)
	{
		boolean result = false;
		//Mode 1: add url to list
		if(mode == 1){
			if(!(arrayList_contain(_CrawledList,url) || arrayList_contain(_CrawlingList,url))){
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

	public synchronized int arrayList_size(ArrayList<?> _arrayList)
	{
		return  _arrayList.size();
	}

}//Class SynchManager