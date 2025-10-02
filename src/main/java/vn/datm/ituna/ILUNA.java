package vn.datm.ituna;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ILUNA {
  private final double minimumSupport;
  private Map<Integer, IUPList> iUPListMap = new HashMap<>();
  private Map<ImmutableSet<Integer>, ICUPList> iCUPListMap = new HashMap<>();
  private List<IUPList> iUPLists;
  private int currentTid = 0;

  public ILUNA(double minimumSupport) {
    this.minimumSupport = minimumSupport;
  }

  public void debug() {
    System.out.println(constructICUPList(625, 1041));
    // System.out.println(iUPLists.subList(0, 5));
  }

  public void addDatabase(UTDatabase db) {
    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int itemId = uItem.getId();

        if (iUPListMap.containsKey(itemId)) {
          iUPListMap.get(itemId).addTPPair(currentTid, uItem.getProbability());
        } else {
          iUPListMap.put(itemId, new IUPList(itemId, currentTid, uItem.getProbability()));
        }
      }

      currentTid++;
    }

    iUPLists = new ArrayList<>(iUPListMap.values());
    iUPLists.sort((a, b) -> -Double.compare(a.getExpectedSupport(), b.getExpectedSupport()));
  }

  public List<UItemSet> mine() {
    List<UItemSet> results = new ArrayList<>();

    for (IUPList jList : iUPListMap.values()) {
      if (jList.getExpectedSupport() >= minimumSupport) {
        results.add(jList.getItemSet());

        int id = jList.getId();

        Set<Integer> prefix = new HashSet<>();
        prefix.add(id);

        for (IUPList kList : iUPListMap.values()) {
          if (jList.getId() > kList.getId()
              && kList.getExpectedSupport() >= minimumSupport
              && jList.getExpectedSupport() * kList.getMaxSupport() >= minimumSupport) {
            System.err.println("hhe");
          }
        }
      }
    }

    return results;
  }

  private ICUPList constructICUPList(int id1, int id2) {
    return iUPListMap.get(id1).join(iUPListMap.get(id2), minimumSupport);
  }
}
