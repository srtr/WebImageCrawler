package webimagecrawler;

import org.jsoup.nodes.Element;

public class Website {       
	Element name;
	int weight;    

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
	
}