/*
 * This program is pretty simple as long as you follow
 * the instructions of the specification
 * At first it bothers me that how to give the node which present two teams(like Boston-Toronto) an index
 * then i realized that it doesn't have to
 * just find it and add the edge related to the node into the network
 * 
 * What's more, I learned the use of Arrays.aslist, it's very useful when you need to return an Iterable
 * 
 * And the two public method, isEliminated() and certificateOfElimination()
 * the specification says it's bad design for the success of calling one method to depend on the client previously
 * calling another method
 * it bothers me at first
 * then I just use the isEliminated() to check if the certificateOfElimination() if null
 * by this means I connect these two method together
 * 
 *Author: maxi
 *Date:2015/12/6
 */

import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.ST;
import edu.princeton.cs.algs4.StdOut;

import java.util.Arrays;


public class BaseballElimination {

    private int N;
    private int s;
    private int t;
    private int[] w;
    private int[] l;
    private int[] r;
    private int[][] g;
    private ST<String, Integer> name; 
    private String[] iname;
    
    public BaseballElimination(
            String filename)                    // create a baseball division from given filename in format specified below
    {
        In in = new In(filename);
        this.N = in.readInt();
        w = new int[N];
        l = new int[N];
        r = new int[N];
        g = new int[N][N];
        name = new ST<String, Integer>();
        iname = new String[N];
        
        for (int i = 0; i < N; i++)
        {
            String temp = in.readString();
            name.put(temp, i);
            iname[i] = temp;
            w[i] = in.readInt();
            l[i] = in.readInt();
            r[i] = in.readInt();
            for (int j = 0; j < N; j++)
            {
                int tmp = in.readInt();
                g[i][j] = tmp;
                g[j][i] = tmp;
            }
        }
    }
    
    public int numberOfTeams()                        // number of teams
    {
        return N;
    }
    
    public Iterable<String> teams()                                // all teams
    {
        return name;
    }
    
    public int wins(String team)                      // number of wins for given team
    {
        if (!name.contains(team)) throw new java.lang.IllegalArgumentException();
        return w[name.get(team)];
    }
    
    public int losses(String team)                    // number of losses for given team
    {
        if (!name.contains(team)) throw new java.lang.IllegalArgumentException();
        return l[name.get(team)];
    }
    
    public int remaining(String team)                 // number of remaining games for given team
    {
        if (!name.contains(team)) throw new java.lang.IllegalArgumentException();
        return r[name.get(team)];
    }
    
    public int against(String team1, String team2)    // number of remaining games between team1 and team2
    {
        if (!name.contains(team1) || !name.contains(team2)) throw new java.lang.IllegalArgumentException();
        return g[name.get(team1)][name.get(team2)];
    }
    
    private FlowNetwork initialGraph(String team)
    {
        int index = name.get(team);
        
        int nvert = N+2;
        for (int i = 0; i < N; i++)
            for (int j = i+1; j < N; j++)
            {
                if (g[i][j] > 0 && i != index && j != index)
                {
                    nvert++;
                }
            }
        FlowNetwork G = new FlowNetwork(nvert);
        
        s = 0;
        t = nvert - 1;
        
        int count = 1;
        for (int i = 0; i < N; i++)
            for (int j = i + 1; j < N; j++)
            {
                if (g[i][j] > 0 && i != index && j != index)
                {
                    FlowEdge edge = new FlowEdge(s, count, g[i][j]);
                    G.addEdge(edge);
                    
                    FlowEdge out1 = new FlowEdge(count, t - N + i, Double.POSITIVE_INFINITY);
                    FlowEdge out2 = new FlowEdge(count, t - N + j, Double.POSITIVE_INFINITY);
                    G.addEdge(out1);
                    G.addEdge(out2);
                    
                    count++;
                }
            }
        
        for (int i = 0; i < N; i++)
        {
            if (i != index)
            {
                FlowEdge right = new FlowEdge(t - N + i, t, r[index] + w[index] - w[i]);
                G.addEdge(right);
            }
        }
        
        return G;
    }
    
    
    private boolean isEliminated(int x, FlowNetwork G)
    {
        for (FlowEdge e : G.adj(x))
        {
            int v = e.other(x);
            if (e.residualCapacityTo(v) > 0)
                return true;
        }
        return false;
    }
    
    private boolean isTrivial(int index, int i)
    {
        return (w[i] > w[index] + r[index]);
    }
    
    public boolean isEliminated(String team)              // is given team eliminated?
    {
        if (!name.contains(team)) throw new java.lang.IllegalArgumentException();
        return (this.certificateOfElimination(team) != null);
    }
    
    public Iterable<String> certificateOfElimination(
            String team)  // subset R of teams that eliminates given team; null if not eliminated
    {
        if (!name.contains(team)) throw new java.lang.IllegalArgumentException();
        
        int index = name.get(team);
        for (int i = 0; i < N; i++)
        {
            if (i != index)
            {
                if (isTrivial(index, i))
                {
                    return Arrays.asList(iname[i]);
                }
            }
        }
        
        FlowNetwork G = initialGraph(team);
        
        FordFulkerson ff = new FordFulkerson(G, s, t);
        
        if (isEliminated(s, G))
        {
            Bag<String> subset = new Bag<String>();
            for (int i = 0; i < N; i++)
            {
                if (i != index)
                {
                    if (ff.inCut(t - N + i))
                        subset.add(iname[i]);
                }
            }
            return subset;
        }
        return null;
    }
    
    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            }
            else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }
}
