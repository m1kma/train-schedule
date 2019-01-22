package mjm.trainschedule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;


/**
 * Class is used to parse XML formatted RSS feed and compose attributes to object model.
 * Parser is hardcoded to parse this specific RSS feed.
 * 
 * @author Mäkelä
 *
 */
public class XMLParser {

    private static final String ns = null;
   
    public List<TrainItem> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }
    
    
    private List<TrainItem> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
    	
        List<TrainItem> entries = new ArrayList<TrainItem>();

        parser.require(XmlPullParser.START_TAG, ns, "rss");
        
        while (parser.next() != XmlPullParser.END_TAG) {
        	
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            
            if (name.equals("channel")) {
            	
	            parser.require(XmlPullParser.START_TAG, ns, "channel");
	            
	            while (parser.next() != XmlPullParser.END_TAG) {
	            	
	            	if (parser.getEventType() != XmlPullParser.START_TAG) {
	                    continue;
	                }
	            	
	            	 String name2 = parser.getName();
	            
	            	// Starts by looking for the entry tag
	                 if (name2.equals("item")) {
	                     entries.add(readEntry(parser));
	                 } else {
	                     skip(parser);
	                 }
	            }
            }
            
        }  
        return entries;
    }
    
        
    public List<TrainDetailsItem> parseDetails(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readDetailsFeed(parser);
        } finally {
            in.close();
        }
    }
    
    
    private List<TrainDetailsItem> readDetailsFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
    	
        List<TrainDetailsItem> entries = new ArrayList<TrainDetailsItem>();

        parser.require(XmlPullParser.START_TAG, ns, "rss");
        
        while (parser.next() != XmlPullParser.END_TAG) {
        	
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            
            if (name.equals("channel")) {
            	
	            parser.require(XmlPullParser.START_TAG, ns, "channel");
	            
	            while (parser.next() != XmlPullParser.END_TAG) {
	            	
	            	if (parser.getEventType() != XmlPullParser.START_TAG) {
	                    continue;
	                }
	            	
	            	 String name2 = parser.getName();
	            
	            	// Starts by looking for the entry tag
	                 if (name2.equals("item")) {
	                     entries.add(readDetailsEntry(parser));
	                 } else {
	                     skip(parser);
	                 }
	            }
            }
            
        }  
        return entries;
    }
    
    
    public static class TrainItem {
    	public final String guid;
        public final String title;
        public final String eta;
        public final String scheduledTime;
        public final String fromStation;
        public final String toStation;
        public final String status;
        public final String category;

        private TrainItem(String guid, String title, String eta, String scheduledTime, String fromStation, String toStation, String status, String category) {
        	this.guid = guid;
            this.title = title;
            this.eta = eta;
            this.scheduledTime = scheduledTime;
            this.fromStation = fromStation;
            this.toStation = toStation;
            this.status = status;
            this.category = category;
        }
    }
      
    
    public static class TrainDetailsItem {
    	public final String guid;
        public final String title;
        public final String eta;        
        public final String scheduledTime;
        public final String status;
        public final String completed;

        private TrainDetailsItem(String guid, String title, String eta, String scheduledTime, String status, String completed) {
        	this.guid = guid;
            this.title = title;
            this.eta = eta;
            this.scheduledTime = scheduledTime;
            this.status = status;
            this.completed = completed;
        }
    }
    
    
    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private TrainItem readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
    	
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String guid = null;
        String title = null;
        String eta = null;
        String scheduledTime = null;
        String fromStation = null;
        String toStation = null;
        String status = null;
        String category = null;
        
        while (parser.next() != XmlPullParser.END_TAG) {
        	
            if (parser.getEventType() != XmlPullParser.START_TAG) 
            {
                continue;
            }
            
            String name = parser.getName();
            
            if (name.equals("title")) 
            {
                title = readTitle(parser, name);
            } 
            else if (name.equals("guid")) 
            {
            	guid = readTitle(parser, name);
            } 
            else if (name.equals("category")) 
            {
            	category = readTitle(parser, name);
            } 
            else if (name.equals("eta")) 
            {
            	eta = readTitle(parser, name);
            } 
            else if (name.equals("scheduledTime")) 
            {
            	scheduledTime = readTitle(parser, name);
            } 
            else if (name.equals("fromStation"))
            {
            	fromStation = readTitle(parser, name);
            }
            else if (name.equals("toStation")) 
            {
            	toStation = readTitle(parser, name);
            } 
            else if (name.equals("status")) 
            {
            	status = readTitle(parser, name);
            } 
            else 
            {
                skip(parser);
            }
        }
        
        return new TrainItem(guid, title, eta, scheduledTime, fromStation, toStation, status, category);
    }
    
    
    
    private TrainDetailsItem readDetailsEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
    	
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String guid = null;
        String title = null;
        String eta = null;
        String scheduledTime = null;
        String status = null;
        String completed = null;
        
        while (parser.next() != XmlPullParser.END_TAG) {
        	
            if (parser.getEventType() != XmlPullParser.START_TAG) 
            {
                continue;
            }
            
            String name = parser.getName();
            
            if (name.equals("title")) 
            {
                title = readTitle(parser, name);
            } 
            else if (name.equals("guid")) 
            {
            	guid = readTitle(parser, name);
            } 
            else if (name.equals("eta")) 
            {
            	eta = readTitle(parser, name);
            } 
            else if (name.equals("scheduledTime")) 
            {
            	scheduledTime = readTitle(parser, name);
            } 
            else if (name.equals("status")) 
            {
            	status = readTitle(parser, name);
            } 
            else if (name.equals("completed")) 
            {
            	completed = readTitle(parser, name);
            } 
            else 
            {
                skip(parser);
            }
        }
        
        return new TrainDetailsItem(guid, title, eta, scheduledTime, status, completed);
    }
    
    
    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
    	
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        
        return title;
    }
    
 // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        
        return result;
    }
    
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
     }

}
