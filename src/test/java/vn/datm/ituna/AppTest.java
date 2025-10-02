package vn.datm.ituna;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

/** Unit test for simple App. */
public class AppTest {

  /** Rigorous Test :-) */
  @Test
  public void shouldAnswerWithTrue() {
    UItemSet uItemSet = new UItemSet(2, 0.1);
    assertTrue(uItemSet.getIds().equals(Set.of(2)));
  }

  @Test
  public void shouldMapItemSet() {
    Map<ImmutableSet<Integer>, Integer> map = new HashMap<>();
    map.put(ImmutableSet.of(1, 2), 1);
    Set<Integer> keySet = new HashSet<>();
    keySet.add(2);
    keySet.add(1);

    assertTrue(map.get(keySet) == 1);
  }
}
