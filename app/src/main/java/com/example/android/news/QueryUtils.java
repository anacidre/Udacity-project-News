package com.example.android.news;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana on 14/06/2017.
 */

final class QueryUtils {
    // Tag for the log messages
    static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /*
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods which can be accessed directly
     * from the class name QueryUtils.
     */
    private QueryUtils() {
    }

    // Returns new URL object from the given string URL
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    // Make an HTTP request to the given URL and return a String as the response
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null)
            return jsonResponse;

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /*milliseconds*/);
            urlConnection.setConnectTimeout(15000 /*milliseconds*/);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            /*
             * If the request was successful (response code 200), then read the input stream and
             * parse the response.
             */
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the news JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null)
                inputStream.close();
        }

        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the whole JSON response from
     * the server.
     *
     * @param inputStream is the JSON response
     * @return output.toString()
     * @throws IOException if there are problems retrieving the JSON results
     */
    @NonNull
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
                    Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return a list of {@link News} object that have been built up from parsing the given JSON
     * response
     *
     * @param newsJSON is the given JSON
     * @return newses
     */
    @Nullable
    private static List<News> extractFeatureFromJson(String newsJSON) {
        // If the JSON string is empty or null, then return early
        if (TextUtils.isEmpty(newsJSON))
            return null;

        // Create an empty ArrayList that we can start adding news items to.
        List<News> newses = new ArrayList<>();
        try {
            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);

            if (baseJsonResponse.has("response")) {
                JSONObject responseObj = baseJsonResponse.getJSONObject("response");
                if (responseObj.has("results")) {
                    JSONArray newsArray = responseObj.getJSONArray("results");
                    for (int i = 0; i < newsArray.length(); i++) {
                        JSONObject currentNews = newsArray.getJSONObject(i);

                        String sectionName;
                        if (currentNews.has("sectionName")) {
                            sectionName = currentNews.getString("sectionName");
                        } else
                            sectionName = "No section name";

                        String webDate;
                        if (currentNews.has("webPublicationDate")) {
                            webDate = currentNews.getString("webPublicationDate");
                            String[] separatedWebDate = webDate.split("T");
                            webDate = separatedWebDate[0];
                        } else
                            webDate = "No publication date";

                        String webTitle;
                        if (currentNews.has("webTitle")) {
                            webTitle = currentNews.getString("webTitle");
                        } else
                            webTitle = "No title";

                        String webUrl;
                        if (currentNews.has("webUrl")) {
                            webUrl = currentNews.getString("webUrl");
                        } else
                            webUrl = "No news link";

                        JSONArray authorsArray;
                        String author = "";
                        String firstName, lastName;
                        if (currentNews.has("tags")) {
                            authorsArray = currentNews.getJSONArray("tags");
                            if (authorsArray.length() != 0)
                                for (int j = 0; j < authorsArray.length(); j++) {
                                    JSONObject nameObject = authorsArray.getJSONObject(j);
                                    if (nameObject.has("firstName")) {
                                        firstName = nameObject.getString("firstName");
                                    } else
                                        firstName = "";
                                    if (nameObject.has("lastName"))
                                        lastName = nameObject.getString("lastName");
                                    else
                                        lastName = "";

                                    author = firstName + " " + lastName;

                                }
                            else
                                author = "Unknown Author";
                        } else
                            author = "Unknown Author";


                        // Extract the value for the key called "imgUrl"
                        JSONObject imageLinks;
                        String imgUrl;
                        if (currentNews.has("fields")) {
                            imageLinks = currentNews.getJSONObject("fields");
                            if (imageLinks.has("thumbnail"))
                                imgUrl = imageLinks.getString("thumbnail");
                            else
                                imgUrl = "No image";
                        } else
                            imgUrl = "No image";

                        News news = new News(imgUrl, webTitle, author, sectionName, webDate, webUrl);
                        newses.add(news);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("QueryUtils", "Problem parsing the news JSON results", e);
        }
        return newses;
    }

    static List<News> fetchNewsData(String requestUrl) {
        // Create URL Object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Return the list of {@link News}
        return extractFeatureFromJson(jsonResponse);
    }
}
