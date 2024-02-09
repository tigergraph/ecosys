package com.tigergraph.spark.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.tigergraph.spark.client.Builder.LoadBalanceTarget;

public class BuilderTest {

  @Test
  public void LoadBalanceTest() {
    List<String> urlList = Arrays.asList("a , b ,,,,c,c", "a", " a a ,a,");
    List<List<String>> urlsList =
        Arrays.asList(
            new ArrayList<>(Arrays.asList("a", "b", "c")),
            new ArrayList<>(Arrays.asList("a")),
            new ArrayList<>(Arrays.asList("a a", "a")));
    for (int i = 0; i < urlList.size(); i++) {
      LoadBalanceTarget<Write> target = new LoadBalanceTarget<Write>(Write.class, urlList.get(i));
      // Iterate 100 times, then it should meet all the elements(very high probability)
      for (int j = 0; j < 100; j++) {
        String randUrl = target.url();
        if (urlsList.get(i).contains(randUrl)) {
          urlsList.get(i).remove(randUrl);
        }
      }
      assertEquals(Arrays.asList(), urlsList.get(i));
    }
  }
}
