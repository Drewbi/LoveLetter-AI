package agents;
import loveletter.*;
import java.util.Random;
import java.util.Arrays;
/**
 * An interface for representing an agent in the game Love Letter
 * All agent's must have a 0 parameter constructor
 * */
public class GodV1 implements Agent{

  private Random rand;
  private State current;
  private int myIndex;
  private Players probabilities;

  //0 place default constructor
  public GodV1(){
    rand  = new Random();
  }

  /**
   * Reports the agents name
   * */
  public String toString(){return "*GodV1*";}


  /**
   * Method called at the start of a round
   * @param start the starting state of the round
   **/
  public void newRound(State start){
    current = start;
    myIndex = current.getPlayerIndex();
    probabilities = new Players(myIndex);
    probabilities.updateProbabilities();
  }

  /**
   * Method called when any agent performs an action. 
   * @param act the action an agent performs
   * @param results the state of play the agent is able to observe.
   * **/
  public void see(Action act, State results){
    current = results;
  }

  /**
   * Perform an action after drawing a card from the deck
   * @param c the card drawn from the deck
   * @return the action the agent chooses to perform
   * */
  public Action playCard(Card c){
    Action act = null;
    Card play;
    Card card1 = c;
    Card card2 = current.getCard(myIndex);
    if(card1.toString() == "Handmaid") {
      play = card1;
      System.out.println("PLAYING HANDMAID");
    } else if(card1.toString() == "Princess"){
      play = current.getCard(myIndex);
    } else if(current.getCard(myIndex).toString() == "Princess"){
      play = card1;
    } else if(card1.toString() == "Countess" && card2.toString() == "Prince" || card2.toString() == "King"){
      play = card1;
    } else if(card2.toString() == "Countess" && card1.toString() == "Prince" || card1.toString() == "King"){
      play = card2;
    }
    else play = c.value() < current.getCard(myIndex).value() ? c : current.getCard(myIndex);
    int target = rand.nextInt(current.numPlayers());
    while(current.eliminated(target) || target == myIndex){
      target = rand.nextInt(current.numPlayers());
    }
    act = getAction(play, target);
    if(!current.legalAction(act, c)){
      System.out.println("Action: " + act +  " is not valid");
      System.out.println("Hand is " + card1 + " and " + card2);
      if(play != Card.PRINCE){
        act = playCard(c);
      } else {
        for(int i = 3; i <= 0; i++){
          act = getAction(play, target);
          if(current.legalAction(act, c)) break;
        }
      }
    }
    return act;
  }

  /**
   * 
   * @param play
   * @param target
   * @return action to try
   * @throws IllegalActionException when the Action produced is not legal.
   */
  private Action getAction(Card play, int target){
    System.out.println("Getting Action " + play + " At Target " + target);
    Action act = null;
    try{
      switch(play){
        case GUARD:
          act = Action.playGuard(myIndex, target, Card.values()[rand.nextInt(7)+1]);
          break;
        case PRIEST:
          act = Action.playPriest(myIndex, target);
          break;
        case BARON:  
          act = Action.playBaron(myIndex, target);
          break;
        case HANDMAID:
          act = Action.playHandmaid(myIndex);
          break;
        case PRINCE:  
          act = Action.playPrince(myIndex, target);
          break;
        case KING:
          act = Action.playKing(myIndex, target);
          break;
        case COUNTESS:
          act = Action.playCountess(myIndex);
          break;
        case PRINCESS:
          act = Action.playPrincess(myIndex);
          break;
        default:
          act = null;//never play princess
      }
    } catch(IllegalActionException e){System.err.println(e);}
    return act;
  }

  public static void main(String[] args) {
    System.out.println("Hello");
  }

}

class Players {
  private double[][] cardProbabilities;
  private int[] cardsUnseen;
  private int[] playerNumbers;
  public Players(int myIndex){
    cardProbabilities = new double[3][8];
    cardsUnseen = new int[]{5, 2, 2, 2, 2, 1, 1, 1};
    playerNumbers = new int[3];
    int count = 0;
    for(int i = 0; i < 4; i++){
      if(i != myIndex && count < 3) {
        playerNumbers[count] = i;
        count++;
      }
    }

  }

  public void updateProbabilities(){
    int totalUnseenCards = 0;
    for(int cardType : cardsUnseen){
      totalUnseenCards += cardType;
    }
    for(int i = 0; i < cardsUnseen.length; i++){
      for(int j = 0; j < cardProbabilities.length; j++){
        cardProbabilities[j][i] = (double) cardsUnseen[i]/(double) totalUnseenCards;
      }
    }
    System.out.println(Arrays.deepToString(cardProbabilities));
  }
}