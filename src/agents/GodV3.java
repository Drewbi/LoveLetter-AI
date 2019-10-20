package agents;

import java.util.Random;

import loveletter.Action;
import loveletter.Agent;
import loveletter.Card;
import loveletter.IllegalActionException;
import loveletter.State;

/**
 * A monte carlo tree search fueled by a genetic algorithm
 */
public class GodV3 implements Agent{

  private Random rand;
  private State current;
  private int myIndex;

  public GodV3(){
    rand  = new Random();
  }

  public String toString(){return "†GodV3†";}

  public void newRound(State start){
    current = start;
    myIndex = current.getPlayerIndex();
  }

  public void see(Action act, State results){
    current = results;
  }

  public Action playCard(Card c){
    RandomAgent randSub = new RandomAgent();
    Action act = randSub.playCard(c); // Get random card for testing
    return act;
  }
}