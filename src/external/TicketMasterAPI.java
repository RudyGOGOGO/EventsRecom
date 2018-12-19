package external;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;
/*
 * TicketMaster API Doc: 
 * https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/#search-events-v2 
 */
public class TicketMasterAPI {
	//URL: protocol + hostname + endpoint
	private static final String URL="https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = "";
	private static final String API_KEY = "14bA652LMFzvc0C2Yk5FUragaQHRGU3A";
	//Query Part
	private static final String QUERY_TEMPLATE="apikey=%s&geoPoint=%s&keyword=%s&radius=50";
	//fields for parsing the response from TicketMaster API
	private static final String EMBEDDED = "_embedded";
	private static final String EVENTS = "events";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String URL_STR = "url";
	private static final String RATING = "rating";
	private static final String DISTANCE = "distance";
	private static final String VENUES = "venues";
	private static final String ADDRESS = "address";
	private static final String LINE1 = "line1";
	private static final String LINE2 = "line2";
	private static final String LINE3 = "line3";
	private static final String CITY = "city";
	private static final String IMAGES = "images";
	private static final String CLASSIFICATIONS = "classifications";
	private static final String SEGMENT = "segment";
	
	private List<Item> getItemList(JSONArray events) throws JSONException{
		List<Item> ls = new ArrayList<>();
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			ItemBuilder builder = new ItemBuilder();
			/*
			 * Id, Name, Rating, Url, Distance are 
			 * direct children of "_embedded"
			 * so that we can directly retrieve those info
			 * */
			if (!event.isNull(ID)) {
				builder.setItemId(event.getString(ID));
			}
			if (!event.isNull(NAME)) {
				builder.setName(event.getString(NAME));
			}
			if (!event.isNull(RATING)) {
				builder.setRating(event.getDouble(RATING));
			}
			if (!event.isNull(URL_STR)) {
				builder.setUrl(event.getString(URL_STR));
			}
			if (!event.isNull(DISTANCE)) {
				builder.setDistance(event.getDouble(DISTANCE));
			}
			/*
			 * three kinds of info to deal with specially
			 * 1)address; 2)image url; 3)categories
			 * */
			builder.setAddress(getAddress(event));
			builder.setImageUrl(getImageURL(event));
			builder.setCategories(getCategories(event));
			ls.add(builder.build());
		}
		return ls;
	}
	private String getAddress(JSONObject event) throws JSONException{
		if (!event.isNull(EMBEDDED)) {
			JSONObject embedded = event.getJSONObject(EMBEDDED);
			if (!embedded.isNull(VENUES)) {
				JSONArray venues = embedded.getJSONArray(VENUES);
				//return the first available venue with valid address
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					StringBuilder builder = new StringBuilder();
					if (!venue.isNull(ADDRESS)) {
						JSONObject address = venue.getJSONObject(ADDRESS);
						if (!address.isNull(LINE1)) {
							builder.append(address.getString(LINE1));
						}
						if (!address.isNull(LINE2)) {
							builder.append('\n');
							builder.append(address.getString(LINE2));
						}
						if (!address.isNull(LINE3)) {
							builder.append('\n');
							builder.append(address.getString(LINE3));
						}
						if (!venue.isNull(CITY)) {
							JSONObject city = venue.getJSONObject(CITY);
							if (!city.isNull(NAME)) {
								builder.append('\n');
								builder.append(city.getString(NAME));
							}
						}
					}
					String address = builder.toString();
					if (address.length() > 0) {
						return address;
					}
				}				
			}
		}
		return "";
	}
	private String getImageURL(JSONObject event) throws JSONException {
		if (!event.isNull(IMAGES)) {
			JSONArray images = event.getJSONArray(IMAGES);
			for (int i = 0; i < images.length(); ++i) {
				JSONObject image = images.getJSONObject(i);
				if (!image.isNull(URL_STR)) {
					return image.getString(URL_STR);
				}
			}
		}
		return "";
	}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull(CLASSIFICATIONS)) {
			JSONArray classifications = event.getJSONArray(CLASSIFICATIONS);
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull(SEGMENT)) {
					JSONObject segment = classification.getJSONObject(SEGMENT);
					if (!segment.isNull(NAME)) {
						categories.add(segment.getString(NAME));
					}
				}
			}
		}
		return categories;
	}
	/*
	 * return JSONArray with all info under "_embedded"
	 * */
	public List<Item> search(double lat, double lon, String keyword) {
		//step 1: convert the input keyword
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			keyword = URLEncoder.encode(keyword, "UTF-8");//e.g. Disney World --> Disney20%World
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		//step 2: convert latitude and longitude into geo hash
		String geoHash = GeoHash.encodeGeohash(lat, lon, 9);
		//step 3: generate the URL
		String query = String.format(QUERY_TEMPLATE, API_KEY, geoHash, keyword);
		try {
			/* step 4: build up the HTTP connection 
			 * based on the specified URL openConnection() 
		     * that returns URLConnection,  
			 * which needs to be casted to HttpURLConnection
			 * how to do?
			 * Create a URLConnection instance that represents a connection 
			 * to the remote object referred to by the URL.
			 * The HttpURLConnection class allows us to perform basic HTTP requests
			 * without the use of any additional libraries
			 */
			HttpURLConnection connection = (HttpURLConnection) 
							  new URL(URL + "?" + query).openConnection();
			/*
			 * step 5: set up the request type
			 * to tell what HTTP method to use. GET by default.
			 * The HTTPURLConnection class is used for all types of requests
			 * GET,POST,HEAD,OPTIONS,PUT,DELETE,TRACE
			 */
			connection.setRequestMethod("GET");
			/*
			 * step 6: get status code from an HTTP response message.
			 * when we can getResponseCode(), we check whether 
			 * we have sent the "GET" request
			 * if not, send it, then get the response code for the request
			 * otherwise, directly get the response code for the request
			 */
			int responseCode = connection.getResponseCode();
			//for debugging
			System.out.println("Send 'Get' request to URL" + URL);
			System.out.println("Response Code" + responseCode);
			/*
			 * step 7:get response body
			 * as the client, we receive info through input stream
			 * in case that the input stream is very large
			 * we should use BufferedReader to read line by line
			 * BufferedReader to improve reading efficiency
			 * Create a BufferedReader to help read text from a
			 * character-input stream. 
			 */
			BufferedReader in = new BufferedReader(new 
					InputStreamReader(connection.getInputStream()));
			/*
			 * step 8: append response data to response StringBuilder
			 * instance line by line
			 */
			StringBuilder response = new StringBuilder();
			//in.readLine() if there is no more content, return null 
			String inputline = null;
			while ((inputline = in.readLine()) != null) {
				response.append(inputline);
			}
			//3)close the input stream, release the connection
			in.close();
			//4)extract the key content we need
			try {
				JSONObject obj = new JSONObject(response.toString());
				if (!obj.isNull("_embedded")) {
					return getItemList(obj.getJSONObject("_embedded").getJSONArray("events"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	/*check whether the info retrieved from the server
	 *is correct
	 */
	private void queryAPI(double lat, double lon) {
		List<Item> items = search(lat, lon, null);
		try {
			for (Item item : items) {
				JSONObject event =  item.toJSONObject();
				System.out.println(event);
			}
		} catch(Exception e) {
			e.printStackTrace();
			
		}
	}
	public static void main(String[] args) {
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		tmAPI.queryAPI(29.682684, -95.295410);
	}
}
