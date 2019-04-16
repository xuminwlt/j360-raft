j360-raft Simple Raft For Java

# Modules



# 功能设计

## Node节点功能设计

1. 配置功能
2. Raft算法设计(状态、心跳、投票等)
3. RPC协议设计
4. Log日志设计
5. MetaData存储设计
6. Snapshot存储设计(可选)
7. 状态机接口及实现设计
8. RPC服务设计(Node通信和Client通信)


## 对象列表

- 配置Options
- 存储日志Log
- 快照Snapshot
- 状态机StateMachine
- 消息结构体:Peer
- 节点NodeServer地址
- 节点Node主体


## Node节点功能执行流程

1. 定义Node节点相关配置
2. 初始化Node节点(初始化Log日志内容, 初始化MetaData数据内容)
3. 初始化状态机(初始化Snapshot日志内容和Meta数据)
4. 定义并初始化Raft RPC服务注册
5. 完成Node节点注册功能


## 选举过程

RAFT中将时间划分到Term，用于选举，标示某个Leader下的Normal Case，每个Term最多只有一个Leader，某些term可能会选主失败而没有Leader（未达到多数投票应答而超时）。 img RAFT的选主过程中，每个Candidate节点先将本地的Current Term加一，然后向其他节点发送RequestVote请求，其他节点根据本地数据版本、长度和之前选主的结果判断应答成功与否。具体处理规则如下：

1. 如果now – lastLeaderUpdateTimestamp < elect_timeout，忽略请求
2. 如果req.term < currentTerm，忽略请求。
3. 如果req.term > currentTerm，设置req.term到currentTerm中，如果是Leader和Candidate转为Follower。
4. 如果req.term == currentTerm，并且本地voteFor记录为空或者是与vote请求中term和CandidateId一致，req.lastLogIndex > lastLogIndex，即Candidate数据新于本地则同意选主请求。
5. 如果req.term == currentTerm，如果本地voteFor记录非空或者是与vote请求中term一致CandidateId不一致，则拒绝选主请求。
6. 如果lastLogTerm > req.lastLogTerm，本地最后一条Log的Term大于请求中的lastLogTerm，说明candidate上数据比本地旧，拒绝选主请求。

上面的选主请求处理，符合Paxos的"少数服从多数，后者认同前者"的原则。按照上面的规则，选举出来的Leader，一定是多数节点中Log数据最新的节点。下面来分析一下选主的时间和活锁问题，设定Follower检测Leader Lease超时为HeartbeatTimeout，Leader定期发送心跳的时间间隔将小于HeartbeatTimeout，避免Leader Lease超时，通常设置为小于 HeartbeatTimeout/2。当选举出现冲突，即存在两个或多个节点同时进行选主，且都没有拿到多数节点的应答，就需要重新进行选举，这就是常见的选主活锁问题。RAFT中引入随机超时时间机制，有效规避活锁问题。

注意上面的Log新旧的比较，是基于lastLogTerm和lastLogIndex进行比较，而不是基于currentTerm和lastLogIndex进行比较。currentTerm只是用于忽略老的Term的vote请求，或者提升自己的currentTerm，并不参与Log新旧的决策。考虑一个非对称网络划分的节点，在一段时间内会不断的进行vote，并增加currentTerm，这样会导致网络恢复之后，Leader会接收到AppendEntriesResponse中的term比currentTerm大，Leader就会重置currentTerm并进行StepDown，这样Leader就对齐自己的Term到划分节点的Term，重新开始选主，最终会在上一次多数集合中选举出一个term>=划分节点Term的Leader。


# 分布式一致性系统搭建

- KV系统
- 分布式锁系统
- 注册中心


参考:

https://raft.github.io/slides/raftuserstudy2013.pdf