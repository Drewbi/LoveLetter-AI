package agents;
import loveletter.*;

/**
 * An interface for representing an agent in the game Love Letter
 * All agent's must have a 0 parameter constructor
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
    
  }
}


