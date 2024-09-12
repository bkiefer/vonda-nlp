package de.dfki.mlt.rudimant.agent.nlp;

import static java.util.Objects.requireNonNullElse;

import java.io.File;
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

public class RasaNlu extends Interpreter {

  private URI uri;

  private static final double DEFAULT_THRESHOLD = 0.75;

  public static final String CFG_RASA_HOST = "host";
  public static final String CFG_RASA_PORT = "port";
  public static final String CFG_MININTENT_CONFINDENCE = "min_confidence";
  public static final String CFG_MINENTITY_CONFINDENCE = "min_entity_confidence";

  public static String CONFIDENCE_DAG_SLOT = "confidence";

  // default is DEFAULT_THRESHOLD for both
  private double intent_confidence_threshold;
  private double entity_confidence_threshold;

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
  String rasaNLU(String text) {
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
  @Override
  public boolean init(File configDir, String language, Map config) {
    name = "rasa_" + language;
    try {
      String host = requireNonNullElse((String)config.get(CFG_RASA_HOST), "localhost");
      int port = requireNonNullElse((Integer)config.get(CFG_RASA_PORT), 5005);
      Double d = (Double)config.get(CFG_MININTENT_CONFINDENCE);
      intent_confidence_threshold = d == null ? DEFAULT_THRESHOLD : d;
      d = (Double)config.get(CFG_MINENTITY_CONFINDENCE);
      entity_confidence_threshold = d == null ? DEFAULT_THRESHOLD : d;
      uri = new URI("http://" + host + ":" + port + "/model/parse");
      //if (rasaNLU("hello.") == null) {
      //  return false;
      //}
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      //return false;
      throw new RuntimeException(ex);
    }
    return super.init(configDir, language, config);
  }

  @Override
  public DialogueAct analyse(String text) {
    String jsonResult = rasaNLU(text);
    if (jsonResult == null) {
      logger.info("No rasa NLU result");
      return null;
    }
    JSONObject obj = new JSONObject(jsonResult);
    obj.put("minEntityConfidence", entity_confidence_threshold);
    DialogueAct r = convert(obj);
    if (r != null && r.hasSlot(CONFIDENCE_DAG_SLOT) &&
        Double.parseDouble(r.getValue(CONFIDENCE_DAG_SLOT)) < intent_confidence_threshold) {
      r = null;
    }
    if (r == null) {
      logger.info("No {} result for {}", name, text);
    } else {
      logger.info("{} result for {}: {}", name, text, r.toString());
    }
    return r;
  }

  /** Override only to make the method accessible to test classes */
  @Override
  public DialogueAct convert(JSONObject object) {
    return super.convert(object);
  }
}
