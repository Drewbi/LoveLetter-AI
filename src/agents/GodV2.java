package agents;
import loveletter.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.lang.Math;

/**
 * Monte Carlo Tree Search Agent
 * Uses code from the provided game source but
 * adapted to provide constructable states partway through a game
 * */
public class GodV2 implements Agent{
  private State current;
  private int myIndex;

  public GodV2(){
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
    Action act;
    System.out.println("Hand is " + c + " and " + current.getCard(myIndex));
    if(c == Card.COUNTESS && (current.getCard(myIndex) == Card.KING || current.getCard(myIndex) == Card.PRINCE)){
      try {
        act = Action.playCountess(myIndex);
        return act;
      } catch (IllegalActionException e){
        System.out.println(e);
      }
    } else if(current.getCard(myIndex) == Card.COUNTESS  && (c == Card.KING || c == Card.PRINCE)){
      try {
        act = Action.playCountess(myIndex);
        return act;
      } catch (IllegalActionException e){
        System.out.println(e);
      }
    }
    MCTS monte;
    try {
      monte = new MCTS(12, 1000000, 0.3, current, c, myIndex);
      MCTSNode bestNode = monte.ISMCTS();
      act = bestNode.getAction();
    } catch(IllegalActionException e){
      System.out.println("Move failed " + e);
      RandomAgent randSub = new RandomAgent();
      randSub.newRound(current);
      act = randSub.playCard(c); // I subsitute actions for you
    }
    return act;
  }
}

/**
 * Nodes for use in the Monte Carlo tree search
 */
class MCTSNode {
  private MCTSNode parent;
  private MCTSNode[] children; 
  private PseudoState determinisation; // Randomly generated state for use with a single nodd
  private int depth; // Current depth of node
  private Action act; // Action associated with arriving at this node
  private int wins; // Number of wins backpropagated through this node
  private int tries; // Number of simulations backpropagated through this node
  private int availablility; // Number of times this node has been considered for selection
  private boolean leaf; // Whether this node is terminal

  // Construct a node
  public MCTSNode(int depth, int maxDepth, MCTSNode parent, PseudoState state, Action act){
    this.depth = depth;
    this.parent = parent;
    determinisation = state;
    this.act = act;
    children = new MCTSNode[8];
    leaf = state.roundOver() || depth == maxDepth;
  }

 // Construct a root node
  public MCTSNode(PseudoState state){
    depth = 0;
    parent = null;
    act = null;
    determinisation = state;
    children = new MCTSNode[8];
    leaf = false;
  }

  // Methods to access object fields
  public int getPlayer(){return determinisation.nextPlayer();}

  public PseudoState getDeterm(){return determinisation;}

  public MCTSNode getParent(){return parent;}

  public boolean isLeaf(){return leaf;}

  public Action getAction(){return act;}
  
  public int getWins(){return wins;}
  
  public int getTries(){return tries;}

  public int getAvailable(){return availablility;}

  public int getDepth(){return depth;}

  public void seen(){ availablility++;}
  
  public MCTSNode[] getChildren(){ return children;}

  // Creates a child node by cloning a state and using it to generate a node one level down
  public MCTSNode createChild(Action act, int maxDepth) throws IllegalActionException {
    PseudoState expanded;
    expanded = determinisation.expand(act, determinisation.getNewCard());
    MCTSNode child = new MCTSNode(depth + 1, maxDepth, this, expanded, act);
    int cardVal = act.card().value();
    children[cardVal - 1] = child;
    return child;
  }

  public boolean hasChild(int cardVal){
    return children[cardVal - 1] != null;
  }

  public MCTSNode getChild(int cardVal){
  return children[cardVal - 1];
  }

  // Potential method for simulating multiple playouts for potentially better 
  // consistency at a cost of perfomance
  public int simulateMulti(int numSimulations, int playerIndex){
    int wins = 0;
    for(int i = 0; i < numSimulations; i++) {
      if(determinisation.playOut(playerIndex)) wins++;
    }
    if((double) wins / (double) numSimulations < 0.5) return 0;
    else return 1;
  }

  // Play out the state at a given node and have it return if the player won or not
  public int simulate(int playerIndex){
    if(determinisation.playOut(playerIndex)) return 1;
    else return 0;
  }

  // Work up the tree of nodes adding one to the attempts and if applicable the wins
  public void backProp(int win){
    MCTSNode current = parent;
    current.wins += win;
    current.tries++;
    if(current.parent != null){
      current.backProp(win);
    }

  }
}

// Class for running the monte carlo search
class MCTS {
  private MCTSNode root;
  private int maxDepth;
  private int maxIterations;
  private double expConst;
  private int playerIndex;

  // Construct the tree search with variable constants like depth, iterations and exploration constant
  public MCTS(int maxDepth, int maxIterations, double expConst, State startState, Card c, int playerIndex){
    this.maxDepth = maxDepth;
    this.expConst = expConst;
    this.playerIndex = playerIndex;
    this.maxIterations = maxIterations;
    PseudoState startDeterm = new PseudoState(startState, c);
    root = new MCTSNode(startDeterm);
  }

  // Runs the Information set monte carlo tree search
  public MCTSNode ISMCTS() throws IllegalActionException {
    int numIterations = 0;
    while (numIterations < maxIterations){
      MCTSNode selectedNode;
      try {
        selectedNode = select();
      } catch (IllegalActionException e) {
        throw e;
      }
      int win = selectedNode.simulate(playerIndex);
      selectedNode.backProp(win);
      numIterations++;
      if(numIterations % 100000 == 0) System.out.println("Iteration: " + numIterations);
    }
    int maxTries = 0;
    int bestChild = -1;
    for(int i = 1; i <= 8; i ++){
      if(root.hasChild(i)) {
        int childTries = root.getChild(i).getTries();
        if(childTries >= maxTries) {
          maxTries = childTries;
          bestChild = i;
        }
      }
    }
    if(bestChild == -1) System.out.println("No child found");
    return root.getChild(bestChild);
  }

  // Works down the tree based on the principles of the monte carlo tree search
  private MCTSNode select() throws IllegalActionException {
    MCTSNode current = root;
    while (!current.isLeaf()){
      PseudoState currentDeterm = current.getDeterm().cloneState();

      // Get the current players hand
      PseudoCard card1 = currentDeterm.getNewCard();
      PseudoCard card2 = currentDeterm.getCard(current.getPlayer());
      
      // Get the agent from state
      int currentPlayerIndex = current.getPlayer();
      PseudoAgent currentPlayer = currentDeterm.getPlayer(currentPlayerIndex);
      
      // Pick one of the cards to try first
      PseudoCard playFirst = currentPlayer.pickRandomCard(card1, card2);
      PseudoCard playSecond = playFirst == card1 ? card2 : card1;
      
      // Generate a player state to use to pick a card
      PseudoState playerState = currentDeterm.playerState(currentPlayerIndex);
      currentPlayer.newRound(playerState);

      Action act1 = currentPlayer.playCard(playFirst);
      Action act2 = currentPlayer.playCard(playSecond);

      if(act1 == null & act2 == null) throw new IllegalActionException("Both actions are illegal: " + act1 + "|" + act2);
      if(act1 != null && !current.hasChild(act1.card().value())){
        try {
          return current.createChild(act1, maxDepth);
        } catch(IllegalActionException e){
          throw e;
        }
      } else if(act2 != null && !current.hasChild(act2.card().value())){
        try {
          return current.createChild(act2, maxDepth);
        } catch(IllegalActionException e){
          throw e;
        }
      } else {
        int[] cardVals = {card1.value(), card2.value()};
        for(int i = 0; i < 2; i++){
          current.getChild(cardVals[i]).seen();
        }
        current = pickISUCT(current.getChild(cardVals[0]), current.getChild(cardVals[1]));
        try {
          currentDeterm = currentDeterm.expand(current.getAction(), current.getDeterm().getNewCard());
        } catch (IllegalActionException e) {
          throw e;
        }
      }
    }
    return current;
  }

  // Choose nodes based on the ISUCT algorithm
  private MCTSNode pickISUCT(MCTSNode node1, MCTSNode node2){
    double node1Score = ISUCT(node1.getWins(), node1.getTries(), node1.getParent().getAvailable(), node1.getAvailable());
    double node2Score = ISUCT(node2.getWins(), node2.getTries(), node2.getParent().getAvailable(), node2.getAvailable());
    return node1Score > node2Score ? node1 : node2;
  }

  // Actual ISUCT formula
  private double ISUCT(int wins, int tries, int parentAvail, int avail){
    if(tries == 0) tries++;
    double winloss = (double)wins / (double)tries;
    if(avail == 0) avail++;
    return (winloss + (expConst * Math.sqrt(((2 * Math.log((double) parentAvail)) / (double) tries))));
  }


}

//            _____   _          _        
//          / ____| | |        | |       
//         | (___  | |_  __ _ | |_  ___ 
//         \___ \ | __|/ _` || __|/ _ \
//         ____) || |_| (_| || |_|  __/
//        |_____/ \__|\__,_| \__|\___|
//                     
//        Recycled from state class

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
  private int[] nextPlayer;
  private PseudoAgent[] randomAgents;
  private PseudoCard newCard;

  public PseudoState(Random random, State startState, PseudoAgent[] randomAgents, Card c) {
    newCard = PseudoCard.convertToPseudoCard(c);
    initRound(random, startState, randomAgents);
  }

  public PseudoState(State startState, Card c) {
    newCard = PseudoCard.convertToPseudoCard(c);
    Random random = new Random();
    PseudoAgent[] randomAgents = {new PseudoAgent(), new PseudoAgent(), new PseudoAgent(), new PseudoAgent()};
    initRound(random, startState, randomAgents);
  }

  public void initRound(Random random, State startState, PseudoAgent[] randomAgents) {
    this.randomAgents = randomAgents;
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
        PseudoCard convertedCard = PseudoCard.convertToPseudoCard(next);
        discards[i][j] = convertedCard;
        discardsStack[numDiscards] = convertedCard;
        j++;
        numDiscards++;
        discardCount[i]++;
      }
    }
    discardsStack[numDiscards++] = newCard;
    discardsStack[numDiscards] = PseudoCard.convertToPseudoCard(startState.getCard(startState.nextPlayer()));
    top = new int[1];
    top[0] = numDiscards;
    deck = PseudoCard.constructDeck(random, discardsStack);
    hand = new PseudoCard[numPlayers];
    handmaid = new boolean[numPlayers];
    known = new boolean[numPlayers][numPlayers];
    for(int i = 0; i<numPlayers; i++){
      if(startState.eliminated(i)) hand[i] = null;
      else {
        hand[i] = this.deck[top[0]++];
        handmaid[i] = startState.handmaid(i);
      }
      known[i][i] = true;
    }
    nextPlayer=new int[1];
  }

  public void displayState(){
    System.out.println("player: " + player);
    System.out.println("discards: " + Arrays.deepToString(discards));
    System.out.println("discardCount: " + Arrays.toString(discardCount));
    System.out.println("hand: " + Arrays.toString(hand));
    System.out.println("deck: " + Arrays.toString(deck));
    System.out.println("top: " + Arrays.toString(top));
    System.out.println("known: " + Arrays.deepToString(known));
    System.out.println("handmaid: " + Arrays.toString(handmaid));
    System.out.println("nextPlayer: " + Arrays.toString(nextPlayer));
    System.out.println("Agents: " + Arrays.toString(randomAgents));
  }

  public Boolean playOut(int playerIndex){
    int winner=0;
    int numPlayers = 4;
    PseudoState gameState = cloneState();//the game state
    PseudoState[] playerStates = new PseudoState[numPlayers];
    try{
      // Create the game states
      for(int i = 0; i<numPlayers; i++){
        playerStates[i] = gameState.playerState(i);
        randomAgents[i].newRound(playerStates[i]);
      }
      // Play the round
      while(!gameState.roundOver()){
        PseudoCard topCard = gameState.drawCard(); 
        Action act = randomAgents[gameState.nextPlayer()].playRandomCard(topCard);
        try{
          gameState.update(act,topCard);
        } catch(IllegalActionException e){ // Hopefully this shouldn't happen
        System.out.println(e);
        System.out.println("Stopping...");
      }
      for(int p = 0; p<numPlayers; p++) randomAgents[p].see(act,playerStates[p]);
    }
    winner = gameState.roundWinner();
      return winner == playerIndex;
    }catch(IllegalActionException e){
      System.out.println("Something has gone wrong.");
      e.printStackTrace();
      return null;
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    PseudoState cloned = (PseudoState)super.clone();
    cloned.discards = (PseudoCard[][])cloned.discards.clone(); 
    cloned.discardCount = (int[])cloned.discardCount.clone(); 
    cloned.hand = (PseudoCard[])cloned.hand.clone(); 
    cloned.deck = (PseudoCard[])cloned.deck.clone(); 
    cloned.top = (int[])cloned.top.clone(); 
    cloned.known = (boolean[][])cloned.known.clone(); 
    cloned.handmaid = (boolean[])cloned.handmaid.clone(); 
    cloned.nextPlayer = (int[])cloned.nextPlayer.clone(); 
    return cloned;
  }

  public PseudoState cloneState(){
    try{
      PseudoState s = (PseudoState)this.clone();
      return s;
    }catch(CloneNotSupportedException e){
      e.printStackTrace();
      return null;
    }
  }

  public PseudoState playerState(int player) throws IllegalActionException{
    if(this.player!=-1) throw new IllegalActionException("Operation not permitted in player's state.");
    if(player<0 || numPlayers<=player) throw new IllegalArgumentException("Player out of range.");
    try{
      PseudoState s = (PseudoState)super.clone();
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
      legalAction(act.player(), act.target(), PseudoCard.convertToPseudoCard(act.card()), drawn);
    }
    catch(IllegalActionException e){
      return false;
    }
    return true;
  }

  public PseudoCard drawCard(){
    return deck[top[0]++];
  }

  public PseudoState expand(Action act, PseudoCard card) throws IllegalActionException {
    PseudoState expanded = cloneState();
    try {
      expanded.update(act, card);
    } catch (IllegalActionException e){
      throw e;
    }
    return expanded;
  }

  public String update(Action act, PseudoCard card) throws IllegalActionException{
    if(player!=-1){
      throw new IllegalActionException("Operation not permitted in player's state.");
    }
    int a = act.player();//actor
    int t = act.target();//target
    PseudoCard c = PseudoCard.convertToPseudoCard(act.card());
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
        ret+=guardAction(a,t,PseudoCard.convertToPseudoCard(act.guess()));
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
        System.out.println("Not a valid action");
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

  public PseudoAgent getPlayer(int playerIndex){
    return randomAgents[playerIndex];
  }

  public PseudoCard getNewCard(){ return newCard; }

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
    return randomAgents[playerIndex].toString()+"("+playerIndex+")";
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


//                                        _   
//             /\                        | |  
//            /  \    __ _   ___  _ __  | |_ 
//          / /\ \  / _` | / _ \| '_ \ | __|
//         / ____ \| (_| ||  __/| | | || |_ 
//       /_/    \_\\__, | \___||_| |_| \__|
//                  __/ |                  
//                 |___/                           

class PseudoAgent {

  private Random random;
  private PseudoState current;
  private int agentIndex;
  private boolean[] availableTargets;
  private double[][] cardProb;
  private int[] unseenCards;

  public PseudoAgent(){
    random  = new Random();
    availableTargets = new boolean[4];
    cardProb = new double[4][8];
    unseenCards = new int[]{5, 2, 2, 2, 2, 1, 1, 1};
    updateProbabilities();
  }

  public PseudoAgent(Random random){
    this.random  = random;
    availableTargets = new boolean[4];
  }

  public String toString(){return "NotSoRandom";}

  public void newRound(PseudoState start){
    current = start;
    agentIndex = current.getPlayerIndex();
    updateKnown();
    getAvailableTargets();
  }

  public void see(Action act, PseudoState results){
    current = results;
    updateKnown();
    getAvailableTargets();
  }

  private void updateKnown(){
    for(int i = 0; i < 4; i++){
      if(i == agentIndex) continue;
      PseudoCard card = current.getCard(i);
      if(card != null && cardProb[i][card.value()-1] != 1.0){
        for(int j = 0; j < 8; j++) cardProb[i][j] = 0.0;
        cardProb[i][card.value()-1] = 1.0;
        unseenCards[card.value()-1]--;
      }
    }
  }

  private void updateProbabilities(){
    int totalUnseenCards = 0;
    for(int i = 0; i < 8; i++){
      totalUnseenCards += unseenCards[i];
    }
    for(int i = 0; i < 8; i++){
      for(int j = 0; j < 4; j++){
        if(j == agentIndex) continue;
        cardProb[j][i] = (double)unseenCards[i] / (double)totalUnseenCards;
      }
    }
  }

  private void getAvailableTargets(){
    for(int i = 0; i < 4; i++){
      if(i == agentIndex || current.eliminated(i) || current.handmaid(i)) availableTargets[i] = false;
      else availableTargets[i] = true;
    }
  }

  private boolean targetsAvailable(){
    for(int i = 0; i < 4; i++) if(availableTargets[i]) return true;
    return false;
  }

  public PseudoCard pickRandomCard(PseudoCard c, PseudoCard d){
    if(random.nextDouble() < 0.5) return c;
    else return d;
  }

  public int getRandomTarget(PseudoCard c){
    int target;
    switch(c){
      case GUARD:
        target = getTarget();
        break;
      case PRIEST:
        target = getTarget();
        break;
      case BARON:  
        target = getTarget();
        break;
      case PRINCE:  
        target = getRandomPrinceTarget();
        break;
      case KING:
        target = getTarget();
        break;
      default:
        target = -1; // Princess, Handmaid and Countess don't target
    }
    return target;
  }

  private int getTarget(){
    getAvailableTargets();
    
    // Try and find someone available
    for(int i = 0; i < 4; i++) {
      if(availableTargets[i]) return i;
    }
    // If not try and find someone not eliminated
    for(int i = 0; i < 4; i++){
      if(i == agentIndex) continue;
      if(!current.eliminated(i)) return i;
    }
    System.out.println("Get target has failed");
    return agentIndex;
  }

  private int getRandomPrinceTarget(){
    Random rand = new Random();
    int i = rand.nextInt(3);
    int j = agentIndex;
    while(i >= 0){
      for(int k = 0; k < 4; k++){
        if(availableTargets[k]){
          j = k;
          i--;
        }
        if(i == 0) break;
      }
      i--;
    }
    return j;
  }

  public int getBestTarget(PseudoCard c, PseudoCard otherCard){
    int target;
    switch(c){
      case GUARD:
        target = getBestGuardTarget();
        break;
      case PRIEST:
        target = getBestPriestTarget();
        break;
      case BARON:  
        target = getBestBaronTarget(otherCard.value());
        break;
      case PRINCE:  
        target = getBestPrinceTarget();
        break;
      case KING:
        target = getBestKingTarget(otherCard.value());
        break;
      default:
        target = -1; // Princess, Handmaid and Countess don't target
    }
    return target;
  }

  private int getBestGuardTarget(){
    return getBestGuardMove()[0];
  }

  private int getBestGuardCard(){
    return getBestGuardMove()[1];
  }

  private int[] getBestGuardMove(){
    int[] guess = new int[2];
    double prob = 0;
    for(int i = 0; i < 4; i++){
      if(!availableTargets[i]) continue;
      for(int j = 1; j < 8; j++){
        if(cardProb[i][j] >= prob) {
          prob = cardProb[i][j];
          guess[0] = i;
          guess[1] = j+1;
        }
      }
    }
    if(prob == 0 && !targetsAvailable()) {
      guess[0] = (agentIndex + 1) % 4;
      guess[1] = 8;
    }
    else if(prob == 0) for(int i = 0; i < 4; i++) if(availableTargets[i]) {
      guess[0] = i;
      guess[1] = 8;
    }
    return guess;
  }

  private int getBestPriestTarget(){
    int target = (agentIndex + 1) % 4;
    double prob = 1;
    for(int i = 0; i < 4; i++){
      if(!availableTargets[i]) continue;
      for(int j = 0; j < 8; j++){
        if(cardProb[i][j] == 0) continue;
        else if(cardProb[i][j] <= prob) {
          target = i;
          prob = cardProb[i][j];
        }
      }
    }
    if(prob == 1 && !targetsAvailable()) target = (agentIndex + 1) % 4;
    else if(prob == 1) for(int i = 0; i < 4; i++) if(availableTargets[i]) {
      target = i;
    }
    return target;
  }

  private int getBestBaronTarget(int cardVal){
    int target = (agentIndex + 1) % 4;
    double prob = 0.3;
    int lowestCard = 9;
    for(int i = 0; i < 4; i++){
      if(!availableTargets[i]) continue;
      for(int j = 0; j < 8; j++){
        if(cardProb[i][j] >= prob && j + 1 <= lowestCard) {
          target = i;
          lowestCard = j + 1;
          prob = cardProb[i][j];
        }
        else if(cardProb[i][j] == 1 && j + 1 < cardVal) {
          target = i;
          lowestCard = 0;
        }
      }
    }
    if(lowestCard == 9 && !targetsAvailable()) target = (agentIndex + 1) % 4;
    else if(lowestCard == 9) for(int i = 0; i < 4; i++) if(availableTargets[i]) {
      target = i;
    }
    return target;
  }

  private int getBestPrinceTarget(){
     int target = agentIndex;
    double prob = 0;
    int highestCard = 0;
    for(int i = 0; i < 4; i++){
      if(!availableTargets[i]) continue;
      for(int j = 0; j < 8; j++){
        if(cardProb[i][j] >= prob && j + 1 >= highestCard) {
          target = i;
          highestCard = j + 1;
          prob = cardProb[i][j];
        }
      }
    }
    if(highestCard == 0 && !targetsAvailable()) target = agentIndex;
    else if(highestCard == 0) for(int i = 0; i < 4; i++) if(availableTargets[i]) {
      target = i;
    }
    return target;
  }

  private int getBestKingTarget(int cardVal){
    int target = (agentIndex + 1) % 4;
    double prob = 0.6;
    int highestCard = 0;
    for(int i = 0; i < 4; i++){
      if(!availableTargets[i]) continue;
      for(int j = 0; j < 8; j++){
        if(cardProb[i][j] >= prob && j + 1 >= highestCard) {
          target = i;
          highestCard = j + 1;
          prob = cardProb[i][j];
        }
      }
    }
    if(highestCard == 0 && !targetsAvailable()) target = (agentIndex + 1) % 4;
    else if(highestCard == 0) for(int i = 0; i < 4; i++) if(availableTargets[i]) {
      target = i;
    }
    return target;
  }

  public Action playCard(PseudoCard c){
    Random rand = new Random();
    Action act = null;
    PseudoCard otherCard = current.getCard(agentIndex);
    updateKnown();
    getAvailableTargets();
    int target = getBestTarget(c, otherCard);
    // int target = getRandomTarget(c);
    try{
      switch(c){
        case GUARD:
          act = Action.playGuard(agentIndex, target, Card.values()[getBestGuardCard()-1]);
          // act = Action.playGuard(agentIndex, target, Card.values()[rand.nextInt(7)+1]);
          break;
        case PRIEST:
          act = Action.playPriest(agentIndex, target);
          break;
        case BARON:  
          act = Action.playBaron(agentIndex, target);
          break;
        case HANDMAID:
          act = Action.playHandmaid(agentIndex);
          break;
        case PRINCE:  
          act = Action.playPrince(agentIndex, target);
          break;
        case KING:
          act = Action.playKing(agentIndex, target);
          break;
        case COUNTESS:
          act = Action.playCountess(agentIndex);
          break;
        case PRINCESS:
          act = Action.playPrincess(agentIndex);
          break;
        default:
          act = null;
      }
      if(act == null) throw new IllegalActionException("No legal action found");
    }catch(IllegalActionException e){/*do nothing*/}
    return act;
  }

  public Action playRandomCard(PseudoCard c){
    Action act = null;
    PseudoCard play;
    while(!current.legalAction(act, c)){
      if(random.nextDouble() < 0.5) play = c;
      else play = current.getCard(agentIndex);
      int target = random.nextInt(4);
      try{
        switch(play){
          case GUARD:
            act = Action.playGuard(agentIndex, target, Card.values()[random.nextInt(7)+1]);
            break;
          case PRIEST:
            act = Action.playPriest(agentIndex, target);
            break;
          case BARON:  
            act = Action.playBaron(agentIndex, target);
            break;
          case HANDMAID:
            act = Action.playHandmaid(agentIndex);
            break;
          case PRINCE:  
            act = Action.playPrince(agentIndex, target);
            break;
          case KING:
            act = Action.playKing(agentIndex, target);
            break;
          case COUNTESS:
            act = Action.playCountess(agentIndex);
            break;
          default:
            act = null;//never play princess
        }
      }catch(IllegalActionException e){/*do nothing*/}
    }
    return act;
  }
}

//             _____                 _ 
//           / ____|               | |
//          | |      __ _  _ __  __| |
//         | |     / _` || '__|/ _` |
//         | |____| (_| || |  | (_| |
//         \_____|\__,_||_|   \__,_|
//
//         Recycled from card class  

enum PseudoCard {

  GUARD(1,"Guard",5),
  PRIEST(2,"Priest",2),
  BARON(3,"Baron",2),
  HANDMAID(4,"Handmaid",2),
  PRINCE(5,"Prince",2),
  KING(6,"King",1),
  COUNTESS(7,"Countess",1),
  PRINCESS(8,"Princess",1);

  private int value; //numerical value of card
  private String name; //String description of card
  private int count; //number of cards in the deck

  private PseudoCard(int value, String name, int count){
    this.value = value;
    this.name = name;
    this.count = count;
  }

  public int value(){return value;}

  public String toString(){return name;}
  
  public int count(){return count;}
  
  public static PseudoCard[] constructDeck(Random rand, PseudoCard[] discards){
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

  public static PseudoCard convertToPseudoCard(Card c){
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

  public static Card convertFromPseudoCard(PseudoCard c){
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
}

// Congratulations for making it down here