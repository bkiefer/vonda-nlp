package de.dfki.mlt.rudimant.agent.nlp;

import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestJsonAnalyser  {

  private static final Logger logger =
      LoggerFactory.getLogger(RestJsonAnalyser.class);

  private URI uri;

  public static final String REST_HOST = "host";
  public static final String REST_PORT = "port";

  private boolean isSuccess(int code) {
    return code >= 200 && code <= 300;
  }

  /** Calls rasa NLU HTTP client. Returns JSON that needs to be converted,
   *  preferably use conversion with cplanner.
   *  Not private for testing purposes
   *
   * @param text the input text
   * @return a string representing a JSON object, or null, if no result was
   * produced by rasa
   */
  public String sendToServer(String text) {
    String jsonResult = null;
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      // Creating a HttpPost object
      JSONObject json = new JSONObject();
      StringEntity params = null;
      logger.debug(text);
      json.put("text", text);

      // Printing the method used
      params = new StringEntity(json.toString(), Charset.forName("UTF-8"));
      // Create a POST object
      HttpPost httppost = new HttpPost(uri);
      httppost.setHeader("Content-type", "application/json");
      httppost.setEntity(params);
      // Executing the request
      try (CloseableHttpResponse httpresponse = httpclient.execute(httppost)) {
        // Printing the status line
        if (!isSuccess(httpresponse.getStatusLine().getStatusCode())) {
          logger.debug("HTTP status: {}", httpresponse.getStatusLine().toString());
          return null;
        }
        jsonResult = EntityUtils.toString(httpresponse.getEntity());
        logger.debug("JSON output: {}", jsonResult);
      }
    } catch (IOException e) {
      logger.error(e.getMessage());
      return null;
    }
    return jsonResult;
  }

  /** We have to return true even if it seems as if the server is not there
   *  since it may take some time to start, so it will work later, even not
   *  at initialisation time.
   */
  @SuppressWarnings("rawtypes")
  public boolean initRest(Map config, String path) {
    try {
      String host = requireNonNullElse((String)config.get(REST_HOST), "localhost");
      int port = requireNonNullElse((Integer)config.get(REST_PORT), 5005);
      uri = new URI("http://" + host + ":" + port + path);
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return false;
    }
    return true;
  }

}
