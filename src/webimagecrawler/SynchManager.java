package webimagecrawler;
import java.util.*;

// Class of SynchManager
public class SynchManager{
	final static int ARRAYLIST_SIZE = 2500;
	private synchronized static boolean arrayList_contain(ArrayList<?> _arrayList, String url)
	{
		// Check if url in present in the arrayList
		return _arrayList.contains(url);
	}

	public synchronized static void imageList_add(ArrayList<Website> _imageList, Website url)
	{
		if(arrayList_size(_imageList) < 10){
			// Add url to imageList if it is not already present
			boolean present = false;
			for (int k = 0; k < _imageList.size(); k++)  
				if( _imageList.get(k).getName().attr("abs:src").contains(url.getName().attr("abs:src"))){
					present = true;
					break;
				}
			if(!present){
				_imageList.add(url);
				System.out.println("Added Image ("+ _imageList.size() +"): "+url.getName().attr("abs:src"));
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

}//Class SynchManager