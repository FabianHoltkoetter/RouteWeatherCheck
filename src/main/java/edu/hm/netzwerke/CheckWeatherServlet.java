package edu.hm.netzwerke;

import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


@WebServlet(description = "Weather Check Servlet", urlPatterns = { "/Weather" })
public class CheckWeatherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String GOOGLE_MAPS_URL = "https://maps.googleapis.com/maps/api/directions/json?origin=";
	// Distance between points to measure in METERS!
	private static int DIST = 50000;
	//Object to measure the weather
	private WeatherMan man = new WeatherMan();
	//Object to fetch the response from Google Maps API
	private URLToJSONParser jpar;
	//Variables to store destinations
	String origin;
	String destination;
	List<String> waypoints;

	public CheckWeatherServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		this.printHtmlPageToClient(
				writer,
				getServletContext().getRealPath(
						"/WebAppHtml/WebAppPageStart.html"));
		this.printHtmlPageToClient(
				writer,
				getServletContext().getRealPath(
						"/WebAppHtml/WebAppPageContentIndex.html"));
		this.printHtmlPageToClient(
				writer,
				getServletContext().getRealPath(
						"/WebAppHtml/WebAppPageEnd.html"));
		writer.flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
		String mapsURL = this.buildGoogelMapsAPIURL(br.readLine());
		jpar = new URLToJSONParser(mapsURL);

		PrintWriter writer = resp.getWriter();
		this.printHtmlPageToClient(writer, getServletContext().getRealPath("/WebAppHtml/WebAppPageStart.html"));

		// Liste aus Steps f�r gesamte Route erstellen (inkl. Zwischenstopps)
		List<JSONObject> stepList = new ArrayList<JSONObject>();
		try{
			stepList = jpar.parseJSONToStepList();
		}catch(Exception e){
			writer.println("<h2>Error: Route couldn't be calculated. Please try again!</h2>");
			e.printStackTrace();
		}

		long count = 0;
		List<WeatherGeoPoint> measurePoints = new ArrayList<WeatherGeoPoint>();
		for (JSONObject next : stepList) {
			JSONObject temp = (JSONObject) next.get("distance");
			long stepDistance  = (long) temp.get("value");
			temp = (JSONObject) next.get("start_location");
			double latStart = (double) temp.get("lat");
			double lngStart = (double) temp.get("lng");
			temp = (JSONObject) next.get("end_location");
			double latEnd = (double) temp.get("lat");
			double lngEnd = (double) temp.get("lng");
			if ( stepDistance > DIST) {
				int s = (int) (stepDistance / DIST);
				for(int i=0; i < s; i++){
					WeatherGeoPoint p1 = new WeatherGeoPoint(latStart, lngStart);
					WeatherGeoPoint p2 = new WeatherGeoPoint(latEnd, lngEnd);
					WeatherGeoPoint newPoint = this.calculateNewPoint(p1, p2, (DIST / 1000 * (i+1) ));
					measurePoints.add(man.getWeatherGeoPoint(newPoint.getLatitude(), newPoint.getLongitude()));
				}
			} else {
				count += stepDistance;
				if (count >= DIST) {
					count = 0;
					measurePoints.add(man.getWeatherGeoPoint(latStart, lngStart));
				}
			}
		}
		
		boolean[] goodToDrive = new boolean[measurePoints.size()];
		for(int i = 0; i < measurePoints.size(); i++){
			goodToDrive[i] = (measurePoints.get(i).getTemperature() > 6 &&
								measurePoints.get(i).getId() == 8);
		}
		double perc = 0;
		int firstStepWithBadWeather = -1;
		for(boolean next : goodToDrive){
			perc = next ? (perc+1) : perc;
			if(!next && firstStepWithBadWeather == -1){
				firstStepWithBadWeather = (int) perc;
			}
		}
		writer.println("<h2>Your result:</h2>");
		writer.println((perc/goodToDrive.length*100) + "% of the route is good to drive with an open roof!<br/>");
		if(firstStepWithBadWeather != -1)
			writer.println("But after " + firstStepWithBadWeather*50 + "km you should really close it...");
		
		writer.print("<br/><br/><hr><br/>");
		// Print Google Map
		this.printHtmlPageToClient(writer, getServletContext().getRealPath("/WebAppHtml/GoogleMap1.html"));
		//Print Markers
		for(WeatherGeoPoint p : measurePoints){
		 writer.println("{position:new google.maps.LatLng(" + p.getLatitude() + "," + p.getLongitude() +"),"
		 		+ "type:'" + p.getId() + "',"
		 		//+ "type:'9',"
		 		+ "temp:'" + p.getTemperature() + " C<br/>" + p.getForecast() + "'},");
		}
		//Print directions for route
		this.printHtmlPageToClient(writer, getServletContext().getRealPath("/WebAppHtml/GoogleMap2.html"));
		String tmpS = "";
		for(String next : waypoints){
		 tmpS += ("{location:'" + next + "'},");
		}
		writer.println("origin:'" + this.origin +
			 "',destination:'" + this.destination +
			 "',waypoints:[" + tmpS	 + "],");
		this.printHtmlPageToClient(writer, getServletContext().getRealPath("/WebAppHtml/GoogleMap3.html"));
		this.printHtmlPageToClient(writer, getServletContext().getRealPath("/WebAppHtml/WebAppPageEnd.html"));
	}

	// Prints a HTML-File from the given file-path to the client
	private void printHtmlPageToClient(PrintWriter writer, String path) {
		try (FileInputStream fis = new FileInputStream(new File(path))) {
			while (fis.available() != 0) {
				writer.write(fis.read());
			}
		} catch (Exception e) {
			writer.println("ERROR OCCURED IN printHtmlPageToClient");
			writer.println(e.toString());
			e.printStackTrace();
		}
	}

	// Build the Google-Maps API request URL based on the User Input
	public String buildGoogelMapsAPIURL(String input) {
		this.waypoints = new ArrayList<String>();
		String[] userInput = input.split("&");
		this.origin=userInput[0].split("=")[1];
		this.destination=userInput[4].split("=")[1];
		String finalUrl = GOOGLE_MAPS_URL + origin + "&destination=" + destination;
		String result = "";
		int cnt = 0;
		for (int i = 1; i < 4; i++) {
			String waypoint = null;
			if (userInput[i].split("=").length != 1) {
				waypoint = userInput[i].split("=")[1];
			}
			if (waypoint != null) {
				result += waypoint + "|";
				this.waypoints.add(waypoint);
			}
		}
		if (!result.equals(""))
			finalUrl += "&waypoints=" + result;
		
		return finalUrl;
	}

	// Calculates a new GeoPoint-Object between two points in a certain distance
	// from p1
	public static WeatherGeoPoint calculateNewPoint(WeatherGeoPoint p1, WeatherGeoPoint p2,
			double dist) {
		// Create Variables
		double distance = dist / 6371.0;
		double latitude1 = Math.toRadians(p1.getLatitude());
		double longitude1 = p1.getLongitude();
		double latitude2 = Math.toRadians(p2.getLatitude());
		double longitude2 = p2.getLongitude();
		double longDiff = Math.toRadians(longitude2 - longitude1);
		// Calculate bearing
		double y = Math.sin(longDiff) * Math.cos(latitude2);
		double x = Math.cos(latitude1) * Math.sin(latitude2)
				- Math.sin(latitude1) * Math.cos(latitude2)
				* Math.cos(longDiff);
		double bearing = Math
				.toRadians((Math.toDegrees(Math.atan2(y, x)) + 360) % 360);
		// Calculate new Point
		latitude1 = Math.toRadians(p1.getLatitude());
		longitude1 = Math.toRadians(p1.getLongitude());
		double newLat = Math.asin(Math.sin(latitude1) * Math.cos(distance)
				+ Math.cos(latitude1) * Math.sin(distance) * Math.cos(bearing));
		double a = Math.atan2(
				Math.sin(bearing) * Math.sin(distance) * Math.cos(latitude1),
				Math.cos(distance) - Math.sin(latitude1) * Math.sin(newLat));
		double newLon = longitude1 + a;
		newLon = (newLon + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
		return new WeatherGeoPoint(Math.toDegrees(newLat), Math.toDegrees(newLon));
	}
}
