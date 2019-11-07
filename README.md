## LoveLetter
LoveLetter java code and interfaces for CITS3001 AI unit at UWA.

### About
The project required us to make an agent to play the game Love Letter. I built two implementations, GodV1 and GodV2 which can be found in the Agents folder. 

#### GodV1
This agent implements a simple heuristic of playing the lowest legal card available and making target decisions by card counting and keeping track of unplayed cards. This agent had a 50-60% win rate against random agents.

#### GodV2
This agent implements a Monte Carlo Tree Search (MCTS) and samples a range of potential states to see which move is most favorable. The issue with this is when generating a guess of the current state, significant error is introduced and the less accurate the predictions about the opponents cards are, the more useless the tree searching is to inform the agent. This agent had at worst a 0% win rate which was able to be improved to 11% by tweaking depth and exploration parameters on the search. Both very impressive results considering the enemy was selecting moves at random. 

