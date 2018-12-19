package rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import sun.java2d.pipe.BufferedBufImgOps;
import entity.Item;

public class RpcHelper {
	public static void writeJSONArray(HttpServletResponse response, JSONArray array)
		throws IOException{
		response.setContentType("application/json");
		response.setHeader("Access-Control-Allow-Origin", "*");
		PrintWriter out = response.getWriter();
		out.print(array);
		out.close();
	}
	public static void writeJSONObject(HttpServletResponse response, JSONObject obj) 
		throws IOException{
		response.setContentType("application/json");
		response.setHeader("Access-Control-Allow-Origin", "*");
		PrintWriter out = response.getWriter();
		out.print(obj);
		out.close();
	}
	public static JSONObject readJSONObject(HttpServletRequest request) 
		throws IOException {
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = request.getReader();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			return new JSONObject(builder.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	  // Converts a list of Item objects to JSONArray.
	public static JSONArray getJSONArray(List<Item> items) {
	    JSONArray result = new JSONArray();
	    if (!items.isEmpty()) {
	  	      for (Item item : items) {
	  	        result.put(item.toJSONObject());
	  	      }
	    } 
	    return result;
	}

}
