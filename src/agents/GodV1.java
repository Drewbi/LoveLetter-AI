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
  private CardCount probabilities;

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
    probabilities = new CardCount(myIndex);
    probabilities.updateOwn(current.getCard(myIndex).value());
    probabilities.updateProbabilities();
  }

  /**
   * Method called when any agent performs an action. 
   * @param act the action an agent performs
   * @param results the state of play the agent is able to observe.
   * **/
  public void see(Action act, State results){
    current = results;
    for(int i = 0; i < 4; i++){
      if(i == myIndex) continue;
      if(current.getCard(i) != null){
        probabilities.updateKnown(current.getCard(i).value(), i);
      } else {
        probabilities.discardKnown(i);
      }
    }
    for(int i = 0; i < 4; i++) if(current.eliminated(i) && i != myIndex) probabilities.playerEliminated(i);
    probabilities.updateUnseen(current.unseenCards());
  }

  /**
   * Perform an action after drawing a card from the deck
   * @param c the card drawn from the deck
   * @return the action the agent chooses to perform
   * */
  public Action playCard(Card c){
    probabilities.updateOwn(c.value());
    Action act = null;
    Card play;
    Card card1 = c;
    Card card2 = current.getCard(myIndex);
    if(card1.toString() == "Handmaid") {
      play = card1;
    } else if(card2.toString() == "Handmaid") {
      play = card1;
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
    int target = rand.nextInt(4);
    if(current.handmaid(target) || current.eliminated(target) || target == myIndex){
      for(int i = 0; i < 4; i++){
        if(!current.handmaid(i) && !current.eliminated(i) || i != myIndex){
          target = i;
        }
      }
    }
    act = getAction(play, target);
    if(!current.legalAction(act, c)){
      System.out.println("Action: " + act +  " is not valid");
      System.out.println("Hand is " + card1 + " and " + card2);
      String protstr = current.handmaid(target)?"":"not ";
      System.out.println("Target is " + protstr + "protected");
      String elimstr = current.eliminated(target)?"":"not ";
      System.out.println("Target is " + elimstr + "eliminated");
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
    Action act = null;
    try{
      switch(play){
        case GUARD:
          int[] protectedPlayers = new int[3];
          int count = 0;
          for(int i = 0; i < 4; i++){
            if(i == myIndex) continue;
            if(current.handmaid(i)) {
               protectedPlayers[count] = i;
            }
            else protectedPlayers[count] = -1;
            count++;
          }
          int[] guess = probabilities.bestGuardGuess(protectedPlayers);
          act = Action.playGuard(myIndex, guess[0], Card.values()[guess[1]]);
          break;
        case PRIEST:
          if(probabilities.cardKnown(target) || current.handmaid(target)) {
            for(int i = 0; i < 4; i++) {
              if(!current.eliminated(i) && !current.handmaid(i) && i != myIndex) {
                if(!probabilities.cardKnown(i)) {
                  target = i;
                }
              }
            }
          }
          System.out.println("Priest ---------------------------------------");
          act = Action.playPriest(myIndex, target);
          break;
        case BARON:
          
          act = Action.playBaron(myIndex, target);
          break;
        case HANDMAID:
          act = Action.playHandmaid(myIndex);
          break;
        case PRINCE:  
          if(current.handmaid(target)){
            int maxTarget = -1;
            int maxCard = 0;
            for(int i = 0; i < 4; i++) {
              if(!current.eliminated(i) && !current.handmaid(i) && i != myIndex) {
                if(probabilities.knownCards[i] >= maxCard) {
                  maxTarget = i;
                }
              }
            }
            target = maxTarget;
          } // Maybe add self targeting in some situation :/
          if(target == -1) target = myIndex;
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

}

class CardCount {
  private double[][] cardProbabilities;
  private int[] cardsUnseen;
  private int[] playerNumbers;
  public int[] knownCards;

  public CardCount(int myIndex){
    cardProbabilities = new double[3][8];
    cardsUnseen = new int[]{5, 2, 2, 2, 2, 1, 1, 1};
    playerNumbers = new int[3];
    knownCards = new int[3];
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
    for(int i = 0; i < 3; i++){
      for(int j = 0; j < 8; j++){
        if(playerNumbers[i] == -1) cardProbabilities[i][j] = 0;

        else cardProbabilities[i][j] = (double) cardsUnseen[j]/(double) totalUnseenCards;
      }
    }
    for(int i = 0; i < 3; i++){
      int known = knownCards[i];
      if(known > 0){
        for(int j = 0; j < 8; j++){
          cardProbabilities[i][j] = 0;
        }
        cardProbabilities[i][known - 1] = 1;
      }
    }
  }

  public void updateOwn(int card){
    cardsUnseen[card - 1]--;
  }

  public void updateKnown(int card, int player){
    int playerIndex = getPlayerIndex(player);
    knownCards[playerIndex] = card;
    cardsUnseen[card - 1]--;
  }

  public Boolean cardKnown(int player){
    int playerIndex = getPlayerIndex(player);
    System.out.println(Arrays.toString(knownCards));
    return knownCards[playerIndex] != 0;
  }

  public void discardKnown(int player){
    int playerIndex = getPlayerIndex(player);
    if(playerIndex != -1) knownCards[playerIndex] = 0;
  }

  public void updateUnseen(Card[] unseen){
    for(int i = 0; i < 8; i++) cardsUnseen[i] = 0;
    for(Card card : unseen){
      cardsUnseen[card.value() - 1]++;
    }
  }

  public void playerEliminated(int player){
    int playerIndex = getPlayerIndex(player);
    if(playerIndex != -1){
      knownCards[playerIndex] = 0;
      playerNumbers[playerIndex] = -1;
      for(int i = 0; i < 8; i++){
        cardProbabilities[playerIndex][i] = 0;
      }
    }
  }

  private int getPlayerIndex(int playerNum){
    for(int i = 0; i < 3; i++){
      if(playerNum == playerNumbers[i]) return i;
    }
    return -1;
  }

  public int[] bestGuardGuess(int[] protectedPlayers){
    updateProbabilities();
    int bestTarget = -1;
    int bestCard = -1;
    double highestProb = 0.0;
    for(int i = 0; i < 3; i++){
      Boolean valid = true;
      for(int player : protectedPlayers) {
        if(player == playerNumbers[i]) {
          valid = false;
          break;
        }

      }
      if(!valid) continue;
      for(int j = 1; j < 8; j++){
        if(cardProbabilities[i][j] >= highestProb){
          bestTarget = i;
          bestCard = j;
          highestProb = cardProbabilities[i][j];
        }
      }
    }
    if(bestTarget == -1){
      for(int i = 0; i < 3; i++){
        if(playerNumbers[i] != -1) {
          bestTarget = i;
          bestCard = 7;
          break;
        }
      }
    }
    int[] bestGuess = new int[2];
    bestGuess[0] = playerNumbers[bestTarget];
    bestGuess[1] = bestCard;
    return bestGuess;
  }
}