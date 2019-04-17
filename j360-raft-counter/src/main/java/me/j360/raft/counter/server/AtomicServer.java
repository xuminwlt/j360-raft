package me.j360.raft.counter.server;

import com.alipay.remoting.rpc.RpcServer;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import me.j360.raft.counter.HashUtils;
import me.j360.raft.counter.server.processor.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/**
 * @author: min_xu
 */
public class AtomicServer {

    private static final Logger LOG    = LoggerFactory.getLogger(AtomicServer.class);

    private TreeMap<Long, AtomicRangeGroup> nodes  = new TreeMap<>();
    private TreeMap<Long, String>           groups = new TreeMap<>();
    private int                             totalSlots;
    private StartupConf                     conf;

    public AtomicRangeGroup getGroupBykey(String key) {
        return nodes.get(HashUtils.getHeadKey(this.nodes, key));
    }

    public int getTotalSlots() {
        return this.totalSlots;
    }

    public AtomicServer(StartupConf conf) throws IOException {
        this.conf = conf;
        LOG.info("Starting atomic server with conf: {}", this.conf);
        this.totalSlots = conf.getTotalSlots();
    }

    public TreeMap<Long, String> getGroups() {
        return this.groups;
    }

    public void start() throws IOException {
        PeerId serverId = new PeerId();
        if (!serverId.parse(conf.getServerAddress())) {
            throw new IllegalArgumentException("Fail to parse serverId:" + conf.getServerAddress());
        }

        FileUtils.forceMkdir(new File(conf.getDataPath()));
        //同一个进程内 raft group 共用同一个 RPC Server.
        RpcServer rpcServer = new RpcServer(serverId.getPort());
        //注册 raft 处理器
        RaftRpcServerFactory.addRaftRequestProcessors(rpcServer);
        //注册业务处理器
        rpcServer.registerUserProcessor(new GetSlotsCommandProcessor(this));
        rpcServer.registerUserProcessor(new GetCommandProcessor(this));
        rpcServer.registerUserProcessor(new IncrementAndGetCommandProcessor(this));
        rpcServer.registerUserProcessor(new CompareAndSetCommandProcessor(this));
        rpcServer.registerUserProcessor(new SetCommandProcessor(this));

        long step = conf.getMaxSlot() / totalSlots;
        if (conf.getMaxSlot() % totalSlots > 0) {
            step = step + 1;
        }
        for (int i = 0; i < totalSlots; i++) {
            long min = i * step;
            long mayMax = (i + 1) * step;
            long max = mayMax > conf.getMaxSlot() || mayMax <= 0 ? conf.getMaxSlot() : mayMax;
            StartupConf nodeConf = new StartupConf();
            String nodeDataPath = conf.getDataPath() + File.separator + i;
            nodeConf.setDataPath(nodeDataPath);
            String nodeGroup = conf.getGroupId() + "_" + i;
            nodeConf.setGroupId(nodeGroup);
            nodeConf.setMaxSlot(max);
            nodeConf.setMinSlot(min);
            nodeConf.setConf(conf.getConf());
            nodeConf.setServerAddress(conf.getServerAddress());
            nodeConf.setTotalSlots(conf.getTotalSlots());
            LOG.info("Starting range node {}-{} with conf {}", min, max, nodeConf);
            nodes.put(i * step, AtomicRangeGroup.start(nodeConf, rpcServer));
            groups.put(i * step, nodeGroup);
        }
    }

    public static void start(String confFilePath) throws IOException {
        StartupConf conf = new StartupConf();
        if (!conf.loadFromFile(confFilePath)) {
            throw new IllegalStateException("Load startup config from " + confFilePath + " failed");
        }
        AtomicServer server = new AtomicServer(conf);
        server.start();
    }

    //for test
    public static void main(String[] arsg) throws Exception {
        start("./j360-raft-counter/config/server1.properties");
        start("./j360-raft-counter/config/server2.properties");
        start("./j360-raft-counter/config/server3.properties");
    }


}
