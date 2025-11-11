package morris;

import java.io.Serializable;

public class GameMove implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int clickIndex;
    public final GamePhase gamePhase;

   public GameMove(int clickIndex, GamePhase phase) {
       this.clickIndex = clickIndex;
       this.gamePhase = phase;
   }
 
}
