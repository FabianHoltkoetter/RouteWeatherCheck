package edu.hm.netzwerke;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WeatherMan {
	private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?";

	public WeatherGeoPoint getWeatherGeoPoint(double lattitude, double longitude) {
		WeatherGeoPoint newPoint = new WeatherGeoPoint(lattitude, longitude);
		String weatherString = "";
		String helpString;
		try {
			URL url = new URL(WEATHER_API_URL + "lat=" + lattitude + "&lon=" + longitude  + "&lang=de");
			URLConnection urlc = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
			helpString = br.readLine();
			while (helpString != null && !helpString.equals("")) {
				weatherString += helpString;
				helpString = br.readLine();
			}
			JSONObject jobj = null;
			JSONParser parser = new JSONParser();
			JSONObject jsonRoot = (JSONObject) parser.parse(weatherString);

			JSONObject tmp = (JSONObject) jsonRoot.get("main");
			if(((Object) tmp.get("temp")).getClass().equals(Double.class)){
				newPoint.setTemperature(((double)tmp.get("temp")) - 273.15);
			}else{
				newPoint.setTemperature(((long)tmp.get("temp")) - 273.15);
			}

			JSONArray arr = (JSONArray) jsonRoot.get("weather");
			tmp = (JSONObject) arr.get(0);
			newPoint.setMain((String) tmp.get("main"));
			newPoint.setForecast((String) tmp.get("description"));
			newPoint.setId((Long) tmp.get("id"));

		} catch (Exception e) {
			weatherString = "An Error occured:\n " + e.getMessage();
			e.printStackTrace();
		}
		return newPoint;
	}
}
