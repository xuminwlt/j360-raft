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


# 分布式一致性系统搭建

- KV系统
- 分布式锁系统
- 注册中心
