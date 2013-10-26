package webimagecrawler;

import org.jsoup.nodes.Element;

public class Website{       
	public Element name;
	public int weight;    
	public String imgUrl;

	public Element getName() {
		return name;
	}

	public void setName(Element name) {
		this.name = name;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}   
	
	public void setimgUrl(String url){
		
		imgUrl = url;
	}

}