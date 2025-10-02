package vn.datm.ituna;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Hello world! */
public class App {
  public static void main(String[] args) {
    UTDatabase db = new UTDatabase();

    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      db.readInputStream(is);
    } catch (IOException e) {
      e.printStackTrace();
    }

    ITUNA iTUNA = new ITUNA(1000);
    iTUNA.addDatabase(db);
    iTUNA.mine();
    System.out.println(Arrays.toString(iTUNA.getTopK()));
  }
}
