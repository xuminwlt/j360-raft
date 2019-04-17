package me.j360.raft.counter.server;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.option.ReadOnlyOption;
import com.alipay.sofa.jraft.util.Bits;
import com.codahale.metrics.ConsoleReporter;
import lombok.Getter;
import lombok.SneakyThrows;
import me.j360.raft.counter.KeyNotFoundException;
import me.j360.raft.counter.command.BooleanCommand;
import me.j360.raft.counter.command.ValueCommand;
import me.j360.raft.counter.server.processor.GetCommandProcessor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: min_xu
 */

public class AtomicRangeGroup {

    final static Logger LOG       = LoggerFactory.getLogger(AtomicRangeGroup.class);

    @Getter
    private final RaftGroupService raftGroupService;
    @Getter
    private final Node node;
    @Getter
    private final AtomicStateMachine fsm;

    @Getter
    private final long minSlot;
    @Getter
    private final long maxSlot;

    private final AtomicInteger requestId = new AtomicInteger(0);

    @SneakyThrows
    public AtomicRangeGroup(String dataPath, String groupId, PeerId serverId, long minSlot, long maxSlot,
                            NodeOptions nodeOptions, RpcServer rpcServer) {

        //初始化路径
        FileUtils.forceMkdir(new File(dataPath));
        this.minSlot = minSlot;
        this.maxSlot = maxSlot;

        //初始化状态机
        this.fsm = new AtomicStateMachine();
        //设置状态机到启动参数
        nodeOptions.setFsm(this.fsm);
        nodeOptions.setEnableMetrics(true);
        nodeOptions.getRaftOptions().setReplicatorPipeline(true);
        nodeOptions.getRaftOptions().setSync(true);
        nodeOptions.getRaftOptions().setReadOnlyOptions(ReadOnlyOption.ReadOnlySafe);
        //设置存储路径
        //日志,必须
        nodeOptions.setLogUri(dataPath + File.separator + "log");
        //元信息,必须
        nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta");
        //snapshot,可选,一般都推荐.
        nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot");
        //初始化 raft group 服务框架
        this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer);
        //启动
        this.node = this.raftGroupService.start();

        final ConsoleReporter reporter = ConsoleReporter.forRegistry(node.getNodeMetrics().getMetricRegistry())
                .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
        reporter.start(60, TimeUnit.SECONDS);
    }

    //读取数据
    public void readFromQuorum(final String key, AsyncContext asyncContext) {
        final byte[] reqContext = new byte[4];
        Bits.putInt(reqContext, 0, requestId.incrementAndGet());
        this.node.readIndex(reqContext, new ReadIndexClosure() {
            @Override
            public void run(Status status, long index, byte[] reqCtx) {
                if (status.isOk()) {
                    try {
                        asyncContext.sendResponse(new ValueCommand(fsm.getValue(key)));
                    } catch (final KeyNotFoundException e) {
                        asyncContext.sendResponse(GetCommandProcessor.createKeyNotFoundResponse());
                    }
                } else {
                    asyncContext.sendResponse(new BooleanCommand(false, status.getErrorMsg()));
                }
            }
        });
    }

    /**
     * Redirect request to new leader
     * @return
     */
    public BooleanCommand redirect() {
        final BooleanCommand response = new BooleanCommand();
        response.setSuccess(false);
        response.setErrorMsg("Not leader");
        if (node != null) {
            final PeerId leader = node.getLeaderId();
            if (leader != null) {
                response.setRedirect(leader.toString());
            }
        }

        return response;
    }



    @SneakyThrows
    public static AtomicRangeGroup start(StartupConf conf, RpcServer rpcServer) {
        final NodeOptions nodeOptions = new NodeOptions();
        //为了测试,调整 snapshot 间隔等参数
        //设置选举超时时间为 1 秒
        nodeOptions.setElectionTimeoutMs(1000);
        //关闭 CLI 服务。
        nodeOptions.setDisableCli(false);
        //每隔30秒做一次 snapshot
        // nodeOptions.setSnapshotIntervalSecs(30);
        //解析参数
        final PeerId serverId = new PeerId();
        if (!serverId.parse(conf.getServerAddress())) {
            throw new IllegalArgumentException("Fail to parse serverId:" + conf.getServerAddress());
        }
        final Configuration initConf = new Configuration();
        if (!initConf.parse(conf.getConf())) {
            throw new IllegalArgumentException("Fail to parse initConf:" + conf.getConf());
        }
        //设置初始集群配置
        nodeOptions.setInitialConf(initConf);
        //启动
        final AtomicRangeGroup node = new AtomicRangeGroup(conf.getDataPath(), conf.getGroupId(), serverId,
                conf.getMinSlot(), conf.getMaxSlot(), nodeOptions, rpcServer);
        LOG.info("Started range node[{}-{}] at port:{}", conf.getMinSlot(), conf.getMaxSlot(), node.getNode()
                .getNodeId().getPeerId().getPort());
        return node;
    }

}
