package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.TicketMasterAPI;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet(urlPatterns= {"/search"})
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * To enforce the login process, we need to protect all the other
	 * servlets with a verification: to see if the session is logged in or not
	 * To do that, we can store the user_id into the session attribute when login
	 * and check if the attribute exists at the beginning of other servlets
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		// allow access only if session exists
		HttpSession session = request.getSession(false);
		if (session == null) {
			response.setStatus(403);
			return;
		}

		// optional
		String userId = session.getAttribute("user_id").toString();
		//get the latitude and longitude as well 
		//as the keyword from front-end
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		//term can be empty
		String term = request.getParameter("term");
		DBConnection connection = DBConnectionFactory.getConnection();//"mysql" as default
		JSONArray array = new JSONArray();
		try {
			/*
			 * search through TicketMasterAPI
			 * return the item list to the front-end
			 * and save the item list into the database
			 * at the same time 
			 */
			List<Item> items = connection.searchItems(lat, lon, term);
			Set<String> favoriteItems = connection.getFavoriteItemIds(userId);
			
			for (Item item : items) {
				JSONObject obj = item.toJSONObject();
				obj.put("favorite", favoriteItems.contains(item.getItemId()));
				array.put(obj);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		RpcHelper.writeJSONArray(response, array);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
