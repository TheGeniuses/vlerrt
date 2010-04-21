package testing;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.Timer;

import com.savarese.spatial.Point;

import rrt.Node;
import rrt.Tree;
import rrt.World;
import rrtImpl.DVLRRTnode;
import rrtImpl.RRTWorld;
import rrtImpl.VLRRTnode;
import search.RRTResearch;
import search.RRTsearch;

public class Testing { 

	/* Class for testing and evaluating the RRT algorithms */

	static public final double PROB_CHANGE_OBSTACLE = 0.10;

	private RRTsearch searcher;

	private World world;
	private int pGoal;
	private int baseLength;
	private int pWayPoint;
	private List<Node> wayPoints;
	protected VLRRTnode.changeEpsilonScheme inc; //= VLRRTnode.changeEpsilonScheme.Linear;
	protected double incFactor;// = .1;
	protected VLRRTnode.changeEpsilonScheme dec;// = VLRRTnode.changeEpsilonScheme.Linear;
	protected double decFactor;// = .1;
	private boolean optimize;// = false;
	private int runtime;


	private LinkedList<Stats> stats;

	/* TODO: recording start time and goal find time  */
	/* TODO: pretty print of information for graphing  */


	class TestTask extends Thread {

		RRTsearch search;
		int delay;
		Stats stats;

		public TestTask(RRTsearch search, int delay, Stats stats) { //pre: totally init'd search.
			super();
			this.search = search;
			this.delay = delay;
			this.stats = stats;
		}

		public void run() {
			ActionListener stopper = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					search.halt();
				}
			};
			javax.swing.Timer timer = new Timer(delay,stopper);
			timer.setRepeats(false); //stop once...
			long t0 = System.currentTimeMillis();
			timer.start();
			int nItrs = search.runSearchHalt(stats);
			long rtime = System.currentTimeMillis() - t0;
			stats.setRuntime(rtime);
			stats.setnIterations(nItrs);
			//System.out.println("Search has halted in "+rtime);

		}

	}

	public Testing(int runtime, int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world, boolean optimize)  {
		this(runtime, p, baseLength, pWayPoint, wayPoints, world, optimize, 
				VLRRTnode.changeEpsilonScheme.Linear, .1, VLRRTnode.changeEpsilonScheme.Linear, .1);


	}

	public Testing(int runtime, int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world)  {
		this(runtime, p, baseLength, pWayPoint, wayPoints, world, false, 
				VLRRTnode.changeEpsilonScheme.Linear, .1, VLRRTnode.changeEpsilonScheme.Linear, .1);

	}

	public Testing( int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world)  {
		this(50, p, baseLength, pWayPoint, wayPoints, world, false, 
				VLRRTnode.changeEpsilonScheme.Linear, .1, VLRRTnode.changeEpsilonScheme.Linear, .1);
	}

	public Testing( int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world, boolean optimize)  {
		this(50, p, baseLength, pWayPoint, wayPoints, world, optimize, 
				VLRRTnode.changeEpsilonScheme.Linear, .1, VLRRTnode.changeEpsilonScheme.Linear, .1);
	}

	public Testing(int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world,
			VLRRTnode.changeEpsilonScheme inc, double incFactor,VLRRTnode.changeEpsilonScheme dec, double decFactor) {
		this(50, p, baseLength, pWayPoint, wayPoints, world, false, 
				inc, incFactor, dec, decFactor);		
	}

	public Testing(int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world, boolean optimize,
			VLRRTnode.changeEpsilonScheme inc, double incFactor,VLRRTnode.changeEpsilonScheme dec, double decFactor) {
		this(50, p, baseLength, pWayPoint, wayPoints, world, optimize, 
				inc, incFactor, dec, decFactor);		
	}

	public Testing(int runtime, int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world,
			VLRRTnode.changeEpsilonScheme inc, double incFactor,VLRRTnode.changeEpsilonScheme dec, double decFactor) {
		this(runtime, p, baseLength, pWayPoint, wayPoints, world, false, 
				inc, incFactor, dec, decFactor);

	}

	public Testing(int runtime, int p, int baseLength, int pWayPoint, List<Node> wayPoints, World world, boolean optimize,
			VLRRTnode.changeEpsilonScheme inc, double incFactor,VLRRTnode.changeEpsilonScheme dec, double decFactor) {
		this.inc = inc;
		this.dec = dec;
		this.incFactor = incFactor;
		this.decFactor = decFactor;
		this.pGoal = p;
		this.baseLength = baseLength;
		this.pWayPoint = pWayPoint;
		this.wayPoints = wayPoints;

		this.stats = new LinkedList<Stats>();
		this.world = world;
		this.runtime = runtime;
		this.optimize = optimize;
	}

	private void execSearch(RRTsearch.Algorithm alg, boolean printToScreen, boolean replan, RRTResearch.averageStrat strat, int nClosest) {


		Stats stat = new Stats();
		stat.setwHeight(world.height());
		stat.setwWidth(world.width());
		stat.setAlg(alg);
		stat.setBaseLength(baseLength);
		stat.setpGoal(pGoal);
		stat.setpWayPoint(pWayPoint);

		switch (alg) {
		case ERRT: setupERRT(replan); break;
		case RRT : setupBasicRRT(replan); break;
		case VLRRT : setupVLRRT(replan); break;
		case VLERRT : setupVLERRT(replan,strat,nClosest); break;
		case DVLRRT : setupDVLRRT(replan); break;
		case DVLERRT : setupDVLERRT(replan,nClosest); break;
		}



		TestTask task = new TestTask(searcher,runtime,stat);
		task.start();
		try {
			task.join();
		} catch (InterruptedException e) {
			e.printStackTrace(); //Should not happen. Probably... :)
		}

		boolean gFound = searcher.foundGoal();
		stat.setGoalFound(gFound);

		if (gFound) { //Goal to root distance in the tree

			Node goal = stat.getGoal();

			while( !goal.isRoot() ){
				Node parent_node = goal.getParent();
				Point2D leaf = goal.getPoint();
				Point2D parent = parent_node.getPoint();
				stat.incgDistance(RRTsearch.euclidianDistance(parent, leaf));
				goal = parent_node;
			}

			stat.setGoal(null);

		}
		if (printToScreen) {
			//System.out.println("World Coverage: "+stat.getTreeCoverage());
			//System.out.println("Goal Distance: "+stat.getgDistance());
			if (stat.getElapsedTime() > 0)
				System.out.println("Alg:" +stat.getAlg() + " Runtime: "+stat.getElapsedTime());
			searcher.show();
		}
		stat.setnNodes(searcher.getSearchTree().nNodes());
		stats.add(stat); //Store for future processing.

		//searcher.show();


		//		searcher.screenshot("Exec_"+alg.toString()+"_"+System.currentTimeMillis());


	}

	private void setupBasicRRT(boolean replan) {

		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}
		this.searcher = RRTResearch.basicRRT(world, pGoal, baseLength, optimize);
		((RRTResearch)searcher).setPrevSearch(t);

	}

	private void setupERRT(boolean replan) {
		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}
		this.searcher = RRTResearch.ERRT(world, pGoal, baseLength, pWayPoint, wayPoints, optimize);
		((RRTResearch)searcher).setPrevSearch(t);
	}

	private void setupVLRRT(boolean replan) {
		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}

		this.searcher = RRTResearch.VLRRT(world, pGoal, baseLength, inc, incFactor, dec, decFactor, optimize);
		((RRTResearch)searcher).setPrevSearch(t);
	}

	private void setupVLERRT(boolean replan, RRTResearch.averageStrat strat, int nClosest) {
		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}
		this.searcher = RRTResearch.VLERRT(world, pGoal, baseLength, pWayPoint, wayPoints, inc, incFactor, dec, decFactor, optimize, strat, nClosest);
		((RRTResearch)searcher).setPrevSearch(t);
	}

	private void setupDVLRRT(boolean replan) {
		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}
		this.searcher = RRTResearch.DVLRRT(world, pGoal, baseLength, inc, incFactor, dec, decFactor, optimize);
		((RRTResearch)searcher).setPrevSearch(t);
	}

	private void setupDVLERRT(boolean replan, int nClosest) {
		Tree t = null;
		if (replan) {
			if (searcher != null)
				t = searcher.getSearchTree();
		}
		this.searcher = RRTResearch.DVLERRT(world, pGoal, baseLength, pWayPoint, wayPoints, inc, incFactor, dec, decFactor, optimize, nClosest);
		((RRTResearch)searcher).setPrevSearch(t);
	}






	private void advanceStart(int n) {
		LinkedList<Node> l = searcher.collectBestPlan();
		if (l.size() <= n)
			world.setStart(l.getLast().getPoint());
		else
			world.setStart(l.get(n).getPoint());


	}

	private void advanceStart(double d, boolean choose_begin) { //if choose_begin round down, else round up.
		LinkedList<Node> l = searcher.collectBestPlan();
		if (l.getFirst().getPoint().distance(world.goal()) == 0.0)
			stats.getLast().setAtGoal(true);

		double aux = 0.0;


		//Find between which 2 points the new start will be:
		Point2D begin = l.getFirst().getPoint();
		Point2D end = null;

		Iterator<Node> itr = l.iterator();
		itr.next();
		while (itr.hasNext()) {
			end = itr.next().getPoint();
			double inc = begin.distance(end);
			aux += inc;
			if (aux >= d) {
				break; //done
			}

			begin = end;
		}	

		if (choose_begin)
			world.setStart(begin);
		else {
			if (end != null)
				world.setStart(end);
			else
				world.setStart(begin);
		}



	}

	public List<Node> genWaypoints(int nWaypoints) {
		LinkedList<Node> l = searcher.collectBestPlan();
		Random rng = new Random(System.nanoTime());
		List<Node> res = new LinkedList<Node>();
		for (int i=0;i<nWaypoints;i++) {
			res.add(l.get(rng.nextInt(l.size())));
		}
		return res;
	}

	//TODO: densities
	public List<Node> genWaypointsDensity(int nWaypoints) {
		LinkedList<Node> l = searcher.collectBestPlan();
		List<Node> res = new LinkedList<Node>();
		if( searcher instanceof RRTResearch && l.size() > 0 ){
			RRTResearch r = (RRTResearch) searcher;
			Node[] array = l.toArray(new Node[l.size()]);
			int[] densities = new int[array.length];
			int max = 0;
			for(int i=0;i<array.length;++i){
				densities[i] = r.calculateDensity(array[i]);
				max += densities[i];
			}

			//invert count to get min density as more probable
			int n_max = 0;
			for(int i=0;i<array.length;++i){
				densities[i] = max - densities[i];
				n_max += densities[i]; 
			}

			Random rng = new Random(System.nanoTime());
			for (int i=0;i<nWaypoints;i++) {
				int n = rng.nextInt(n_max);
				for(int j=0;i<array.length;++j){
					n -= densities[i];
					if( n<=0 ){
						res.add( array[j] );
						break;
					}
				}
			}
		}
		return res;
	}



	//output to the stats directory
	public void printStats(String outputFile, boolean includeTime) {
		String fileName = "runStats/" + outputFile;
		if (includeTime) fileName = fileName+System.currentTimeMillis();

		File f = new File(fileName);
		try {
			FileWriter fwr = new FileWriter(f);

			Iterator<Stats> itr = stats.iterator();
			fwr.write("Algorithm \t baseLength \t gDistance \t goalFTime \t" +
					"goalFound \t initTime \t nIterations \t nNodes \t pGoal \t pWayPoint \t " +
			"runtimeCap \t treeCoverage \t wHeight \t wWidth \t coverageRatio \t TimeDelta\n");
			while (itr.hasNext()) {
				Stats s = itr.next();
				fwr.write(s.toString()+"\n");
			}
			fwr.close();

		} catch (FileNotFoundException e) {
			System.err.println("Bad file.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error writing to file.");
		}

	}

	public List<Stats> getStats() {
		return stats;
	}

	public void execNRuns(int n, RRTsearch.Algorithm alg, boolean printToScreen) {

		for (int i=0;i<n;i++) {
			execSearch(alg,printToScreen,false,RRTResearch.averageStrat.Weighted, 5);
			//changeWorld();
		}
		if (printToScreen) 
			searcher.screenshot("Exec_"+alg.toString()+"_"+System.currentTimeMillis());



	}

	public void execNRuns(int n, RRTsearch.Algorithm alg) {
		for (int i=0;i<n;i++) {
			execSearch(alg,false,false,RRTResearch.averageStrat.Weighted, 5);
			//changeWorld();
		}
	}



	public void execNReRuns(int n, RRTsearch.Algorithm alg, boolean printToScreen, RRTResearch.averageStrat strat, int nWaypoints, int nClosest, boolean changeWorld, boolean round) {
		for (int i=0;i<n;i++) {
			execSearch(alg,printToScreen, true, strat, nClosest);
			advanceStart(25.0, round);
			wayPoints = genWaypoints(nWaypoints);
//			wayPoints = genWaypointsDensity(nWaypoints);
			if (changeWorld && i<(n-1))
				world = world.changeWorld();
		}
	}



	public RRTsearch getSearcher(){
		return this.searcher;
	}

	//Testing(int runtimelimit, int p, int baseLength, int pWayPoint, 
	//	List<Node> wayPoints, double baseEpsilon, String testFile)  
	//

	public static void main(String[] args) throws Exception{

		Testing test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world")); //


		//		test.execNRuns(50,RRTsearch.Algorithm.RRT,true);
		test.execNReRuns(5,RRTsearch.Algorithm.DVLERRT,true, null,10, 10, true, false);
		test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
		//		test.execNReRuns(100,RRTsearch.Algorithm.VLERRT,true, RRTResearch.averageStrat.Simple,10, 10, true,false);
		//		test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
		//	    test.execNReRuns(100, RRTsearch.Algorithm.ERRT, true, null, 10, 10, true,false);

		//test.execNRuns(50,RRTsearch.Algorithm.DVLRRT,true);
		//test.printStats("stats_bb_50_20_10_", true);

		/*test = new Testing(50,40, 15, 0, null, 1, new RRTWorld("worlds/big_barrier_redux")); //


		test.execNRuns(50,RRTsearch.Algorithm.RRT);
		test.execNRuns(50,RRTsearch.Algorithm.VLRRT);
		test.execNRuns(50,RRTsearch.Algorithm.DVLRRT);
		test.printStats("stats_bbr_40_15_20_", true);

		test = new Testing(50,40, 15, 0, null, 1, new RRTWorld("worlds/mazzy")); //


		test.execNRuns(50,RRTsearch.Algorithm.RRT);
		test.execNRuns(50,RRTsearch.Algorithm.VLRRT);
		test.execNRuns(50,RRTsearch.Algorithm.DVLRRT);
		test.printStats("stats_mazzy_40_15_20_", true);

		test = new Testing(50,40, 15, 0, null, 1, new RRTWorld("worlds/tiny_passage")); //


		test.execNRuns(50,RRTsearch.Algorithm.RRT);
		test.execNRuns(50,RRTsearch.Algorithm.VLRRT);
		test.execNRuns(50,RRTsearch.Algorithm.DVLRRT);
		test.printStats("stats_tp_40_15_20_", true);*/
		//		for (int i=0;i<50;i++) {
		//			megaTest(0.10);
		//			megaTest(0.05);
		//		}
	}

	public void printer(FileWriter fwr, boolean change, boolean round) throws IOException {
		Iterator<Stats> itr = stats.iterator();
		while (itr.hasNext()) {
			Stats s = itr.next();
			fwr.write(s.alg + "\t" + world.getName() + "\t" +s.getElapsedTime() + "\t" + s.isAtGoal() + "\t" + change + "\t" + round + "\n");
		}
	}

	public static void megaTest(double percent) {
		String s = "megaTests_replanning" + percent + "_"+(System.currentTimeMillis()%1000);
		RRTResearch.thresholdFactor = percent;
		try {
			FileWriter fwr = new FileWriter(s+"_ERRT.xls");
			fwr.write("Algorithm \t World \t Runtime \t atGoal \t ChangeWorld \t Round\n");
			Testing test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000	, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.ERRT, false, null, 10, 10, false,false);
			test.printer(fwr, false, false);
			fwr.close();


			fwr = new FileWriter(s+"_VLERRT.xls");
			fwr.write("Algorithm \t World \t Runtime \t atGoal \t ChangeWorld \t Round\n");
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.VLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);
			fwr.close();


			fwr = new FileWriter(s+"_DVLERRT.xls");
			fwr.write("Algorithm \t World \t Runtime \t atGoal \t ChangeWorld \t Round\n");
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/RRTpaper-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/proposal-world"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/clutter"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);

			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,true);
			test.printer(fwr, true, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, true,false);
			test.printer(fwr, true, false);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,true);
			test.printer(fwr, false, true);
			test = new Testing(50,20, 15, 0, null, new RRTWorld("worlds/cluttered"));
			test.execNReRuns(5000, RRTsearch.Algorithm.DVLERRT, false, RRTResearch.averageStrat.Simple, 10, 5, false,false);
			test.printer(fwr, false, false);
			fwr.close();





		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
