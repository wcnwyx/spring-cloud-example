本文翻译自zookeeper官网[https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperOver.md](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperOver.md)

# ZooKeeper

## ZooKeeper: A Distributed Coordination Service for Distributed Applications
> 面向分布式应用的分布式协调服务

ZooKeeper is a distributed, open-source coordination service for
distributed applications. It exposes a simple set of primitives that
distributed applications can build upon to implement higher level services
for synchronization, configuration maintenance, and groups and naming. It
is designed to be easy to program to, and uses a data model styled after
the familiar directory tree structure of file systems. It runs in Java and
has bindings for both Java and C.

> ZooKeeper是一个分布式的、开源的分布式应用程序协调服务。它公开了一组简单的原语，
> 分布式应用程序可以基于这些原语实现更高级别的同步、配置维护、组和命名服务。它的
> 设计很容易编程，并且使用了一个类似于熟悉的文件系统目录树结构的数据模型。
> 它在Java中运行，并且具有Java和C的绑定。


Coordination services are notoriously hard to get right. They are
especially prone to errors such as race conditions and deadlock. The
motivation behind ZooKeeper is to relieve distributed applications the
responsibility of implementing coordination services from scratch.

> 众所周知，协调服务很难做好。它们特别容易出错，例如竞争条件和死锁。
> ZooKeeper背后的动机是从零开始减轻分布式应用程序实现协调服务的责任。



### Design Goals
> 设计目标

**ZooKeeper is simple.** ZooKeeper
allows distributed processes to coordinate with each other through a
shared hierarchical namespace which is organized similarly to a standard
file system. The name space consists of data registers - called znodes,
in ZooKeeper parlance - and these are similar to files and directories.
Unlike a typical file system, which is designed for storage, ZooKeeper
data is kept in-memory, which means ZooKeeper can achieve high
throughput and low latency numbers.

> **ZooKeeper很简单。** ZooKeeper允许分布式进程通过一个共享的层次化名称空间相互协调，
> 这个名称空间的组织方式类似于标准文件系统。名称空间由数据寄存器组成——用ZooKeeper的说法称为znodes
> ——这些寄存器类似于文件和目录。与为存储而设计的典型文件系统不同，ZooKeeper数据保存在内存中，
> 这意味着ZooKeeper可以实现高吞吐量和低延迟数。


The ZooKeeper implementation puts a premium on high performance,
highly available, strictly ordered access. The performance aspects of
ZooKeeper means it can be used in large, distributed systems. The
reliability aspects keep it from being a single point of failure. The
strict ordering means that sophisticated synchronization primitives can
be implemented at the client.
> ZooKeeper的实现重视高性能、高可用性和严格有序的访问。ZooKeeper的性能方面意味着
> 它可以在大型分布式系统中使用。可靠性方面使它不至于成为单点故障。严格的排序意味着
> 复杂的同步原语可以在客户端实现。

**ZooKeeper is replicated.** Like the
distributed processes it coordinates, ZooKeeper itself is intended to be
replicated over a sets of hosts called an ensemble.
> **ZooKeeper是自我复制的。** 与它协调的分布式进程一样，ZooKeeper本身也是通过一组
> 称为集成的主机进行复制。

![ZooKeeper Service](https://zookeeper.apache.org/doc/r3.5.9/images/zkservice.jpg)

The servers that make up the ZooKeeper service must all know about
each other. They maintain an in-memory image of state, along with a
transaction logs and snapshots in a persistent store. As long as a
majority of the servers are available, the ZooKeeper service will be
available.
> 组成ZooKeeper服务的服务器必须相互知道。它们维护状态的内存映像，
> 以及持久存储中的事务日志和快照。只要大多数服务器可用，ZooKeeper服务就可以使用。

Clients connect to a single ZooKeeper server. The client maintains
a TCP connection through which it sends requests, gets responses, gets
watch events, and sends heart beats. If the TCP connection to the server
breaks, the client will connect to a different server.
> 客户端连接到单个ZooKeeper服务器。客户机维护一个TCP连接，通过它发送请求、获取响应、
> 获取监视事件和发送心跳。如果到服务器的TCP连接中断，客户端将连接到其他服务器。

**ZooKeeper is ordered.** ZooKeeper
stamps each update with a number that reflects the order of all
ZooKeeper transactions. Subsequent operations can use the order to
implement higher-level abstractions, such as synchronization
primitives.
> **Zookeeper 是有序的**。ZooKeeper会在每次更新时标记一个反映所有ZooKeeper事务顺序的数字。
> 后续操作可以使用顺序来实现更高级别的抽象，例如同步原语。

**ZooKeeper is fast.** It is
especially fast in "read-dominant" workloads. ZooKeeper applications run
on thousands of machines, and it performs best where reads are more
common than writes, at ratios of around 10:1.
> **ZooKeeper 很快。** 它在"以读为主"的工作负载中尤其快速。ZooKeeper应用程序在数千台机器上运行，
> 在读操作比写操作更常见的情况下，它的性能最好，比率大约为10:1。


### Data model and the hierarchical namespace
> 数据模型和分层名称空间

The name space provided by ZooKeeper is much like that of a
standard file system. A name is a sequence of path elements separated by
a slash (/). Every node in ZooKeeper's name space is identified by a
path.
> ZooKeeper提供的名称空间与标准文件系统的名称空间非常相似。
> 名称是由斜杠（/）分隔的路径元素序列。ZooKeeper名称空间中的每个节点都由一条路径标识。

#### ZooKeeper's Hierarchical Namespace
> ZooKeeper的层次命名空间

![ZooKeeper's Hierarchical Namespace](https://zookeeper.apache.org/doc/r3.5.9/images/zknamespace.jpg)


### Nodes and ephemeral nodes
> 节点和临时节点

Unlike standard file systems, each node in a ZooKeeper
namespace can have data associated with it as well as children. It is
like having a file-system that allows a file to also be a directory.
(ZooKeeper was designed to store coordination data: status information,
configuration, location information, etc., so the data stored at each
node is usually small, in the byte to kilobyte range.) We use the term
_znode_ to make it clear that we are talking about
ZooKeeper data nodes.
> 与标准文件系统不同，ZooKeeper命名空间中的每个节点都可以有与其关联的数据以及子节点。
> 这就像有一个文件系统，允许一个文件也成为一个目录。(ZooKeeper被设计用来存储协调数据：
> 状态信息、配置、位置信息等，因此存储在每个节点上的数据通常很小，在字节到千字节的范围内。）
> 我们使用 _znode_ 这个术语用来清楚地说明我们所说的ZooKeeper数据节点。

Znodes maintain a stat structure that includes version numbers for
data changes, ACL changes, and timestamps, to allow cache validations
and coordinated updates. Each time a znode's data changes, the version
number increases. For instance, whenever a client retrieves data it also
receives the version of the data.
> Znode维护一个stat结构，其中包含数据更改、ACL更改和时间戳的版本号，以允许缓存验证和协调更新。
> 每次znode的数据更改时，版本号都会增加。例如，每当客户机检索数据时，它也会接收数据的版本。

The data stored at each znode in a namespace is read and written
atomically. Reads get all the data bytes associated with a znode and a
write replaces all the data. Each node has an Access Control List (ACL)
that restricts who can do what.
> 存储在命名空间中每个znode上的数据是原子读写的。Reads获取与znode相关的所有数据字节，
> write替换所有数据。每个节点都有一个访问控制列表（ACL），限制谁可以做什么。

ZooKeeper also has the notion of ephemeral nodes. These znodes
exists as long as the session that created the znode is active. When the
session ends the znode is deleted.
> ZooKeeper也有短暂节点的概念。只要创建znode的会话处于活动状态，这些znode就存在。
> 当会话结束时，znode被删除。


### Conditional updates and watches
> 条件更新和监视

ZooKeeper supports the concept of _watches_.
Clients can set a watch on a znode. A watch will be triggered and
removed when the znode changes. When a watch is triggered, the client
receives a packet saying that the znode has changed. If the
connection between the client and one of the ZooKeeper servers is
broken, the client will receive a local notification.
> ZooKeeper支持 _监视_ 的概念。客户可以在znode上设置监视。当znode发生变化时，
> 监视将被触发并移除。当一个监视被触发时，客户端收到一个数据包，说znode已经改变了。
> 如果客户端和ZooKeeper服务器之一之间的连接断开，客户端将收到一个本地通知。


### Guarantees
> 保证

ZooKeeper is very fast and very simple. Since its goal, though, is
to be a basis for the construction of more complicated services, such as
synchronization, it provides a set of guarantees. These are:
> ZooKeeper非常快速并且非常简单。不过，由于它的目标是作为构建更复杂服务（如同步）的基础，
> 因此它提供了一组保证。这些是：

* Sequential Consistency - Updates from a client will be applied
  in the order that they were sent.
  > 顺序一致性 - 来自客户端的更新将按发送顺序应用。
* Atomicity - Updates either succeed or fail. No partial
  results.
  > 原子性 - 更新要么成功要么失败。没有部分结果。
* Single System Image - A client will see the same view of the
  service regardless of the server that it connects to.
  > 单个系统映像 — 无论连接到哪个服务器，客户端都将看到相同的服务视图。
* Reliability - Once an update has been applied, it will persist
  from that time forward until a client overwrites the update.
  > 可靠性 — 一旦应用了更新，它将从那时起一直存在，直到客户端覆盖更新。
* Timeliness - The clients view of the system is guaranteed to
  be up-to-date within a certain time bound.
  > 及时性 — 保证系统的客户端视图在一定的时间范围内是最新的。


### Simple API
> 简单的API

One of the design goals of ZooKeeper is providing a very simple
programming interface. As a result, it supports only these
operations:
> ZooKeeper的设计目标之一是提供一个非常简单的编程接口。因此，它仅支持以下操作：

* *create* :
  creates a node at a location in the tree
  > 在树中的某个位置创建节点

* *delete* :
  deletes a node
  > 删除一个节点

* *exists* :
  tests if a node exists at a location
  > 测试某个位置是否存在节点

* *get data* :
  reads the data from a node
  > 从一个节点读取数据

* *set data* :
  writes data to a node
  > 向一个节点写入数据

* *get children* :
  retrieves a list of children of a node
  > 检索节点的子列表

* *sync* :
  waits for data to be propagated
  > 等待数据传播


### Implementation
> 实施

[ZooKeeper Components](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkComponents) shows the high-level components
of the ZooKeeper service. With the exception of the request processor,
each of
the servers that make up the ZooKeeper service replicates its own copy
of each of the components.
> [ZooKeeper Components](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkComponents) 显示ZooKeeper服务的高级组件。
> 除了请求处理器之外，组成ZooKeeper服务的每个服务器都复制自己的每个组件的副本。


![ZooKeeper Components](https://zookeeper.apache.org/doc/r3.5.9/images/zkcomponents.jpg)

The replicated database is an in-memory database containing the
entire data tree. Updates are logged to disk for recoverability, and
writes are serialized to disk before they are applied to the in-memory
database.
> 复制数据库是一个内存中的数据库，包含整个数据树。更新会记录到磁盘以备恢复，
> 写入操作在应用到内存数据库之前会序列化到磁盘。

Every ZooKeeper server services clients. Clients connect to
exactly one server to submit requests. Read requests are serviced from
the local replica of each server database. Requests that change the
state of the service, write requests, are processed by an agreement
protocol.
> 每个ZooKeeper服务器都为客户端提供服务。客户端只连接到一个服务器以提交请求。
> 读取请求由每个服务器数据库的本地副本提供服务。更改服务状态的请求（写请求）由一致协议处理。

As part of the agreement protocol all write requests from clients
are forwarded to a single server, called the
_leader_. The rest of the ZooKeeper servers, called
_followers_, receive message proposals from the
leader and agree upon message delivery. The messaging layer takes care
of replacing leaders on failures and syncing followers with
leaders.
> 作为一致协议的一部分，来自客户机的所有写请求都被转发到一个名为 _leader_ 的服务器。
> 其余的ZooKeeper服务器称为 _followers_，接收来自leader的消息建议，并就消息传递达成一致。
> 消息传递层负责在失败时替换leaders，并将followers与leaders同步。

ZooKeeper uses a custom atomic messaging protocol. Since the
messaging layer is atomic, ZooKeeper can guarantee that the local
replicas never diverge. When the leader receives a write request, it
calculates what the state of the system is when the write is to be
applied and transforms this into a transaction that captures this new
state.
> ZooKeeper使用自定义的原子消息传递协议。由于消息传递层是原子的，
> ZooKeeper可以保证本地副本永远不会分离。当leader接收到一个写请求时，
> 它会计算应用写请求时系统的状态，并将其转换为捕获这个新状态的事务。


### Uses

The programming interface to ZooKeeper is deliberately simple.
With it, however, you can implement higher order operations, such as
synchronizations primitives, group membership, ownership, etc.
> ZooKeeper的编程接口非常简单。但是，通过它，您可以实现更高阶的操作，
> 例如同步原语、组成员资格、所有权等。


### Performance
> 性能

ZooKeeper is designed to be highly performance. But is it? The
results of the ZooKeeper's development team at Yahoo! Research indicate
that it is. (See [ZooKeeper Throughput as the Read-Write Ratio Varies](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkPerfRW).) It is especially high
performance in applications where reads outnumber writes, since writes
involve synchronizing the state of all servers. (Reads outnumbering
writes is typically the case for a coordination service.)
> ZooKeeper的设计是高性能的。但是是吗？雅虎ZooKeeper开发团队的成果！研究表明确实如此。
>(请参阅[ZooKeeper Throughput as the Read Write Ratio variables](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkPerfRW)。）
> 在读比写多的应用程序中，它的性能尤其高，因为写操作涉及同步所有服务器的状态
> (对于协调服务来说，读取多余写入是典型的情况。）


![ZooKeeper Throughput as the Read-Write Ratio Varies](https://zookeeper.apache.org/doc/r3.5.9/images/zkperfRW-3.2.jpg)

The [ZooKeeper Throughput as the Read-Write Ratio Varies](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkPerfRW) is a throughput
graph of ZooKeeper release 3.2 running on servers with dual 2Ghz
Xeon and two SATA 15K RPM drives.  One drive was used as a
dedicated ZooKeeper log device. The snapshots were written to
the OS drive. Write requests were 1K writes and the reads were
1K reads.  "Servers" indicate the size of the ZooKeeper
ensemble, the number of servers that make up the
service. Approximately 30 other servers were used to simulate
the clients. The ZooKeeper ensemble was configured such that
leaders do not allow connections from clients.
> [ZooKeeper Throughput as the Read-Write Ratio Varies](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkPerfRW)
> 是ZooKeeper 3.2版的吞吐量图，该版本运行在具有双2Ghz Xeon和两个SATA 15K RPM驱动器的服务器上。
> 一个驱动器用作专用的ZooKeeper日志设备。快照已写入操作系统驱动器。写入请求是1K次写入，
> 读取是1K次读取。"Servers" 表示ZooKeeper集合的大小，以及组成服务的服务器的数量。
> 大约30台其他服务器被用来模拟客户端。ZooKeeper集合的配置使leaders不允许来自客户端的连接。

######Note
>In version 3.2 r/w performance improved by ~2x compared to
the [previous 3.1 release](http://zookeeper.apache.org/docs/r3.1.1/zookeeperOver.html#Performance).
> 在版本3.2中，r/w性能比之前的3.1版本提高了约2倍。

Benchmarks also indicate that it is reliable, too.
[Reliability in the Presence of Errors](#zkPerfReliability) shows how a deployment responds to
various failures. The events marked in the figure are the following:
> 基准也表明它是可靠的。[Reliability in the Presence of Errors](https://zookeeper.apache.org/doc/r3.5.9/zookeeperOver.html#zkPerfReliability) 显示了部署如何响应各种故障。图中标记的事件如下：

1. Failure and recovery of a follower
    > follower的失败与恢复
1. Failure and recovery of a different follower
    > 不同follower的故障与恢复
1. Failure of the leader
    > leader的故障
1. Failure and recovery of two followers
    > 两个follower的故障和恢复
1. Failure of another leader
    > 另一个leader的故障


### Reliability
> 可靠性

To show the behavior of the system over time as
failures are injected we ran a ZooKeeper service made up of
7 machines. We ran the same saturation benchmark as before,
but this time we kept the write percentage at a constant
30%, which is a conservative ratio of our expected
workloads.
> 为了在注入故障时显示系统随时间的行为，我们运行了一个由7台机器组成的ZooKeeper服务。
> 我们像以前一样运行相同的饱和基准测试，但是这次我们将写百分比保持在30%不变，
> 这是我们预期工作负载的保守比率。


![Reliability in the Presence of Errors](https://zookeeper.apache.org/doc/r3.5.9/images/zkperfreliability.jpg)

There are a few important observations from this graph. First, if
followers fail and recover quickly, then ZooKeeper is able to sustain a
high throughput despite the failure. But maybe more importantly, the
leader election algorithm allows for the system to recover fast enough
to prevent throughput from dropping substantially. In our observations,
ZooKeeper takes less than 200ms to elect a new leader. Third, as
followers recover, ZooKeeper is able to raise throughput again once they
start processing requests.
> 从这张图中有一些重要的观察结果。首先，如果followers失败并快速恢复，
> 那么ZooKeeper能够在失败的情况下保持高吞吐量。但也许更重要的是，
> leader选举算法允许系统恢复足够快，以防止吞吐量大幅下降。根据我们的观察，
> ZooKeeper只需不到200秒就可以选出一位新的领导人。第三，随着followers的恢复，
> 一旦他们开始处理请求，ZooKeeper就能够再次提高吞吐量。


### The ZooKeeper Project

ZooKeeper has been
[successfully used](https://cwiki.apache.org/confluence/display/ZOOKEEPER/PoweredBy)
in many industrial applications.  It is used at Yahoo! as the
coordination and failure recovery service for Yahoo! Message
Broker, which is a highly scalable publish-subscribe system
managing thousands of topics for replication and data
delivery.  It is used by the Fetching Service for Yahoo!
crawler, where it also manages failure recovery. A number of
Yahoo! advertising systems also use ZooKeeper to implement
reliable services.
> ZooKeeper已成功应用于许多工业领域。它是用在雅虎！作为雅虎的协调和故障恢复服务！
> Message Broker是一个高度可扩展的发布-订阅系统，管理数千个主题的复制和数据传递。
> 它被雅虎的爬虫抓取服务使用，它还管理故障恢复。多个雅虎广告系统也使用ZooKeeper来实现可靠的服务。

All users and developers are encouraged to join the
community and contribute their expertise. See the
[Zookeeper Project on Apache](http://zookeeper.apache.org/)
for more information.
> 鼓励所有用户和开发人员加入社区并贡献他们的专业知识。
> 有关更多信息，请参阅Apache上的Zookeeper项目。

