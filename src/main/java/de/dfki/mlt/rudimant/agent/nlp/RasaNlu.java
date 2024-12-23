package de.dfki.mlt.rudimant.agent.nlp;

import java.io.File;
import java.util.Map;

import org.json.JSONObject;

public class RasaNlu extends Interpreter {

  private static final double DEFAULT_THRESHOLD = 0.75;

  public static final String CFG_MININTENT_CONFINDENCE = "min_confidence";
  public static final String CFG_MINENTITY_CONFINDENCE = "min_entity_confidence";
  public static final String CFG_ALIVE_STRING = "alive_string";

  public static String CONFIDENCE_DAG_SLOT = "confidence";

  /** A string that can be analysed, to check if the server is up */
  public String alive_string = "hello";

  // default is DEFAULT_THRESHOLD for both
  private double intent_confidence_threshold;
  private double entity_confidence_threshold;

  private RestJsonAnalyser rja = new RestJsonAnalyser();

  /** We have to return true even if it seems as if the server is not there
   *  since it may take some time to start, so it will work later, even not
   *  at initialisation time.
   */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean init(File configDir, String language, Map config) {
    name = "rasa_" + language;
    Double d = (Double)config.get(CFG_MININTENT_CONFINDENCE);
    intent_confidence_threshold = d == null ? DEFAULT_THRESHOLD : d;
    d = (Double)config.get(CFG_MINENTITY_CONFINDENCE);
    entity_confidence_threshold = d == null ? DEFAULT_THRESHOLD : d;
    if (config.containsKey(CFG_ALIVE_STRING)) {
      alive_string = (String) config.get(CFG_ALIVE_STRING);
    }
    boolean ok = rja.initRest(config);
    return ok && super.init(configDir, language, config);
  }

  /** Only for test purposes */
  String rasaNLU(String text) {
    String jsonResult = rja.sendToServer(text, "/model/parse");
    if (jsonResult == null) {
      logger.info("No rasa NLU result");
      return null;
    }
    return jsonResult;
  }


  @Override
  public DialogueAct analyse(String text) {
    String jsonResult = rasaNLU(text);
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

  public boolean alive() {
    return rja.sendToServer("hello.", "/model/parse") != null;
  }
}
