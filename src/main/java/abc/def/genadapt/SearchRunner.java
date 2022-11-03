/*  
This class uses and extends existing third-party material 
obtained from the following source: https://figshare.com/s/b4f6b58da221341989dc 
*/
package abc.def.genadapt;

import ec.EvolutionState;
import ec.Evolve;
import ec.Individual;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.koza.KozaFitness;
import ec.gp.koza.KozaShortStatistics;
import ec.simple.SimpleStatistics;
import ec.simple.SimpleEvolutionState;
import ec.util.DataPipe;
import ec.util.Output;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.ParameterDatabase;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class SearchRunner {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private TopologyService topologyService;
    private LinkService linkService;
    private MonitorUtil monitorUtil;
    private HostService hostService;

    private Map<SrcDstPair, List<Link>> solutions = new HashMap<>();

    CongestionProblem congestionProblem;

    private EvolutionState state;
    private Individual solution;
    private Map<Link, Integer> newWeight;
    private boolean flag = true;
    private List<String> indsString;
    private Map<SrcDstPair, Path> solutionPath = null;
    private int GPRound;
    private boolean flagNoSolution;
    private FileWriter fwr;


    public SearchRunner(TopologyService topologyService, LinkService linkService, HostService hostService, MonitorUtil monitorUtil, int GPRound, boolean dflag) {
        this.topologyService = topologyService;
        this.linkService = linkService;
        this.hostService = hostService;
        this.monitorUtil = monitorUtil;
        this.flag = dflag;
        this.indsString = new ArrayList<>();
        this.GPRound = GPRound;
        File file = new File("./GPCollection");
        try {
            fwr = new FileWriter(file,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void search() {
        try {
            fwr.write(GPRound+"\r\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        flagNoSolution = false;
        Map<SrcDstPair, List<Link>> oldPath = getCurSDPath();
        log.info("Search runner-Search");
        long initTime = System.currentTimeMillis();
        ParameterDatabase child = new ParameterDatabase();

        //if (!flag) {
        if(true){
            File infiletmp = new File(("./start.in"));
            if (infiletmp.exists()) {
                log.info("half start from file");
                System.out.println("half start from file");
                File parameterFile = new File("./parameters1.params");
                ParameterDatabase dbase = null;
                try {
                    dbase = new ParameterDatabase(parameterFile,
                            new String[]{"-file", parameterFile.getCanonicalPath()});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    ParameterDatabase copy = (ParameterDatabase) (DataPipe.copy(dbase));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                child.addParent(dbase);
            }else{
                log.info("start from random");
                System.out.println("start from random");
                try {
                    fwr.write("start from random \r\n");
                    File infile = new File("./start.in");
                    infile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File parameterFile = new File("./parameters2.params");
                ParameterDatabase dbase = null;
                try {
                    dbase = new ParameterDatabase(parameterFile,
                            new String[]{"-file", parameterFile.getCanonicalPath()});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    ParameterDatabase copy = (ParameterDatabase) (DataPipe.copy(dbase));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                child.addParent(dbase);
            }
        } else {
            log.info("start from random");
            System.out.println("start from random");
            try {
                fwr.write("start from random \r\n");
                File infile = new File("./start.in");
                infile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File parameterFile = new File("./parameters2.params");
            ParameterDatabase dbase = null;
            try {
                dbase = new ParameterDatabase(parameterFile,
                        new String[]{"-file", parameterFile.getCanonicalPath()});
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ParameterDatabase copy = (ParameterDatabase) (DataPipe.copy(dbase));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            child.addParent(dbase);
        }

        long time1 = System.currentTimeMillis();
        Output out = Evolve.buildOutput();
        Thread t = Thread.currentThread();
        log.warn(String.valueOf(child));
        log.warn(String.valueOf(t.getId()));
        log.warn(String.valueOf(out));
        SimpleEvolutionState evaluatedState = (SimpleEvolutionState) Evolve.initialize(child, (int) t.getId(), out);
        state = evaluatedState;
        evaluatedState.startFresh(this);
        int result = EvolutionState.R_NOTDONE;
        long time2 = System.currentTimeMillis();
        System.out.println("time1： " + (time2 - time1) + ", " + getCurrentTime());
        try {
            fwr.write("time1： " + (time2 - time1) + ", " + getCurrentTime()+"\r\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int countG = 0;
        while (result == EvolutionState.R_NOTDONE) {
            result = evaluatedState.evolve();
            countG++;
        }
        long time3 = System.currentTimeMillis();
        System.out.println("time2: " + (time3 - time2) + ", " + getCurrentTime());
        System.out.println("average time for one generation is : " + (time3 - time2) / countG);
        try {
            fwr.write("time2: " + (time3 - time2) + ", " + getCurrentTime()+"\r\n");
            fwr.write("average time for one generation is : " + (time3 - time2) / countG+"\r\n");
            fwr.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Individual solutionTree = null;
        ArrayList<Individual> pops = new ArrayList<>();
        ArrayList<Individual> subpop0 = evaluatedState.population.subpops.get(0).individuals;
        for (Individual i1 : subpop0) {
            if (!pops.contains(i1)) {
                pops.add(i1);
            }
        }
        if (!flag) {
            ArrayList<Individual> subpop1 = evaluatedState.population.subpops.get(1).individuals;
            for (Individual i2 : subpop1) {
                if (!pops.contains(i2)) {
                    pops.add(i2);
                }
            }
        }
        double tempFitness = ((KozaFitness) pops.get(0).fitness).standardizedFitness();
        solutionTree = pops.get(0);
        for (Individual id : pops) {
            if (((KozaFitness) (id.fitness)).standardizedFitness() < tempFitness) {
                tempFitness = ((KozaFitness) (id.fitness)).standardizedFitness();
                solutionTree = id;
            }
        }

        if (((KozaFitness) (solutionTree.fitness)).standardizedFitness() >= 2.44) {
            solutionTree = null;
        }

        try {
            FileWriter fw = new FileWriter(Config.ConfigFile, true);
            List<String> indString = ((CongestionProblem) evaluatedState.evaluator.p_problem).getIndString();
            for (String s : indString) {
                fw.append(GPRound + "\t" + s + "\r\n");
            }
            fw.append("////////////////////////////////////" + "\r\n");
            fw.flush();

            if (solutionTree == null) {
                long computingTime = System.currentTimeMillis() - initTime;
                log.warn("no valid solution");
                log.info("Search time (ms): " + computingTime + "， one search finished.");
                flagNoSolution = true;
            } else {
                try {
                    //write the last generation to
                    PrintWriter writer = new PrintWriter("./start.in");
                    while (pops.size() > 5) {
                        int biggestFitnessIndex = 0;
                        int popsSize = pops.size();
                        int popsIndex = 0;
                        while (popsIndex < popsSize) {
                            if (((KozaFitness) (pops.get(biggestFitnessIndex).fitness)).standardizedFitness() < ((KozaFitness) (pops.get(popsIndex).fitness)).standardizedFitness()) {
                                biggestFitnessIndex = popsIndex;
                            }
                            popsIndex++;
                        }
                        pops.remove(biggestFitnessIndex);
                    }
                    evaluatedState.population.subpops.get(0).individuals.clear();
                    evaluatedState.population.subpops.get(0).individuals.addAll(pops);
                    evaluatedState.population.subpops.get(0).printSubpopulation(evaluatedState, writer);
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                fw.append("solution: " + "\t" + ((GPIndividual) solutionTree).toGPString() + "\t" + (((KozaFitness) solutionTree.fitness)).standardizedFitness() + "\r\n");
                fw.flush();

                solution = solutionTree;
                congestionProblem = (CongestionProblem) evaluatedState.evaluator.p_problem;
                List<SrcDstPair> srcDstPairs = congestionProblem.getSrcDstPair();

                Map<SrcDstPair, Path> newMap = congestionProblem.simLink(evaluatedState, solutionTree, 0);
                if (newMap != null)
                    solutionPath = new HashMap<>(newMap);
                else {
                    System.out.println(((GPIndividual) solutionTree).toGPString());
                    fwr.write(((GPIndividual) solutionTree).toGPString()+"\r\n");
                    fwr.flush();
                    return;
                }
                for (SrcDstPair pair : newMap.keySet()) {
                    solutions.put(pair, newMap.get(pair).links());
                }

                long computingTime = System.currentTimeMillis() - initTime;

                log.info("Search time (ms): " + computingTime + "， one search finished.");

                for (SrcDstPair sd : oldPath.keySet()) {
                    String oldPathString = "";
                    for (Link ol : oldPath.get(sd)) {
                        oldPathString = oldPathString + ol.src().toString() + "_" + ol.dst().toString() + " | ";
                    }
                    //log.info("oldPath: " + sd.src.toString() + " : " + sd.dst.toString() + " : " + oldPathString);
                }

                for (SrcDstPair sd : solutions.keySet()) {
                    String newPathString = "";
                    for (Link nl : solutions.get(sd)) {
                        newPathString = newPathString + nl.src().toString() + "_" + nl.dst().toString() + " | ";
                    }
                   // log.info("newPath: " + sd.src.toString() + " : " + sd.dst.toString() + " : " + newPathString);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long time4 = System.currentTimeMillis();
        System.out.println("time3: " + (time4 - time3) + ", " + getCurrentTime());
        try {
            fwr.write("time3: " + (time4 - time3) + ", " + getCurrentTime()+"\r\n");
            fwr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public int getNumberOfBadFlows() {
        return congestionProblem.getBadFlowNumver();
    }

    public void setFlag(boolean value) {
        this.flag = value;
    }

    public boolean getFlag() {
        return this.flag;
    }

    private Map<SrcDstPair, List<Link>> getCurSDPath() {
        Set<FlowEntry> flowEntrySet = monitorUtil.getAllCurrentFlowEntries();
        Set<SrcDstPair> sdSet = monitorUtil.getAllSrcDstPairs(flowEntrySet);
        Iterator<SrcDstPair> it = sdSet.iterator();
        Map<SrcDstPair, List<Link>> result = new HashMap<>();
        while (it.hasNext()) {
            SrcDstPair sd = it.next();
            List<Link> dIdPath = monitorUtil.getCurrentPath(sd);
            if (dIdPath == null) {
                it.remove();
                continue;
            }
            result.put(sd, dIdPath);
        }
        return result;
    }

    public String getCurrentTime() {
        long totalMilliSeconds = System.currentTimeMillis();
        long totalSeconds = totalMilliSeconds / 1000;
        long currentSecond = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long currentMinute = totalMinutes % 60;
        return (currentMinute + ":" + currentSecond);
    }

    public List<String> getIndsString() {
        return indsString;
    }

    public CongestionProblem getCongestionProblem() {
        return congestionProblem;
    }

    public Map<Link, Integer> getWeightUsingSolutionTree(Individual tree, LinkService linkService, CongestionProblem congestionProblem) {
        return ((CongestionProblem) congestionProblem).getIndividualResultWeight(
                state, tree, 0, linkService
        );
    }

    public Map<SrcDstPair, List<Link>> getCurrentLinkPath() {
        return ((CongestionProblem) congestionProblem).getCurrentLinkPath();
    }

    public Map<SrcDstPair, List<Link>> getSolutionLinkPath() {
        return solutions;
    }

    public Map<SrcDstPair, Path> getSolutionPath() {
        return solutionPath;
    }

    public List<Link> findLCS(List<Link> x, List<Link> y) {
        return ((CongestionProblem) congestionProblem).findLCS(x, y);
    }

    public boolean isSolvable() {
        if (flagNoSolution)
            return false;
        else
            return true;
    }

    public TopologyService getTopologyService() {
        return this.topologyService;
    }

    public LinkService getLinkService() {
        return linkService;
    }

    public HostService getHostService() {
        return hostService;
    }

    public MonitorUtil getMonitorUtil() {
        return monitorUtil;
    }

    public Individual getSolution() {
        return solution;
    }
}
