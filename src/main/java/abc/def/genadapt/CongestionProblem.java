/*  
This class uses and extends existing third-party material 
obtained from the following source: https://figshare.com/s/b4f6b58da221341989dc 
*/
package abc.def.genadapt;

import ec.gp.koza.KozaFitness;
import org.onosproject.net.*;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class CongestionProblem extends GPProblem implements SimpleProblemForm {
    private static final long serialVersionUID = 1L;

    private final Logger log = getLogger(getClass());
    private boolean weightAboveZero = true;
    private TopologyService topologyService;
    private LinkService linkService;
    private HostService hostService;
    private MonitorUtil monitorUtil;

    private SearchRunner runner;

    private Map<SrcDstPair, Long> sdTxBitsMap;
    private Map<Link, Long> simLinkThroughputMap;
    private Map<Link, Long> currentThroughputMap;
    private Map<Link, Long> currentSimLinkThroughputMap;

    private List<SrcDstPair> curSDList;
    private Map<SrcDstPair, Path> sdAltPathListMap;
    private Map<SrcDstPair, List<Link>> sdCurrentPathMap;
    private Map<Link, Integer> newWeight;
    private Map<Link, Double> packetLossRateMap;
    private ArrayList<SrcDstPair> simPair;
    public double currentY;
    public double currentZ;
    private List<String> indsString;

    private ArrayList<Individual> individualInOneGeneration;
    private ArrayList<Individual> individualResults;
    private Map<Link, Long> tempThroughputMap = new HashMap<>();
    private int numberOfBadFlows = 0;
    private Iterable<Link> currentLinks;

    public void setup(final EvolutionState state,
                      final Parameter base, SearchRunner runner) {
        this.runner = runner;

        log.info("congestion problem setup!");

        this.topologyService = runner.getTopologyService();
        this.linkService = runner.getLinkService();
        this.hostService = runner.getHostService();
        this.monitorUtil = runner.getMonitorUtil();

        this.sdTxBitsMap = new HashMap<>();
        this.simLinkThroughputMap = new HashMap<>();
        this.sdAltPathListMap = new HashMap<>();
        this.sdCurrentPathMap = new HashMap<>();
        this.currentThroughputMap = new HashMap<>();
        this.currentSimLinkThroughputMap = new HashMap<>();
        this.newWeight = new HashMap<>();
        this.packetLossRateMap = new HashMap<>();
        this.indsString = runner.getIndsString();
        this.simPair = new ArrayList<>();
        this.individualInOneGeneration = new ArrayList<>();
        super.setup(state, base, runner);
        this.currentLinks = linkService.getLinks();
        setCurSDPath();
        initSimLinkThroughputMap();
        updateSDTxBitsMap();
        initCurrentLinkThroughputMap();
        updateCurrentThroughputMap();
        simPair = initialSimLink();

        // verify our input is the right class (or subclasses from it)
        if (!(input instanceof DoubleData)) {
            state.output.fatal("GPData class must subclass from " + abc.def.genadapt.DoubleData.class,
                    base.push(P_DATA), null);
            log.error("GPData class must subclass from " + abc.def.genadapt.DoubleData.class);
        }
    }

    public int getBadFlowNumver() {
        return numberOfBadFlows;
    }

    //set the inital value of throughput to 0
    private void initCurrentLinkThroughputMap() {
        for (Link l : currentLinks) {
            currentThroughputMap.put(l, new Long(0));
        }
    }

    public void updateCurrentThroughputMap() {
        for (Link l : currentLinks) {
            long newValue = monitorUtil.getDeltaTxBits(l);
            currentThroughputMap.put(l, newValue);
        }
    }

    public double getUtilization(Link l) {
        long throughput = currentSimLinkThroughputMap.get(l);
        return monitorUtil.calculateUtilization(l, throughput);
    }

    //store the relation of all (src,dst) pair and links based on the flowEntries
    private void setCurSDPath() {
        Set<FlowEntry> flowEntrySet = monitorUtil.getAllCurrentFlowEntries();
        Set<SrcDstPair> sdSet = monitorUtil.getAllSrcDstPairs(flowEntrySet);
        if (Config.test) {
            for (SrcDstPair sdpair : sdSet) {
                log.info("src: " + sdpair.src + ", dst: " + sdpair.dst);
            }
        }
        curSDList = new ArrayList<>(sdSet);
        Iterator<SrcDstPair> it = sdSet.iterator();

        while (it.hasNext()) {
            SrcDstPair sd = it.next();
            List<Link> dIdPath = monitorUtil.getCurrentPath(sd);
            if (dIdPath == null) {
                it.remove();
                continue;
            }
            sdCurrentPathMap.put(sd, dIdPath);
        }
    }

    public List<SrcDstPair> getSrcDstPair() {
        return curSDList;
    }

    //set the inital value of throughput to 0
    private void initSimLinkThroughputMap() {
        for (Link l : currentLinks) {
            simLinkThroughputMap.put(l, new Long(0));
        }
    }

    //bits number - update
    private void updateSDTxBitsMap() {
        for (SrcDstPair sd : curSDList) {
            long deltaTxBits = monitorUtil.getTxBitsPerSec(sd);
            log.info("update: "+sd.src.toString() + " - " + deltaTxBits);
            int srcCnt = cntSameSrcInCurFlows(sd);
            deltaTxBits /= srcCnt;
            sdTxBitsMap.put(sd, deltaTxBits);
        }
    }

    //the number of paths of the same src
    private int cntSameSrcInCurFlows(SrcDstPair sd) {
        int cnt = 0;
        for (SrcDstPair cSD : curSDList) {
            if (cSD.src.equals(sd.src)) {
                cnt++;
            }
        }
        return cnt;
    }

    public void setSearchRunner(SearchRunner runnner) {
        this.runner = runnner;
    }

    public ArrayList<SrcDstPair> simPair() {
        return this.simPair;
    }

    public void evaluate(final EvolutionState state,
                         Individual ind,
                         final int subpopulation,
                         final int threadnum) {
        KozaFitness f = (KozaFitness) ind.fitness;
        if ((ind.evaluated == false) || (state.generation == 0)) {
            ind.evaluated = true;
            Map<SrcDstPair, Path> newSolution = simLink(state, ind, threadnum);
            if (newSolution == null) {
                f.setStandardizedFitness(state, 3);
                return;
            }
            double fitness = 0;

            double maxEstimateUtilization = estimateMaxLinkUtilization();
            if (maxEstimateUtilization >= Config.UTILIZATION_THRESHOLD) {
                fitness = maxEstimateUtilization / (maxEstimateUtilization + 1) + 2;
            } else {
                double costByDiff = calculateDiffFromOrig(newSolution);
                double totalDelay = sumDelay(newSolution);
                fitness = (costByDiff / (1 + costByDiff)) + (totalDelay / (totalDelay + 1));
            }

            if (Config.test) {
                log.info("fitness : {}", fitness);
            }

            f.setStandardizedFitness(state, fitness);

            if (Config.collectFitness) {
                indsString.add(state.generation + "\t" + ((KozaFitness) ind.fitness).standardizedFitness() + "\t" + ((GPIndividual) ind).toGPString());
            }
        } else {
            if (Config.test) {
                log.info("already evaluated, do nothing");
            }
            if (Config.collectFitness) {
                indsString.add(state.generation + "\t" + ((KozaFitness) ind.fitness).standardizedFitness() + "\t" + ((GPIndividual) ind).toGPString());
            }
        }

    }

    public List<String> getIndString() {
        return indsString;
    }
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }
    public Map<SrcDstPair, Path> simLink(final EvolutionState state,
                                         final Individual ind,
                                         final int threadnum) {
        TempLinkWeight tempWeight = new TempLinkWeight();
        DoubleData input = (DoubleData) (this.input);

        for (Link l5 : currentLinks) {
            currentY = monitorUtil.calculateUtilization(l5, currentSimLinkThroughputMap.get(l5));
            if (currentY < 0.01)
                currentY = 0.01;
            currentZ = monitorUtil.getDelay(l5);
            try {
                ((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), this);
            } catch (Exception e) {
                return null;
            }
            double newLinkWeight = input.x;
            if ((int) newLinkWeight <= 0) {
                return null;
            }
            tempWeight.setLinkWeight(l5, (int) newLinkWeight);
        }
        tempThroughputMap.clear();

        for (Link l : currentSimLinkThroughputMap.keySet()) {
            tempThroughputMap.put(l, currentSimLinkThroughputMap.get(l));
        }

        for (SrcDstPair sd : simPair()) {
            Host srcHost = hostService.getHost(HostId.hostId(sd.src));
            DeviceId srcDevId = srcHost.location().deviceId();
            Host dstHost = hostService.getHost(HostId.hostId(sd.dst));
            DeviceId dstDevId = dstHost.location().deviceId();

            Set<Path> allPathSet =
                    topologyService.getKShortestPaths(
                            topologyService.currentTopology(),
                            srcDevId, dstDevId,
                            tempWeight,
                            Config.MAX_NUM_PATHS);

            //Set<Path> allPathSet =topologyService.getPaths(topologyService.currentTopology(),
             //       srcDevId, dstDevId,
             //       tempWeight);
           // Path path = pickForwardPathIfPossible(allPathSet, srcHost.location().port());
           // sdAltPathListMap.put(sd, path);
            sdAltPathListMap.put(sd, (Path) (allPathSet.toArray()[0]));
            List<Link> newLinks = sdAltPathListMap.get(sd).links();
            for (Link nL : newLinks) {
                long throughputPerSec = sdTxBitsMap.get(sd);
                long updateThroughput = tempThroughputMap.get(nL) + throughputPerSec;
                double simUtilization = monitorUtil.calculateUtilization(nL, updateThroughput);
                currentY = simUtilization;
                if (currentY < 0.01)
                    currentY = 0.01;
                currentZ = (double) monitorUtil.getDelay(nL);
                try {
                    ((GPIndividual) ind).trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), this);
                } catch (Exception e) {
                    return null;
                }
                double newLinkWeight = input.x;
                if ((int) newLinkWeight <= 0) {
                    return null;
                }
                tempWeight.setLinkWeight(nL, (int) newLinkWeight);
                tempThroughputMap.put(nL, updateThroughput);
            }
            if (Config.test) {
                log.info("sd-pair:" + sd + "; srcDev" + srcDevId + "; dstDev" + dstDevId +
                        "; host src:" + srcHost + "; host dst:" + dstHost + "; Path" + allPathSet.toArray()[0]);
            }
        }

        return sdAltPathListMap;
    }

    public ArrayList<SrcDstPair> initialSimLink() {

        ArrayList<Link> congestedLinks = new ArrayList<>();

        //initial currentSimLinkThroughputMap and find the congested links
        for (Link l0 : currentLinks) {
            currentSimLinkThroughputMap.put(l0, (long) 0);
            long throuputPerLink = currentThroughputMap.get(l0);
            double utilizationPerLink = monitorUtil.calculateUtilization(l0, throuputPerLink);
            if (utilizationPerLink >= Config.UTILIZATION_THRESHOLD) {
                congestedLinks.add(l0);
            }
        }

        for (SrcDstPair sdForThroughput : curSDList) {
            List<Link> linksForThroughput = sdCurrentPathMap.get(sdForThroughput);
            //log.info(linksForThroughput + " : " + sdTxBitsMap.get(sdForThroughput));
            if (linksForThroughput != null) {
                for (Link linkForThroughput : linksForThroughput) {
                    Long newThroughput = currentSimLinkThroughputMap.get(linkForThroughput) + sdTxBitsMap.get(sdForThroughput);
                    currentSimLinkThroughputMap.put(linkForThroughput, newThroughput);
                }
            }
        }

        // find the flow whose links match the congested links most, delete its utilization
        // from the original utilization of the congested link. If congestion is still there
        // find the second flow whose links match the congested links most...
        boolean congested = true;
        ArrayList<SrcDstPair> sds = new ArrayList<>();
        ArrayList<SrcDstPair> tempCurSDList = new ArrayList<>();
        for (SrcDstPair s0 : curSDList) {
            tempCurSDList.add(s0);
        }
        while (congested) {
            congested = false;
            SrcDstPair readyToDelete = null;
            int maxCount = 0;
            boolean first = true;
            for (SrcDstPair sd : tempCurSDList) {
                if (first) {
                    readyToDelete = sd;
                    first = false;
                }
                int count = 0;
                List<Link> sdLinks = sdCurrentPathMap.get(sd);
                String sdLink = sd.src + " - " + sd.dst + " : ";
                if (sdLinks != null) {
                    for (Link l1 : sdLinks) {
                        sdLink = sdLink + l1.src() + l1.dst() + "|||";
                        if (congestedLinks.contains(l1)) {
                            count++;
                        }
                    }

                    if (count > maxCount) {
                        maxCount = count;
                        readyToDelete = sd;
                    }
                }

            }
            sds.add(readyToDelete);
            List<Link> readytoDeleteLinks = sdCurrentPathMap.get(readyToDelete);
            tempCurSDList.remove(readyToDelete);
            if (readytoDeleteLinks == null)
                log.warn(readyToDelete.src + "===" + readyToDelete.dst);
            else {
                for (Link l3 : readytoDeleteLinks) {
                    Long newThroughput = currentSimLinkThroughputMap.get(l3) - sdTxBitsMap.get(readyToDelete);
                    currentSimLinkThroughputMap.put(l3, newThroughput);
                }
            }

            for (Link l4 : congestedLinks) {
                double newUtil = monitorUtil.calculateUtilization(l4, currentSimLinkThroughputMap.get(l4));
                //more conservative computation of BadFlows.
                if (newUtil >= 0.8) {
                    congested = true;
                    log.info(congested + " : " + newUtil);
                    break;
                }
            }
        }
        numberOfBadFlows = sds.size();
        //for (SrcDstPair s1 : sds)
            //log.warn("bad: " + s1.src + " - " + s1.dst);
        return sds;
    }

    private long sumDelay(Map<SrcDstPair, Path> newLinksForPair) {
        long sum = 0;
        try {
            for (SrcDstPair key : curSDList) {
                if (newLinksForPair.keySet().contains(key)) {
                    Path sPath = newLinksForPair.get(key);
                    for (Link l : sPath.links()) {
                        sum += monitorUtil.getDelay(l);
                    }
                } else {
                    List<Link> sLinks = sdCurrentPathMap.get(key);
                    if (sLinks != null) {
                        for (Link l : sLinks) {
                            sum += monitorUtil.getDelay(l);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return sum;
    }

    private long calculateDiffFromOrig(Map<SrcDstPair, Path> newLinksForPair) {
        int distSum = 0;
        try {
            for (SrcDstPair key : newLinksForPair.keySet()) {
                SrcDstPair sd = key;
                Path sPath = newLinksForPair.get(sd);
                List<Link> sLinkPath = sPath.links();
                List<Link> oLinkPath = sdCurrentPathMap.get(sd);
                int dist = editLCSDistance(oLinkPath, sLinkPath);
                distSum += dist;
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return distSum;
    }

    private int editLCSDistance(List<Link> x, List<Link> y) {
        int m = x.size(), n = y.size();
        int l[][] = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    l[i][j] = 0;
                } else if (x.get(i - 1).equals(y.get(j - 1))) {
                    l[i][j] = l[i - 1][j - 1] + 1;
                } else {
                    l[i][j] = Math.max(l[i - 1][j], l[i][j - 1]);
                }
            }
        }
        int lcs = l[m][n];
        return (m - lcs) + (n - lcs);
    }

    public List<Link> findLCS(List<Link> x, List<Link> y) {
        int m = x.size(), n = y.size();
        int l[][] = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    l[i][j] = 0;
                } else if (x.get(i - 1).equals(y.get(j - 1))) {
                    l[i][j] = l[i - 1][j - 1] + 1;
                } else {
                    l[i][j] = Math.max(l[i - 1][j], l[i][j - 1]);
                }
            }
        }
        List<Link> lcs = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (x.get(i - 1).equals(y.get(j - 1))) {
                lcs.add(x.get(i - 1));
                i--;
                j--;
            } else if (l[i - 1][j] > l[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        Collections.reverse(lcs);
        return lcs;
    }

    public Map<SrcDstPair, List<Link>> getCurrentLinkPath() {
        return sdCurrentPathMap;
    }

    private double estimateMaxLinkUtilization() {
        double max = 0D;
        for (Link l : currentLinks) {
            double u = estimateUtilization(l);
            if (max < u) {
                max = u;
            }
        }
        return max;
    }

    public double estimateUtilization(Link l) {
        long throughput = 0;

        if (tempThroughputMap.keySet().contains(l)) {
            throughput = tempThroughputMap.get(l);
            return monitorUtil.calculateUtilization(l, throughput);
        } else {
            log.warn(l.src() + "--" + l.dst());
            return 0;
        }
    }

    public Map<Link, Integer> getIndividualResultWeight(EvolutionState state, Individual tree, int threadnum, LinkService linkService) {
        Map<Link, Integer> result = new HashMap<>();
        DoubleData input = (DoubleData) (this.input);
        Iterable<Link> links = linkService.getLinks();
        for (Link l : links) {
            long delay = monitorUtil.getDelay(l);
            currentY = monitorUtil.monitorLinkUtilization(l);
            if (currentY < 0.01)
                currentY = 0.01;

            currentZ = (double) delay;
            try {
                ((GPIndividual) tree).trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) tree), this);
            } catch (Exception e) {
                return null;
            }
            double newLinkWeight = input.x;
            if ((int) newLinkWeight <= 0) {
                return null;
            }
            result.put(l, (int) newLinkWeight);
        }
        return result;
    }

    private double getPacketLossRate(Link l) {
        double result = 0;
        result = packetLossRateMap.get(l);
        return result;
    }
}