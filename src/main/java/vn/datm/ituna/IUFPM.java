package vn.datm.ituna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class IUFPM {
  protected int currentTid = 0;
  protected double minimumSupport = 0;

  protected Map<Integer, IUPList> iUPMap = new HashMap<>();
  protected Map<Set<Integer>, ICUPList> iCUPMap = new HashMap<>();

  public void addDatabase(UTDatabase db) {
    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.getId();

        if (iUPMap.containsKey(id)) {
          iUPMap.get(id).addTPPair(currentTid, uItem.getProbability());
        } else {
          iUPMap.put(id, new IUPList(id, currentTid, uItem.getProbability()));
        }
      }

      currentTid++;
    }
  }

  public abstract List<UItemSet> mine();

  protected ICUPList constructICUPList(IUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setLateIndex(l1.getId(), i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2Index);

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  protected ICUPList constructICUPList(ICUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setLateIndex(l1.getIds(), i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2Index);

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  protected void reconstructICUPList(ICUPList cList) {
    List<Set<Integer>> parentPattern = cList.getParentPatterns();

    if (parentPattern.get(0).size() == 1) {
      IUPList l1 = iUPMap.get(parentPattern.get(0).toArray(Integer[]::new)[0]);
      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() > cList.getLateIndex(l1.getId())
          && l2.size() > cList.getLateIndex(l2.getId())) {
        int l2Index = cList.getLateIndex(l2.getId());

        for (int i = cList.getLateIndex(l1.getId()); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setLateIndex(l1.getId(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2Index);
      }
    } else {
      ICUPList l1 = iCUPMap.get(parentPattern.get(0));
      reconstructICUPList(l1);

      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() > cList.getLateIndex(l1.getIds())
          && l2.size() > cList.getLateIndex(l2.getId())) {
        int l2Index = cList.getLateIndex(l2.getId());

        for (int i = cList.getLateIndex(l1.getIds()); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setLateIndex(l1.getIds(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2Index);
      }
    }
  }
}
