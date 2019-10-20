package agents;
import loveletter.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * Monte Carlo Tree Search Agent
 * */
public class GodV2 implements Agent{
  private Random rand;
  private State current;
  private int myIndex;

  public GodV2(){
    rand  = new Random();
    System.out.println("Initialising GodV2");
  }

  public String toString(){return "∆GodV2∆";}

  public void newRound(State start){
    current = start;
    myIndex = current.getPlayerIndex();
  }

  public void see(Action act, State results){
    current = results;
  }

  public Action playCard(Card c){
    RandomAgent randSub = new RandomAgent();
    PseudoRandomAgent[] simAgents = {new PseudoRandomAgent(), new PseudoRandomAgent(), new PseudoRandomAgent(), new PseudoRandomAgent()};
    PseudoState simState = new PseudoState(rand, current, simAgents);
    GameSimulation gameSim = new GameSimulation(simState, myIndex, simAgents);
    gameSim.runSimulation();
    randSub.newRound(current);
    System.out.println("∆ Simulation ended ----------");
    Action act = randSub.playCard(c); // Get random card for testing
    return act;
  }
}

class GameSimulation {
  private PseudoState state;
  private Random random;
  private PseudoRandomAgent[] agents;
  private int playerIndex;

  public GameSimulation(PseudoState startState, int playerIndex, PseudoRandomAgent[] agents){
    System.out.println("∆ Starting game sim -----------");
    this.state = startState;
    this.random = new Random();
    this.playerIndex = playerIndex;
    this.agents = agents;
  }

  public Boolean runSimulation(){
    System.out.println("∆ Running sim");
    int winner=0;
    int numPlayers = 4;
    PseudoState gameState = state;//the game state
    PseudoState[] playerStates = new PseudoState[numPlayers];
    try{
      // Create the game states
      for(int i = 0; i<numPlayers; i++){
        playerStates[i] = gameState.playerState(i);
        agents[i].newRound(playerStates[i]);
      }
      // Play the round
      while(!gameState.roundOver()){
        System.out.println("∆ Starting sim round");
        PseudoCard topCard = gameState.drawCard(); 
        Action act = agents[gameState.nextPlayer()].playCard(topCard);
        try{
          System.out.println("∆ " + gameState.update(act,topCard));
        } catch(IllegalActionException e){ // Hopefully this shouldn't happen
        System.out.println(e);
        System.out.println("Stopping...");
      }
      for(int p = 0; p<numPlayers; p++) agents[p].see(act,playerStates[p]);
    }
    winner = gameState.roundWinner();
    System.out.println("∆ Player " + winner + " has won the round");
      return winner == playerIndex;
    }catch(IllegalActionException e){
      System.out.println("Something has gone wrong.");
      e.printStackTrace();
      return null;
    }
  }
}


// --------------------------------------------------------------------------------------------
// ----------------------------- Taken from State Class ---------------------------------------
// --------------------------------------------------------------------------------------------


class PseudoState implements Cloneable{
  private int player;
  private int numPlayers;
  private PseudoCard[][] discards;
  private int[] discardCount;
  private PseudoCard[] hand;
  private PseudoCard[] deck;
  private int[] top;
  private boolean[][] known;
  private boolean[] handmaid;
  private Random random;
  private int[] nextPlayer;
  private PseudoRandomAgent[] agents;

  public PseudoState(Random random, State startState, PseudoRandomAgent[] randomAgents) {
    System.out.println("Initialising state");
    this.agents = randomAgents;
    this.random = random;
    player = -1;
    numPlayers = 4;
    PseudoCard[] discardsStack = new PseudoCard[16];
    discards = new PseudoCard[numPlayers][16];
    discardCount = new int[numPlayers];
    int numDiscards = 0;
    for(int i = 0; i < 4; i++){
      Iterator<Card> discard = startState.getDiscards(i);
      int j = 0;
      while(discard.hasNext()){
        Card next = discard.next();
        System.out.println("Adding card " + next + " to deck");
        PseudoCard convertedCard = convertToPseudoCard(next);
        discards[i][j] = convertedCard;
        discardsStack[numDiscards] = convertedCard;
        j++;
        numDiscards++;
      }
    }
    top = new int[1];
    top[0] = numDiscards;
    System.out.println("Raw discard stack");
    System.out.println(Arrays.toString(discardsStack));
    deck = PseudoCard.constructDeck(discardsStack);
    System.out.println("Constructed deck");
    System.out.println(Arrays.deepToString(deck));
    initRound();
    nextPlayer=new int[1];
  }

  public void initRound() {
    hand = new PseudoCard[numPlayers];
    handmaid = new boolean[numPlayers];
    known = new boolean[numPlayers][numPlayers];
    for(int i = 0; i<numPlayers; i++){
      hand[i] = this.deck[top[0]++];
      known[i][i] = true;
    }
  }

  public PseudoState playerState(int player) throws IllegalActionException{
    if(this.player!=-1) throw new IllegalActionException("Operation not permitted in player's state.");
    if(player<0 || numPlayers<=player) throw new IllegalArgumentException("Player out of range.");
    try{
      PseudoState s = (PseudoState)this.clone();
      s.player = player;
      return s;
    }catch(CloneNotSupportedException e){
      e.printStackTrace();
      return null;
    }
  }
    
  private void legalAction(int a, int t, PseudoCard c, PseudoCard drawn) throws IllegalActionException{
    if(hand[a]!=c && drawn!=c)
      throw new IllegalActionException("Player does not hold the played card");
    if(nextPlayer[0]!=a)//it must be the actors turn
      throw new IllegalActionException("Wrong player in action");
    if((hand[a]==PseudoCard.COUNTESS || drawn==PseudoCard.COUNTESS) && (c==PseudoCard.KING || c==PseudoCard.PRINCE))//if one of the cards is the countess, a king or prince may not be played.
      throw new IllegalActionException("Player must play the countess");
    if(t!=-1){//if this action has a target (1,2,3,5,6 cards)
      if(eliminated(t)) //you cannot target an eliminated player
        throw new IllegalActionException("The action's target is already eliminated");
      if(c==PseudoCard.PRINCE && a==t) return;//a player can always target themselves with the Prince.
      if(handmaid(t) && (!allHandmaid(a) || c==PseudoCard.PRINCE))//you cannot target a player with the handmaid
        throw new IllegalActionException("The action's target is protected by the handmaid");
    } 
  }

  public boolean legalAction(Action act, PseudoCard drawn){
    if(act ==null) return false;
    try{
      legalAction(act.player(), act.target(), convertToPseudoCard(act.card()), drawn);
    }
    catch(IllegalActionException e){return false;}
    return true;
  }

  public PseudoCard drawCard() throws IllegalActionException{
    if(player!=-1) throw new IllegalActionException("operation not permitted in player's state.");
    return deck[top[0]++];
  }

  public PseudoCard convertToPseudoCard(Card c){
    PseudoCard convertedCard;
    switch(c){
      case GUARD:
        convertedCard = PseudoCard.GUARD;
        break;
      case PRIEST:
        convertedCard = PseudoCard.PRIEST;
        break;
      case BARON:
        convertedCard = PseudoCard.BARON;
        break;
      case HANDMAID:
        convertedCard = PseudoCard.HANDMAID;
        break;
      case PRINCE:
        convertedCard = PseudoCard.PRINCE;
        break;
      case KING:
        convertedCard = PseudoCard.KING;
        break;
      case COUNTESS:  
        convertedCard = PseudoCard.COUNTESS;
        break;
      case PRINCESS:
        convertedCard = PseudoCard.PRINCESS;
        break;
      default: 
        return null;
    }
    return convertedCard;
  }

  public Card convertFromPseudoCard(PseudoCard c){
    Card convertedCard;
    switch(c){
      case GUARD:
        convertedCard = Card.GUARD;
        break;
      case PRIEST:
        convertedCard = Card.PRIEST;
        break;
      case BARON:
        convertedCard = Card.BARON;
        break;
      case HANDMAID:
        convertedCard = Card.HANDMAID;
        break;
      case PRINCE:
        convertedCard = Card.PRINCE;
        break;
      case KING:
        convertedCard = Card.KING;
        break;
      case COUNTESS:  
        convertedCard = Card.COUNTESS;
        break;
      case PRINCESS:
        convertedCard = Card.PRINCESS;
        break;
      default: 
        return null;
    }
    return convertedCard;
  }

  public String update(Action act, PseudoCard card) throws IllegalActionException{
    if(player!= -1)//Actions may only be executed from game states 
      throw new IllegalActionException("Method cannot be called from a player state");
    int a = act.player();//actor
    int t = act.target();//target
    PseudoCard c = convertToPseudoCard(act.card());
    discards[a][discardCount[a]++] = c;//put played card on the top of the acting player's discard pile, required for checking actions.
    try{
       legalAction(a,t,c,card);
    }catch(IllegalActionException e){
      discardCount[a]--;
      throw e;//reset discard top
    }
    if(c==hand[a]){//if the player played the card in their hand, insert the new card into their hand.
      hand[a]=card;
      for(int p = 0; p<numPlayers; p++)
        if(p!=a) known[p][a]=false;//rescind players knowledge if a known card was played
    }
    handmaid[a]=false;
    String ret = act.toString(name(a), t!=-1?name(t):"");
    switch(c){
      case GUARD://actor plays the guard
        ret+=guardAction(a,t,convertToPseudoCard(act.guess()));
        break;
      case PRIEST:
        ret+=priestAction(a,t);
        break;
      case BARON:
        ret+=baronAction(a,t);
        break;
      case HANDMAID:
        handmaid[a]=true;
        break;
      case PRINCE:
        ret+= princeAction(t);  
        break;
      case KING:
        ret+= kingAction(a,t);
        break;
      case COUNTESS:  
        //no update required
        break;
      case PRINCESS:
        ret+= princessAction(a);
        break;
      default: 
        throw new IllegalActionException("Illegal Action? Something's gone very wrong");
    }//end of switch
    if(roundOver()){//check for round over
      for(int i = 0; i<numPlayers; i++)
       for(int p = 0; p<numPlayers; p++) 
         known[i][p]=true;
      int winner = roundWinner();
      ret+="\nPlayer "+winner+" wins the round.";
      nextPlayer[0] = winner;
    }
    else{//set nextPlayer to next noneliminated player
      nextPlayer[0] = (nextPlayer[0]+1)%numPlayers; 
      while(eliminated(nextPlayer[0])) nextPlayer[0] = (nextPlayer[0]+1)%numPlayers; 
    }
    return ret;
  }

  private String guardAction(int a, int t, PseudoCard guess){
    if(allHandmaid(a))
      return "\nPlayer "+name(t)+" is protected by the Handmaid.";//no effect action
    else if(guess==hand[t]){//correct guess, target eliminated
      discards[t][discardCount[t]++] = hand[t];
      hand[t]=null;
      for(int p = 0; p<numPlayers; p++)known[p][t]=true;
      return "\nPlayer "+name(t)+" had the "+guess+" and is eliminated from the round";
    } 
    else return "\nPlayer "+name(t)+" does not have the "+guess;
  }

  private String priestAction(int a, int t){
    if(allHandmaid(a))
      return "\nPlayer "+name(t)+" is protected by the Handmaid.";//no effect action
    else known[a][t]=true;
    return "\nPlayer "+name(a)+" sees player "+name(t)+"'s card.";
  }

  private String baronAction(int a, int t){
    if(allHandmaid(a))
      return "\nPlayer "+name(t)+" is protected by the Handmaid.";//no effect action
    int elim = -1;
    if(hand[a].value()>hand[t].value()) elim = t;
    else if(hand[a].value()<hand[t].value()) elim = a;
    if(elim!=-1){
      discards[elim][discardCount[elim]++] = hand[elim];
      hand[elim]=null;
      for(int p = 0; p<numPlayers; p++) known[p][elim]=true;
      return "\nPlayer "+name(elim)+" holds the lesser card: "+discards[elim][discardCount[elim]-1]+", and is eliminated";
    }
    known[a][t]=true;
    known[t][a]=true;
    return "\n Both players hold the same card, and neither is eliminated.";
  }

  private String princeAction(int t){
    PseudoCard discard = hand[t];
    discards[t][discardCount[t]++] = discard;
    if(discard==PseudoCard.PRINCESS){
      hand[t]=null;
      for(int p = 0; p<numPlayers; p++) known[p][t]=true;
      return "\nPlayer "+name(t)+" discarded the Princess and is eliminated.";
    }
    hand[t]=deck[top[0]++];
    for(int p =0; p<numPlayers;p++) 
      if(p!=t)known[p][t]=false;
    return "\nPlayer "+name(t)+" discards the "+discard+".";
  }

  private String kingAction(int a, int t){
    if(allHandmaid(a))
      return "\nPlayer "+name(t)+" is protected by the Handmaid.";
    known[a][t]=true;
    known[t][a]=true;
    for(int p =0; p<numPlayers;p++){ 
      if(p!=t && p!=a){
        boolean tmp = known[p][t];
        known[p][t] = known[p][a];
        known[p][a] = tmp;
      }
    }
    PseudoCard tmp = hand[a];
    hand[a] = hand[t];
    hand[t] = tmp;
    return "\nPlayer "+name(a)+" and player "+name(t)+" swap cards.";
  }

  private String princessAction(int a){
    discards[a][discardCount[a]++] = hand[a];
    hand[a]=null;
    for(int p = 0; p< numPlayers; p++) known[p][a]=true;
    String outcome =  "\nPlayer "+name(a)+" played the Princess and is eliminated.";
    outcome += "\n Player "+name(a)+" was also holding the "+discards[a][discardCount[a]-1]+".";
    return outcome;
  }

  public int getPlayerIndex(){return player;}

  public java.util.Iterator<PseudoCard> getDiscards(int player){
    return new java.util.Iterator<PseudoCard>(){
      int p=player;
      int top=discardCount[player];
      public boolean hasNext(){return top>0;}
      public PseudoCard next() throws java.util.NoSuchElementException{
        if(hasNext()) return discards[p][--top];
        else throw new java.util.NoSuchElementException();
      }
    };
  }

  public PseudoCard getCard(int playerIndex){
    if(player==-1 || known[player][playerIndex]) return hand[playerIndex];
    else return null;
  }

  public boolean eliminated(int player){
    return hand[player]==null;
  }

  public int nextPlayer(){
    return nextPlayer[0];
  }

  public boolean handmaid(int player){
    if(player<0 || player >=numPlayers) return false;
    return handmaid[player];
  }

  public boolean allHandmaid(int player){
    boolean noAction = true;
    for(int i = 0; i<numPlayers; i++)
      noAction = noAction && (eliminated(i) || handmaid[i] || i==player); 
    return noAction;
  }

  private String name(int playerIndex){
    return agents[playerIndex].toString()+"("+playerIndex+")";
  }
 
  public int deckSize(){
    return 16-top[0];
  }

  public boolean roundOver(){
    int remaining = 0;
    for(int i=0; i<numPlayers; i++) 
      if(!eliminated(i)) remaining++;
    return remaining==1 || deckSize()<2;
  }

  public int roundWinner(){
    if(!roundOver()) return -1;
    int winner=-1;
    int topCard=-1;
    int discardValue=-1;
    for(int p=0; p<numPlayers; p++){
      if(!eliminated(p)){
        int dv = 0;
        for(int j=0; j<discardCount[p]; j++) dv+=discards[p][j].value();
        if(hand[p].value()>topCard || (hand[p].value()==topCard && dv>discardValue)){
          winner = p;
          topCard = hand[p].value();
          discardValue = dv;
        }
      }
    }
    return winner;
  }
}



// --------------------------------------------------------------------------------------------
// ----------------------------- Taken from Random Agent Class --------------------------------
// --------------------------------------------------------------------------------------------


class PseudoRandomAgent {

  private Random rand;
  private PseudoState current;
  private int myIndex;

  public PseudoRandomAgent(){
    rand  = new Random();
  }

  public String toString(){return "NotSoRandom";}

  public void newRound(PseudoState start){
    current = start;
    myIndex = current.getPlayerIndex();
  }

  public void see(Action act, PseudoState results){
    current = results;
  }

  public Action playCard(PseudoCard c){
    Action act = null;
    PseudoCard play;
    while(!current.legalAction(act, c)){
      if(rand.nextDouble()<0.5) play= c;
      else play = current.getCard(myIndex);
      int target = rand.nextInt(4);
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
          default:
            act = null;//never play princess
        }
      }catch(IllegalActionException e){/*do nothing*/}
    }
    return act;
  }
}

enum PseudoCard {
    GUARD(1 ,5),
    PRIEST(2 ,2),
    BARON(3 ,2),
    HANDMAID(4 ,2),
    PRINCE(5, 2),
    KING(6, 1),
    COUNTESS(7, 1),
    PRINCESS(8, 1);
  
    private int value; //numerical value of card
    private int count; //number of cards in the deck

    private PseudoCard(int value, int count){
      this.value = value;
      this.count = count;
    }

    public int value(){return value;}
    
    public int count(){return count;}
    
    public static PseudoCard[] constructDeck(java.util.Random rand, PseudoCard[] discards){
      PseudoCard[] deck = new PseudoCard[16];
      int deckIndex = 0;
      while(discards[deckIndex] != null) {
        deck[deckIndex] = discards[deckIndex];
        deckIndex++;
      }
      PseudoCard[] deckLeft = new PseudoCard[16-deckIndex];
      int j = 0;
      for(PseudoCard c: PseudoCard.values()) {
        int countLeft = c.count();
        for(int i = 0; i<deckIndex; i++){
          if(deck[i].value() == c.value()) countLeft--;
        }
        for(int i = 0; i<countLeft; i++){
          deckLeft[j++] = c;
        }
      }
      System.out.println(Arrays.toString(deckLeft));
      for(int i = 0; i<50; i++){//make two hundred random swaps of cards
        int index1 = rand.nextInt(deckLeft.length);
        int index2 = rand.nextInt(deckLeft.length);
        PseudoCard c = deckLeft[index1];
        deckLeft[index1]=deckLeft[index2];
        deckLeft[index2]=c;
      }
      for(int i = 0; i<deckLeft.length; i++) deck[i + deckIndex] = deckLeft[i];
      return deck;
    }

    public static PseudoCard[] constructDeck(PseudoCard[] discards){
      return constructDeck(new java.util.Random(), discards);
    }
}