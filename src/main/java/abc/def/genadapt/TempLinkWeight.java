/*  
This class uses and extends existing third-party material 
obtained from the following source: https://figshare.com/s/b4f6b58da221341989dc 
*/
package abc.def.genadapt;

import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.Link;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TempLinkWeight implements LinkWeigher {
    private final Lock weightLock = new ReentrantLock();
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final ScalarWeight ONE = new ScalarWeight(1D);
    private static final ScalarWeight ZERO = new ScalarWeight(0D);

    private Map<Link, ScalarWeight> edgeCostMap;

    public TempLinkWeight() {
        edgeCostMap = new HashMap<>();
    }

    public Weight weight(TopologyEdge edge) {
        weightLock.lock();
        Weight weight = edgeCostMap.get(edge.link());
        if (weight == null) {
            String annotateVal = edge.link().annotations().value(Config.LATENCY_KEY);
            if (annotateVal == null) {
                edgeCostMap.put(edge.link(), new ScalarWeight(Integer.valueOf(Config.DEFAULT_DELAY)));
                //edgeCostMap.put(edge.link(), ONE);
            } else {
                edgeCostMap.put(edge.link(), new ScalarWeight(Integer.valueOf(annotateVal)));
            }
            weightLock.unlock();
            return edgeCostMap.get(edge.link());
        }
        weightLock.unlock();
        return weight;
    }

    public Weight getInitialWeight() {
        return ZERO;
    }

    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }

    public double getLinkWeight(Link l) {
        ScalarWeight weight = edgeCostMap.get(l);
        return weight.value();
    }

    public void setLinkWeight(Link l, int weight) {
        edgeCostMap.put(l, new ScalarWeight(weight));
    }

    public void lock() {
        weightLock.lock();
    }

    public void unlock() {
        weightLock.unlock();
    }
}
