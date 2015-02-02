package edu.hm.netzwerke;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class URLToJSONParser {

	private String jsonOrigin;

	public URLToJSONParser(String urlToJSON) {
		try {
			URL url = new URL(urlToJSON);
			URLConnection urlc = url.openConnection();
			InputStream in = urlc.getInputStream();
			jsonOrigin = new Scanner(in, "UTF-8").useDelimiter("\\A").next();
		} catch (Exception e) {
			jsonOrigin = null;
			e.printStackTrace();
		}
	}

	public List<JSONObject> parseJSONToStepList() {
		if (jsonOrigin == null) {
			return null;
		}
		JSONObject jobj = null;
		List<JSONObject> returnList = new ArrayList<JSONObject>();
		try {
			JSONParser parser = new JSONParser();
			jobj = (JSONObject) parser.parse(jsonOrigin);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONArray arr = (JSONArray) jobj.get("routes");
		jobj = (JSONObject) arr.get(0);
		arr = (JSONArray) jobj.get("legs");
		for (Object next : arr) {
			jobj = (JSONObject) next;
			JSONArray temp = (JSONArray) jobj.get("steps");
			returnList.addAll(temp);
		}
		return returnList;
	}
}
