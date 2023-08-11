package de.dfki.mlt.rudimant.agent.nlp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.dfki.lt.tr.dialogue.cplan.DagNode;
import de.dfki.lt.tr.dialogue.cplan.FlatHierarchy;

public class TestRasaConversion {
  static String input = "{\"text\":\"spring zu kapitel sieben\", "
        + "\"intent\":{\"name\":\"Command_GotoChapter_\","
        + "\"confidence\":0.992901623249054}, "
        + "\"entities\":[{\"entity\":\"number\",\"start\":18,\"end\":24,\"confidence_entity\":0.9997685551643372,\"value\":\"sieben\",\"extractor\":\"DIETClassifier\",\"processors\":[\"EntitySynonymMapper\"]}], "
        + "\"text_tokens\":[[0,6],[7,9],[10,17],[18,24]], "
        + "\"intent_ranking\":["
        + "{\"name\":\"Command_GotoChapter_\", \"confidence\":0.992901623249054},"
        + "{\"name\":\"Command_JumpChapter_Forward\",\"confidence\":0.00343071180395782},"
        + "{\"name\":\"Command_JumpChapter_Back\",\"confidence\":0.0013150766026228666},"
        + "{\"name\":\"Command_Quit_\",\"confidence\":0.0006327275768853724},"
        + "{\"name\":\"Command_Start_\",\"confidence\":0.0005132745718583465},"
        + "{\"name\":\"Command_JumpChapter_0\",\"confidence\":0.00035781715996563435},"
        + "{\"name\":\"Command_Stop_\",\"confidence\":0.00031213683541864157},"
        + "{\"name\":\"Command_Gotoslide_\",\"confidence\":0.0003002288576681167},"
        + "{\"name\":\"Command_Jumpslide_Forward\",\"confidence\":0.00011200324661331251},"
        + "{\"name\":\"Command_GotoChapter_1\",\"confidence\":0.00010749153443612158}],"
        + " \"response_selector\":{\"all_retrieval_intents\":[],\"default\":{\"response\":{\"responses\":null,\"confidence\":0.0,\"intent_response_key\":null,\"utter_action\":\"utter_None\"},\"ranking\":[]}}}";


  @BeforeClass
  public static void initTest() {
    DagNode.init(new FlatHierarchy());
  }

  private RasaNlu init(Double d) {
    RasaNlu nlu = new RasaNlu();
    Map<String, Object> conf = new HashMap<>();
    conf.put("converter", "src/test/resources/cplanner/rasaconv");
    if (d != null) {
      conf.put(RasaNlu.CFG_MINENTITY_CONFINDENCE, d);
    }
    nlu.init(new File("."), "de_DE", conf);
    if (nlu.rasaNLU("hello.") == null) {
      return null; // rasa not available
    }
    return nlu;
  }


  @Test
  public void test() {
    RasaNlu nlu = init(null);
    assumeTrue(nlu != null); // rasa server available?

    JSONObject obj = new JSONObject(input);
    DialogueAct res = nlu.convert(obj);
    //System.out.println(res);
    assertEquals("7", res.getValue("what"));

    res = nlu.analyse("vorheriges kapitel");
    assertEquals("-1", res.getValue("what"));

    res = nlu.analyse("kapitel zurück");
    assertEquals("-1", res.getValue("what"));

    res = nlu.analyse("zwei kapitel vor");
    assertEquals("2", res.getValue("what"));
  }

  @Test
  public void testThresh() {
    RasaNlu nlu = init(null);
    assumeTrue(nlu != null); // rasa server available?

    // reject based on confidence
    DialogueAct res = nlu.analyse("springe kapitel hotel");
    assertNull(res);
  }

  @Test
  public void testEntityThresh() {
    RasaNlu nlu = init(1.0);
    assumeTrue(nlu != null); // rasa server available?

    // reject based on entity confidence
    DialogueAct res = nlu.analyse("springe zu kapitel acht");
    assertNull(res.getValue("what"));
  }
}
