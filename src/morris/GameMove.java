package morris;

import java.io.Serializable;

public class GameMove implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int clickIndex;

   public GameMove(int clickIndex) {
       this.clickIndex = clickIndex;
   }
 
}
