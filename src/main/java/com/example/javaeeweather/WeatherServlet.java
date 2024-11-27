package com.example.javaeeweather;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherServlet extends HttpServlet {

    private static final String API_KEY = "4aa517965aeed5c015fa6b0f8e7af8be";
    private static final String CITY_URL = "http://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s";
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String city = request.getParameter("city");

        if (city == null || city.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Parameter 'city' is required");
            return;
        }

        try {
            // Получаем координаты города
            String cityCoordinates = getCityCoordinates(city);
            if (cityCoordinates == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("City not found");
                return;
            }

            // Разбиваем координаты на широту и долготу
            JSONObject cityData = new JSONObject(cityCoordinates);
            double lat = cityData.getDouble("lat");
            double lon = cityData.getDouble("lon");
            // Получаем данные о погоде
            String weatherData = getWeatherData(lat, lon);

            JSONObject weatherJson = new JSONObject(weatherData);

            // Извлекаем нужные поля
            String cityName = weatherJson.getString("name");
            double temp = weatherJson.getJSONObject("main").getDouble("temp");
            double feelsLike = weatherJson.getJSONObject("main").getDouble("feels_like");

            // Форматируем вывод
            String result = String.format("City: %s, Temperature: %.2f°C, Feels Like: %.2f°C", cityName, temp, feelsLike);

            // Отправляем пользователю
            response.setContentType("text/plain");
            response.getWriter().write(result);

//            response.setContentType("application/json");
//            response.getWriter().write(weatherData);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error processing request: " + e.getMessage());
        }
    }

    // Метод для получения координат города
    private String getCityCoordinates(String city) throws IOException {
        String apiUrl = String.format(CITY_URL, city, API_KEY);
        String response = sendHttpRequest(apiUrl);

        JSONArray cityArray = new JSONArray(response);
        if (cityArray.isEmpty()) {
            return null;
        }

        // Берем первый элемент из массива (самый релевантный результат)
        JSONObject cityObject = cityArray.getJSONObject(0);
        return cityObject.toString();
    }

    // Метод для получения данных о погоде
    private String getWeatherData(double lat, double lon) throws IOException {
        String apiUrl = String.format(WEATHER_URL, lat, lon, API_KEY);
        return sendHttpRequest(apiUrl);
    }

    // Общий метод для отправки HTTP-запроса
    private String sendHttpRequest(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        String responseMessage = conn.getResponseMessage();
        if (responseCode == 200) { // Успешный ответ
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return content.toString();
        } else {
            throw new IOException("Failed to fetch data from API, response code: " + responseCode + ", response message: " + responseMessage);
        }
    }
}
