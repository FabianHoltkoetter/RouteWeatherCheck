package edu.hm.netzwerke;

public class WeatherGeoPoint {
	double latitude;
	double longitude;
	//Temperatur
	double temperature;
	//Wetter-ID (laut OpenWeatherMap)
	int id;
	//Text-Vorhersage
	String forecast = "no forecast found";
	//Wetter-Typ als String
	String main;
	
	public WeatherGeoPoint(double lat, double lon){
		this.latitude = lat;
		this.longitude = lon;
	}
	
	public void setTemperature(double temp){
		this.temperature = Math.round(temp);
	}
	
	public void setId(double id){
		this.id = (int) id / 100;
	}
	
	public void setForecast(String forecast){
		this.forecast = forecast;
	}
	
	public void setMain(String main){
		this.main = main;
	}
	
	public double getTemperature(){
		return temperature;
	}
	
	public int getId(){
		return id;
	}
	
	public String getForecast(){
		return forecast;
	}
	
	public String getMain(){
		return main;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	@Override
	public String toString(){
		return ("Lat: " + latitude + " | Lng: " + longitude);
	}
}