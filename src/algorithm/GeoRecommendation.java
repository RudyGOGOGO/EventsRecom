package algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Collections;

import apple.laf.JRSUIConstants.SegmentTrailingSeparator;

import java.util.Map;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class GeoRecommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		List<Item> recommendedItems = new ArrayList<>();
		DBConnection conn = DBConnectionFactory.getConnection();
		try {
			// step 1: get all favorite items
			Set<String> favoriteItems = conn.getFavoriteItemIds(userId);
			// step 2: get all categories of favorite items, sort by count
			Map<String, Integer> allCategories = new HashMap<>();
			for (String itemId : favoriteItems) {
				Set<String> categories = conn.getCategories(itemId);
				for (String category : categories) {
					allCategories.put(category, allCategories.getOrDefault(category, 0) + 1);
				}
			}
			List<Map.Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
			Collections.sort(categoryList, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
			Set<Item> visitedItems = new HashSet<>();
			// step 3: do search based on category, filter out favorite events, sort by distance
			if (!categoryList.isEmpty()) {
				for (Entry<String, Integer> category : categoryList) {
					List<Item> items = conn.searchItems(lat, lon, category.getKey());
					List<Item> filteredItems = new ArrayList<>();
					//de-duplication
					for (Item item : items) {
						if (!favoriteItems.contains(item.getItemId()) 
							&& !visitedItems.contains(item)) {
							filteredItems.add(item);
						}
					}
					Collections.sort(filteredItems, (item1, item2) -> Double.compare(item1.getDistance(), 
																	  item2.getDistance()));
					visitedItems.addAll(items);
					recommendedItems.addAll(filteredItems);
				}
			// step 4: if no favorite items, return items based on the location
			} else {
				recommendedItems.addAll(conn.searchItems(lat, lon,null));
			}
		} finally {
			conn.close();
		}
		System.out.println(recommendedItems.size());
		return recommendedItems;
	}
}
