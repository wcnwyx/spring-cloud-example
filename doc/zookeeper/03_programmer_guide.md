本文翻译自zookeeper官网[https://github.com/apache/zookeeper/blob/release-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.md](https://github.com/apache/zookeeper/blob/release-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.md)

# ZooKeeper Programmer's Guide

## Introduction

This document is a guide for developers wishing to create
distributed applications that take advantage of ZooKeeper's coordination
services. It contains conceptual and practical information.
> 本文档为希望创建利用ZooKeeper协调服务的分布式应用程序的开发人员提供了指南。
> 它包含概念和实用信息。

The first four sections of this guide present a higher level
discussions of various ZooKeeper concepts. These are necessary both for an
understanding of how ZooKeeper works as well how to work with it. It does
not contain source code, but it does assume a familiarity with the
problems associated with distributed computing. The sections in this first
group are:
> 本指南的前四节对各种ZooKeeper概念进行了更高层次的讨论。这些对于理解ZooKeeper如何工作
> 以及如何使用它都是必要的。它不包含源代码，但它假定您熟悉与分布式计算相关的问题。
> 第一组中的部分是：

* [The ZooKeeper Data Model](#ch_zkDataModel)
* [ZooKeeper Sessions](#ch_zkSessions)
* [ZooKeeper Watches](#ch_zkWatches)
* [Consistency Guarantees](#ch_zkGuarantees)

The next four sections provide practical programming
information. These are:
> 接下来的四节将提供实用的编程信息。这些是：

* [Building Blocks: A Guide to ZooKeeper Operations](#ch_guideToZkOperations)
* [Bindings](#ch_bindings)
* [Gotchas: Common Problems and Troubleshooting](#ch_gotchas)

The book concludes with an [appendix](#apx_linksToOtherInfo) containing links to other
useful, ZooKeeper-related information.
> 这本书最后附有一个附录，其中包含了其他有用的、与ZooKeeper相关的信息的链接。

Most of the information in this document is written to be accessible as
stand-alone reference material. However, before starting your first
ZooKeeper application, you should probably at least read the chapters on
the [ZooKeeper Data Model](#ch_zkDataModel) and [ZooKeeper Basic Operations](#ch_guideToZkOperations).
> 本文档中的大部分信息都是作为独立的参考资料编写的。但是，在启动第一个ZooKeeper应用程序之前，
> 您可能至少应该阅读有关ZooKeeper数据模型和ZooKeeper基本操作的章节。

<a name="ch_zkDataModel"></a>

## The ZooKeeper Data Model

ZooKeeper has a hierarchal name space, much like a distributed file
system. The only difference is that each node in the namespace can have
data associated with it as well as children. It is like having a file
system that allows a file to also be a directory. Paths to nodes are
always expressed as canonical, absolute, slash-separated paths; there are
no relative reference. Any unicode character can be used in a path subject
to the following constraints:
> ZooKeeper有一个分层的名称空间，很像一个分布式文件系统。唯一的区别是命名空间中的
> 每个节点都可以有与其关联的数据以及子节点。这就像有一个文件系统，允许一个文件也成
> 为一个目录。到节点的路径总是表示为规范的、绝对的、斜杠分隔的路径；没有相对引用。
> 任何unicode字符都可以在受以下约束的路径中使用：

* The null character (\\u0000) cannot be part of a path name. (This
  causes problems with the C binding.)
  > 空字符（\\u0000）不能是路径名的一部分(这会导致C绑定出现问题。）
* The following characters can't be used because they don't
  display well, or render in confusing ways: \\u0001 - \\u001F and \\u007F
  - \\u009F.
  > 下列字符无法使用，因为它们显示不好，或呈现方式混乱：\\u0001-\\u001F和\\u007F - \\u009F。

* The following characters are not allowed: \\ud800 - uF8FF,
  \\uFFF0 - uFFFF.
  > 不允许使用以下字符：\\ud800-uF8FF，\\uFFF0-uFFFF。
* The "." character can be used as part of another name, but "."
  and ".." cannot alone be used to indicate a node along a path,
  because ZooKeeper doesn't use relative paths. The following would be
  invalid: "/a/b/./c" or "/a/b/../c".
  > "."字符可以用作另一个名称的一部分，但是"."和".."不能单独用于指示路径上的节点，
  > 因为ZooKeeper不使用相对路径。以下内容无效："/a/b/./c"或"/a/b/../c"。
* The token "zookeeper" is reserved.
  > "zookeeper"标记已保留。

<a name="sc_zkDataModel_znodes"></a>

### ZNodes

Every node in a ZooKeeper tree is referred to as a
_znode_. Znodes maintain a stat structure that
includes version numbers for data changes, acl changes. The stat
structure also has timestamps. The version number, together with the
timestamp, allows ZooKeeper to validate the cache and to coordinate
updates. Each time a znode's data changes, the version number increases.
For instance, whenever a client retrieves data, it also receives the
version of the data. And when a client performs an update or a delete,
it must supply the version of the data of the znode it is changing. If
the version it supplies doesn't match the actual version of the data,
the update will fail. (This behavior can be overridden.)
> ZooKeeper树中的每个节点都称为znode。znode维护一个stat结构，其中包括数据更改、
> acl更改的版本号。stat结构也有时间戳。版本号和时间戳允许ZooKeeper验证缓存并协调更新。
> 每次znode的数据更改时，版本号都会增加。例如，每当客户机检索数据时，它也会接收数据的版本。
> 当客户机执行更新或删除时，它必须提供正在更改的znode的数据版本。
> 如果它提供的版本与数据的实际版本不匹配，则更新将失败(可以覆盖此行为。）

######Note

>In distributed application engineering, the word
_node_ can refer to a generic host machine, a
server, a member of an ensemble, a client process, etc. In the ZooKeeper
documentation, _znodes_ refer to the data nodes.
_Servers_ refers to machines that make up the
ZooKeeper service; _quorum peers_ refer to the
servers that make up an ensemble; client refers to any host or process
which uses a ZooKeeper service.
> 在分布式应用工程中，node一词可以指一般主机、服务器、集成成员、客户机进程等。
> 在ZooKeeper文档中，znode指的是数据节点。服务器是指组成ZooKeeper服务的机器；
> 仲裁对等点是指组成一个集合的服务器；客户机是指使用ZooKeeper服务的任何主机或进程。

Znodes are the main enitity that a programmer access. They have
several characteristics that are worth mentioning here.
> znode是程序员访问的主要元素。它们有几个值得一提的特点。

<a name="sc_zkDataMode_watches"></a>

#### Watches

Clients can set watches on znodes. Changes to that znode trigger
the watch and then clear the watch. When a watch triggers, ZooKeeper
sends the client a notification. More information about watches can be
found in the section
[ZooKeeper Watches](#ch_zkWatches).
> 客户可以在znode上设置监视。对znode的更改会触发监视，然后清除监视。
> 当监视触发时，ZooKeeper会向客户端发送一个通知。更多关于监视的信息可以在[ZooKeeper Watches](#ch_zkWatches)
> 一节中找到。

<a name="Data+Access"></a>

#### Data Access
> 数据存取

The data stored at each znode in a namespace is read and written
atomically. Reads get all the data bytes associated with a znode and a
write replaces all the data. Each node has an Access Control List
(ACL) that restricts who can do what.
> 存储在命名空间中每个znode上的数据是原子读写的。Reads获取与znode相关的所有数据字节，
> write替换所有数据。每个节点都有一个访问控制列表（ACL），限制谁可以做什么。

ZooKeeper was not designed to be a general database or large
object store. Instead, it manages coordination data. This data can
come in the form of configuration, status information, rendezvous, etc.
A common property of the various forms of coordination data is that
they are relatively small: measured in kilobytes.
The ZooKeeper client and the server implementations have sanity checks
to ensure that znodes have less than 1M of data, but the data should
be much less than that on average. Operating on relatively large data
sizes will cause some operations to take much more time than others and
will affect the latencies of some operations because of the extra time
needed to move more data over the network and onto storage media. If
large data storage is needed, the usually pattern of dealing with such
data is to store it on a bulk storage system, such as NFS or HDFS, and
store pointers to the storage locations in ZooKeeper.
> ZooKeeper不是设计成一个通用数据库或大型对象存储。相反，它管理协调数据。
> 这些数据可以以配置、状态信息、会合等形式出现。各种形式的协调数据的一个共同特点是它们
> 相对较小：以千字节为单位。ZooKeeper客户机和服务器实现都进行了健全性检查，
> 以确保znode的数据量小于1M，但数据量应该远小于平均值。在相对较大的数据大小上操作会
> 导致某些操作比其他操作花费更多的时间，并且会影响某些操作的延迟，因为通过网络和存储介质
> 移动更多数据需要额外的时间。如果需要大数据存储，通常处理此类数据的模式是将其存储在
> 大容量存储系统（如NFS或HDFS）上，并将指向ZooKeeper中存储位置的指针存储。

<a name="Ephemeral+Nodes"></a>

#### Ephemeral Nodes
> 临时节点

ZooKeeper also has the notion of ephemeral nodes. These znodes
exists as long as the session that created the znode is active. When
the session ends the znode is deleted. Because of this behavior
ephemeral znodes are not allowed to have children.
> ZooKeeper也有短暂节点的概念。只要创建znode的会话处于活动状态，
> 这些znode就存在。当会话结束时，znode被删除。由于这种行为，临时节点不允许有子节点。

<a name="Sequence+Nodes+--+Unique+Naming"></a>

#### Sequence Nodes -- Unique Naming
> 序列节点 -- 唯一命名

When creating a znode you can also request that
ZooKeeper append a monotonically increasing counter to the end
of path. This counter is unique to the parent znode. The
counter has a format of %010d -- that is 10 digits with 0
(zero) padding (the counter is formatted in this way to
simplify sorting), i.e. "<path>0000000001". See
[Queue
Recipe](recipes.html#sc_recipes_Queues) for an example use of this feature. Note: the
counter used to store the next sequence number is a signed int
(4bytes) maintained by the parent node, the counter will
overflow when incremented beyond 2147483647 (resulting in a
name "<path>-2147483648").
> 在创建znode时，还可以请求ZooKeeper在路径的末尾附加一个单调递增的计数器。
> 此计数器对于父znode是唯一的。计数器的格式为%010d，即10位数字，0（零）填充（计数器的格式为简化排序），
> 即“0000000001”。有关此功能的使用示例，请参见[Queue Recipe](recipes.html#sc_recipes_Queues)。
> 注意：用于存储下一个序列号的计数器是由父节点维护的有符号int（4字节），
> 当递增超过2147483647时，计数器将溢出（导致名称为"<path>-2147483648"）。

<a name="Container+Nodes"></a>

#### Container Nodes
> 容器节点

**Added in 3.5.3**

ZooKeeper has the notion of container znodes. Container znodes are
special purpose znodes useful for recipes such as leader, lock, etc.
When the last child of a container is deleted, the container becomes
a candidate to be deleted by the server at some point in the future.
> ZooKeeper有容器节点的概念。容器znode是一种特殊用途的znode，可用于诸如leader、
> lock等方法。当删除容器的最后一个子级时，该容器将成为服务器在将来某个时候删除的候选对象。

Given this property, you should be prepared to get
KeeperException.NoNodeException when creating children inside of
container znodes. i.e. when creating child znodes inside of container znodes
always check for KeeperException.NoNodeException and recreate the container
znode when it occurs.
> 给定此属性，在容器znode内创建子级时，应该准备好获取KeeperException.NoNodeException。
> 既在容器znode内部创建子znode时，始终检查KeeperException.NoNodeException，
> 并在发生时重新创建容器znode。

<a name="TTL+Nodes"></a>

#### TTL Nodes

**Added in 3.5.3**

When creating PERSISTENT or PERSISTENT_SEQUENTIAL znodes,
you can optionally set a TTL in milliseconds for the znode. If the znode
is not modified within the TTL and has no children it will become a candidate
to be deleted by the server at some point in the future.
> 创建持久或持久的连续znode时，可以选择为znode设置以毫秒为单位的TTL。
> 如果znode没有在TTL（Time To Live）内修改并且没有子节点，那么它将成为服务器在将来某个时候删除的候选节点。

Note: TTL Nodes must be enabled via System property as they
are disabled by default. See the [Administrator's Guide](zookeeperAdmin.html#sc_configuration) for
details. If you attempt to create TTL Nodes without the
proper System property set the server will throw
KeeperException.UnimplementedException.
> 注意：TTL节点必须通过系统属性启用，因为默认情况下它们被禁用。有关详细信息，
> 请参阅[Administrator's Guide](zookeeperAdmin.html#sc_configuration)。
> 如果您试图在没有适当的系统属性设置的情况下创建TTL节点，
> 服务器将抛出KeeperException.UnimplementedException。

<a name="sc_timeInZk"></a>
 
### Time in ZooKeeper

ZooKeeper tracks time multiple ways:
> ZooKeeper以多种方式跟踪时间：

* **Zxid**
  Every change to the ZooKeeper state receives a stamp in the
  form of a _zxid_ (ZooKeeper Transaction Id).
  This exposes the total ordering of all changes to ZooKeeper. Each
  change will have a unique zxid and if zxid1 is smaller than zxid2
  then zxid1 happened before zxid2.
  > 对ZooKeeper状态的每次更改都会收到 _zxid_ （ZooKeeper事务Id）形式的戳。
  > 这将向ZooKeeper公开所有更改的总顺序。每个变化都有一个唯一的zxid，
  > 如果zxid1小于zxid2，那么zxid1发生在zxid2之前。
* **Version numbers**
  Every change to a node will cause an increase to one of the
  version numbers of that node. The three version numbers are version
  (number of changes to the data of a znode), cversion (number of
  changes to the children of a znode), and aversion (number of changes
  to the ACL of a znode).
  > 对节点的每次更改都会导致该节点的某个版本号增加。三个版本号分别是
  >version（对znode数据的更改数）、cversion（对znode的子级的更改数）和aversion（对znode的ACL的更改数）。
* **Ticks**
  When using multi-server ZooKeeper, servers use ticks to define
  timing of events such as status uploads, session timeouts,
  connection timeouts between peers, etc. The tick time is only
  indirectly exposed through the minimum session timeout (2 times the
  tick time); if a client requests a session timeout less than the
  minimum session timeout, the server will tell the client that the
  session timeout is actually the minimum session timeout.
  > 当使用多服务器ZooKeeper时，服务器使用ticks来定义事件的计时，如状态上载、会话超时、对等方之间的连接超时等。
  > tick时间仅通过最小会话超时（tick时间的2倍）间接公开；如果客户机请求的会话超时小于最小会话超时，
  > 服务器将告诉客户机会话超时实际上是最小会话超时。
* **Real time**
  ZooKeeper doesn't use real time, or clock time, at all except
  to put timestamps into the stat structure on znode creation and
  znode modification.
  > ZooKeeper除了在znode创建和znode修改时将时间戳放入stat结构之外，根本不使用实时或时钟时间。

<a name="sc_zkStatStructure"></a>

### ZooKeeper Stat Structure

The Stat structure for each znode in ZooKeeper is made up of the
following fields:
> ZooKeeper中每个znode的Stat结构由以下字段组成：

* **czxid**
  The zxid of the change that caused this znode to be
  created.
  > 创建此节点的事务id
* **mzxid**
  The zxid of the change that last modified this znode.
  > 最后一次更改此节点的事务id
* **pzxid**
  The zxid of the change that last modified children of this znode.
  > 最后一次更改该节点的子节点的事务id
* **ctime**
  The time in milliseconds from epoch when this znode was
  created.
  > 节点被创建爱你的事件（毫秒单位）
* **mtime**
  The time in milliseconds from epoch when this znode was last
  modified.
  > 节点最后一次更新时间
* **version**
  The number of changes to the data of this znode.
  > 该节点的数据版本号
* **cversion**
  The number of changes to the children of this znode.
  > 该节点的子节点版本号
* **aversion**
  The number of changes to the ACL of this znode.
  > 该节点的ACL版本号
* **ephemeralOwner**
  The session id of the owner of this znode if the znode is an
  ephemeral node. If it is not an ephemeral node, it will be
  zero.
  > 创建该临时节点的sessionId。如果不是临时节点，值为0
* **dataLength**
  The length of the data field of this znode.
  > 该节点数据内容的长度
* **numChildren**
  The number of children of this znode.
  > 该节点的子节点个数

<a name="ch_zkSessions"></a>

## ZooKeeper Sessions

A ZooKeeper client establishes a session with the ZooKeeper
service by creating a handle to the service using a language
binding. Once created, the handle starts off in the CONNECTING state
and the client library tries to connect to one of the servers that
make up the ZooKeeper service at which point it switches to the
CONNECTED state. During normal operation the client handle will be in one of these
two states. If an unrecoverable error occurs, such as session
expiration or authentication failure, or if the application explicitly
closes the handle, the handle will move to the CLOSED state.
The following figure shows the possible state transitions of a
ZooKeeper client:
> ZooKeeper客户端通过使用语言绑定创建服务的句柄来建立与ZooKeeper服务的会话。
> 一旦创建，句柄将在CONNECTING状态下启动，客户端库将尝试连接到构成ZooKeeper服务的其中一个服务器，
> 此时它将切换到CONNECTED状态。在正常操作期间，客户端句柄将处于这两种状态之一。
> 如果发生不可恢复的错误，例如会话过期或身份验证失败，或者如果应用程序显式关闭句柄，
> 则句柄将移动到CLOSED状态。下图显示了ZooKeeper客户端可能的状态转换：

![State transitions](https://github.com/apache/zookeeper/raw/release-3.5.9/zookeeper-docs/src/main/resources/markdown/images/state_dia.jpg)

To create a client session the application code must provide
a connection string containing a comma separated list of host:port pairs,
each corresponding to a ZooKeeper server (e.g. "127.0.0.1:4545" or
"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"). The ZooKeeper
client library will pick an arbitrary server and try to connect to
it. If this connection fails, or if the client becomes
disconnected from the server for any reason, the client will
automatically try the next server in the list, until a connection
is (re-)established.
> 要创建客户机会话，应用程序代码必须提供一个包含逗号分隔的会话列表的连接字符串host:port对，
> 每个对应一个ZooKeeper服务器（例如"127.0.0.1:4545"或"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"）。
> ZooKeeper客户端库将选择任意服务器并尝试连接到它。如果此连接失败，或者客户端由于任何原因与服务器断开连接，
> 客户端将自动尝试列表中的下一个服务器，直到（重新）建立连接。

**Added in 3.2.0**: An
optional "chroot" suffix may also be appended to the connection
string. This will run the client commands while interpreting all
paths relative to this root (similar to the unix chroot
command). If used the example would look like:
"127.0.0.1:4545/app/a" or
"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a" where the
client would be rooted at "/app/a" and all paths would be relative
to this root - ie getting/setting/etc...  "/foo/bar" would result
in operations being run on "/app/a/foo/bar" (from the server
perspective). This feature is particularly useful in multi-tenant
environments where each user of a particular ZooKeeper service
could be rooted differently. This makes re-use much simpler as
each user can code his/her application as if it were rooted at
"/", while actual location (say /app/a) could be determined at
deployment time.
> 可选的“chroot”后缀也可以附加到连接字符串。这将运行客户机命令，
> 同时解释与此根目录相关的所有路径（类似于unix chroot命令）。
> 如果使用该示例则类似于："127.0.0.1:4545/app/a"或"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"，
> 其中客户端的根目录为"/app/a"，并且所有路径都将与此根目录相关-即获取/设置/等等。
> "/foo/bar"将导致在“/app/a/foo/bar”上运行操作（从服务器角度）。此功能在多租户环境中特别有用，
> 在这些环境中，特定ZooKeeper服务的每个用户的根目录都可能不同。这使得重用变得更加简单，
> 因为每个用户都可以编写他/她的应用程序，就好像它的根在"/"，而实际位置（比如/app/a）可以在部署时确定。

When a client gets a handle to the ZooKeeper service,
ZooKeeper creates a ZooKeeper session, represented as a 64-bit
number, that it assigns to the client. If the client connects to a
different ZooKeeper server, it will send the session id as a part
of the connection handshake.  As a security measure, the server
creates a password for the session id that any ZooKeeper server
can validate.The password is sent to the client with the session
id when the client establishes the session. The client sends this
password with the session id whenever it reestablishes the session
with a new server.
> 当客户机获得ZooKeeper服务的句柄时，ZooKeeper将创建一个ZooKeeper会话（表示为64位数字），
> 并将该会话分配给客户机。如果客户端连接到不同的ZooKeeper服务器，它将发送会话id作为连接握手的一部分。
> 作为一种安全措施，服务器为会话id创建一个任何ZooKeeper服务器都可以验证的密码。
> 当客户端建立会话时，该密码将与会话id一起发送给客户端。每当客户机与新服务器重新建立会话时，
> 它都会将此密码与会话id一起发送。

One of the parameters to the ZooKeeper client library call
to create a ZooKeeper session is the session timeout in
milliseconds. The client sends a requested timeout, the server
responds with the timeout that it can give the client. The current
implementation requires that the timeout be a minimum of 2 times
the tickTime (as set in the server configuration) and a maximum of
20 times the tickTime. The ZooKeeper client API allows access to
the negotiated timeout.
> ZooKeeper客户端库调用创建ZooKeeper会话的参数之一是会话超时（以毫秒为单位）。
> 客户机发送一个请求的超时，服务器用它可以给客户机的超时来响应。
> 当前的实现要求超时时间至少是tickTime的2倍（在服务器配置中设置），
> 最多是tickTime的20倍。ZooKeeper客户端API允许访问协商超时。

When a client (session) becomes partitioned from the ZK
serving cluster it will begin searching the list of servers that
were specified during session creation. Eventually, when
connectivity between the client and at least one of the servers is
re-established, the session will either again transition to the
"connected" state (if reconnected within the session timeout
value) or it will transition to the "expired" state (if
reconnected after the session timeout). It is not advisable to
create a new session object (a new ZooKeeper.class or zookeeper
handle in the c binding) for disconnection. The ZK client library
will handle reconnect for you. In particular we have heuristics
built into the client library to handle things like "herd effect",
etc... Only create a new session when you are notified of session
expiration (mandatory).
> 当客户机（会话）从ZK服务集群分割时，它将开始搜索会话创建期间指定的服务器列表。
> 最后，当客户端和至少一个服务器之间的连接重新建立时，会话将再次转换为“已连接”状态
> （如果在会话超时值内重新连接），或者转换为“已过期”状态（如果在会话超时后重新连接）。
> 不建议为断开连接而创建新的会话对象（c绑定中的新ZooKeeper.class或ZooKeeper句柄）。
> ZK客户端库将为您处理重新连接。特别是，我们在客户端库中内置了启发式算法来处理“羊群效应”等问题。
> 仅在收到会话过期通知时创建新会话（强制）。

Session expiration is managed by the ZooKeeper cluster
itself, not by the client. When the ZK client establishes a
session with the cluster it provides a "timeout" value detailed
above. This value is used by the cluster to determine when the
client's session expires. Expirations happens when the cluster
does not hear from the client within the specified session timeout
period (i.e. no heartbeat). At session expiration the cluster will
delete any/all ephemeral nodes owned by that session and
immediately notify any/all connected clients of the change (anyone
watching those znodes). At this point the client of the expired
session is still disconnected from the cluster, it will not be
notified of the session expiration until/unless it is able to
re-establish a connection to the cluster. The client will stay in
disconnected state until the TCP connection is re-established with
the cluster, at which point the watcher of the expired session
will receive the "session expired" notification.
> 会话过期由ZooKeeper集群本身管理，而不是由客户端管理。当ZK客户机与集群建立会话时，
> 它会提供一个"超时"值，详见上文。集群使用此值来确定客户端会话何时过期。
> 当集群在指定的会话超时期间（即没有心跳）内没有收到来自客户端的消息时，就会发生过期。
> 在会话到期时，集群将删除该会话所拥有的任何/所有临时节点，并立即将更改通知任何/所有连接的客户端（任何正在监视这些znode的人）。
> 此时，已过期会话的客户端仍与群集断开连接，除非它能够重新建立到群集的连接，否则不会收到会话过期的通知。
> 客户端将保持断开连接状态，直到与集群重新建立TCP连接，此时过期会话的观察者将收到"session expired"通知。

Example state transitions for an expired session as seen by
the expired session's watcher:
> 过期会话的观察者看到的过期会话的状态转换示例：

1. 'connected' : session is established and client
  is communicating with cluster (client/server communication is
  operating properly)
  > 会话已建立，客户端正在与群集通信（客户端/服务器通信正常）
1. .... client is partitioned from the
  cluster
  > 客户端已从集群中分割
1. 'disconnected' : client has lost connectivity
  with the cluster
  > 客户端已失去与群集的连接
1. .... time elapses, after 'timeout' period the
  cluster expires the session, nothing is seen by client as it is
  disconnected from cluster
  > 时间流逝，在'超时'时间段之后，集群将使会话过期，客户端看不到任何内容因为它已经和集群断开连接。
1. .... time elapses, the client regains network
  level connectivity with the cluster
  > 随着时间的推移，客户机恢复与集群的网络级连接
1. 'expired' : eventually the client reconnects to
  the cluster, it is then notified of the
  expiration
  > 最后，客户机重新连接到集群，然后被通知已过期

Another parameter to the ZooKeeper session establishment
call is the default watcher. Watchers are notified when any state
change occurs in the client. For example if the client loses
connectivity to the server the client will be notified, or if the
client's session expires, etc... This watcher should consider the
initial state to be disconnected (i.e. before any state changes
events are sent to the watcher by the client lib). In the case of
a new connection, the first event sent to the watcher is typically
the session connection event.
> ZooKeeper会话建立调用的另一个参数是默认的watcher。当客户端中发生任何状态更改时，都会通知watcher。
> 例如，如果客户端失去与服务器的连接，则会通知客户端，或者如果客户端的会话过期，等等。
> 此watcher应考虑断开连接的初始状态（即，在客户端lib向观察程序发送任何状态更改事件之前）。
> 在新连接的情况下，发送给watcher的第一个事件通常是会话连接事件。

The session is kept alive by requests sent by the client. If
the session is idle for a period of time that would timeout the
session, the client will send a PING request to keep the session
alive. This PING request not only allows the ZooKeeper server to
know that the client is still active, but it also allows the
client to verify that its connection to the ZooKeeper server is
still active. The timing of the PING is conservative enough to
ensure reasonable time to detect a dead connection and reconnect
to a new server.
> 会话通过客户端发送的请求保持活动状态。如果会话空闲一段时间会使会话超时，
> 则客户端将发送PING请求以使会话保持活动状态。此PING请求不仅允许ZooKeeper服务器
> 知道客户端仍处于活动状态，而且还允许客户端验证其到ZooKeeper服务器的连接是否仍处于活动状态。
> PING的计时非常保守，足以确保有合理的时间检测到死连接并重新连接到新服务器。

Once a connection to the server is successfully established
(connected) there are basically two cases where the client lib generates
connectionloss (the result code in c binding, exception in Java -- see
the API documentation for binding specific details) when either a synchronous or
asynchronous operation is performed and one of the following holds:

1. The application calls an operation on a session that is no
  longer alive/valid
1. The ZooKeeper client disconnects from a server when there
  are pending operations to that server, i.e., there is a pending asynchronous call.

**Added in 3.2.0 -- SessionMovedException**. There is an internal
exception that is generally not seen by clients called the SessionMovedException.
This exception occurs because a request was received on a connection for a session
which has been reestablished on a different server. The normal cause of this error is
a client that sends a request to a server, but the network packet gets delayed, so
the client times out and connects to a new server. When the delayed packet arrives at
the first server, the old server detects that the session has moved, and closes the
client connection. Clients normally do not see this error since they do not read
from those old connections. (Old connections are usually closed.) One situation in which this
condition can be seen is when two clients try to reestablish the same connection using
a saved session id and password. One of the clients will reestablish the connection
and the second client will be disconnected (causing the pair to attempt to re-establish
its connection/session indefinitely).
> 有一个内部异常称为SessionMovedException，它通常不会被客户端看到。
> 发生此异常的原因是，在一个链接的session上收到了一个请求，但是该session已经被重连到了另外一台服务商。
> 此错误的正常原因是客户端向服务器发送请求，但网络数据包被延迟，因此客户端超时并连接到新服务器。
> 当延迟的数据包到达第一台服务器时，旧服务器检测到会话已移动，并关闭客户端连接。
> 客户机通常不会看到此错误，因为他们不会从这些旧连接中读取数据。(旧的连接通常是关闭的。）
> 可以看到这种情况的一种情况是，两个客户端尝试使用保存的会话id和密码重新建立同一个连接。
> 其中一个客户机将重新建立连接，第二个客户机将断开连接（导致该对尝试无限期地重新建立其连接/会话）。

**Updating the list of servers**.  We allow a client to
update the connection string by providing a new comma separated list of host:port pairs,
each corresponding to a ZooKeeper server. The function invokes a probabilistic load-balancing
algorithm which may cause the client to disconnect from its current host with the goal
to achieve expected uniform number of connections per server in the new list.
In case the current host to which the client is connected is not in the new list
this call will always cause the connection to be dropped. Otherwise, the decision
is based on whether the number of servers has increased or decreased and by how much.
> 我们允许客户端通过提供一个新的逗号分隔的连接字符串列表来更新连接字符串host:port 对，
> 每个对应一个ZooKeeper服务器。该函数调用概率负载平衡算法，该算法可能导致客户端与其当前主机断开连接，
> 目的是在新列表中实现每个服务器预期的统一连接数。如果客户端连接到的当前主机不在新列表中，则此调用将始终导致断开连接。
> 否则，决定是基于服务器的数量是增加了还是减少了以及增加减少的量。

For example, if the previous connection string contained 3 hosts and now the list contains
these 3 hosts and 2 more hosts, 40% of clients connected to each of the 3 hosts will
move to one of the new hosts in order to balance the load. The algorithm will cause the client
to drop its connection to the current host to which it is connected with probability 0.4 and in this
case cause the client to connect to one of the 2 new hosts, chosen at random.
> 例如，如果以前的连接字符串包含3个主机，而现在列表包含这3个主机和另外2个主机，
> 则连接到这3个主机中每个主机的40%的客户端将移动到新主机之一，以平衡负载。
> 该算法将导致客户端以0.4的概率断开与当前主机的连接，在这种情况下，
> 将导致客户端连接到随机选择的两个新主机之一。

Another example -- suppose we have 5 hosts and now update the list to remove 2 of the hosts,
the clients connected to the 3 remaining hosts will stay connected, whereas all clients connected
to the 2 removed hosts will need to move to one of the 3 hosts, chosen at random. If the connection
is dropped, the client moves to a special mode where he chooses a new server to connect to using the
probabilistic algorithm, and not just round robin.
> 另一个例子——假设我们有5台主机，现在更新列表以删除其中的2台主机，连接到其余3台主机的客户机将保持连接，
> 而连接到2台已删除主机的所有客户机将需要移动到随机选择的3台主机中的一台。如果连接断开，
> 客户机将进入一种特殊模式，在这种模式下，他将使用概率算法（而不仅仅是循环）选择要连接的新服务器。

In the first example, each client decides to disconnect with probability 0.4 but once the decision is
made, it will try to connect to a random new server and only if it cannot connect to any of the new
servers will it try to connect to the old ones. After finding a server, or trying all servers in the
new list and failing to connect, the client moves back to the normal mode of operation where it picks
an arbitrary server from the connectString and attempts to connect to it. If that fails, it will continue
trying different random servers in round robin. (see above the algorithm used to initially choose a server)
> 在第一个示例中，每个客户机决定以0.4的概率断开连接，但一旦做出决定，它将尝试连接到随机的新服务器，
> 并且只有当它无法连接到任何新服务器时，才会尝试连接到旧服务器。在找到服务器或尝试新列表中的所有服务器但连接失败后，
> 客户端将返回到正常操作模式，从connectString中选择任意服务器并尝试连接到该服务器。
> 如果失败，它将继续在循环中尝试不同的随机服务器(请参见上面用于最初选择服务器的算法）

<a name="ch_zkWatches"></a>

## ZooKeeper Watches

All of the read operations in ZooKeeper - **getData()**, **getChildren()**, and **exists()** - have the option of setting a watch as a
side effect. Here is ZooKeeper's definition of a watch: a watch event is
one-time trigger, sent to the client that set the watch, which occurs when
the data for which the watch was set changes. There are three key points
to consider in this definition of a watch:
> ZooKeeper中的所有读取操作 - **getData()**, **getChildren()**, and **exists()** - 都可以选择将监视设置为副作用。
> 以下是ZooKeeper对watch的定义：watch事件是一次性触发器，当设置watch的数据发生更改时, 发送给设置watch的客户端。
> 在watch的定义中，有三个关键点需要考虑：

* **One-time trigger**
  One watch event will be sent to the client when the data has changed.
  For example, if a client does a getData("/znode1", true) and later the
  data for /znode1 is changed or deleted, the client will get a watch
  event for /znode1. If /znode1 changes again, no watch event will be
  sent unless the client has done another read that sets a new
  watch.
  > **一次性触发器** 当数据发生更改时，将向客户端发送一个监视事件。例如，如果客户机执行getData("/znode1", true)，
  > 并且稍后/znode1的数据被更改或删除，则客户机将获得/znode1的监视事件。如果/znode1再次更改，则不会发送监视事件，
  > 除非客户端执行了另一次设置新监视的读取。
* **Sent to the client**
  This implies that an event is on the way to the client, but may
  not reach the client before the successful return code to the change
  operation reaches the client that initiated the change. Watches are
  sent asynchronously to watchers. ZooKeeper provides an ordering
  guarantee: a client will never see a change for which it has set a
  watch until it first sees the watch event. Network delays or other
  factors may cause different clients to see watches and return codes
  from updates at different times. The key point is that everything seen
  by the different clients will have a consistent order.
  > **发送给客户端** 这意味着事件正在到达客户端的途中，但在更改操作的成功返回码到达发起更改的客户端之前可能无法到达客户端。 
  > Watches异步发送给观察者。 ZooKeeper提供了排序保证：客户端永远不会看到它设置了监视的更改，直到它第一次看到监视事件。 
  > 网络延迟或其他因素可能会导致不同的客户端在不同时间查看更新并返回代码。 关键是不同客户端看到的一切都会有一个一致的顺序。
* **The data for which the watch was
  set**
  This refers to the different ways a node can change.  It
  helps to think of ZooKeeper as maintaining two lists of
  watches: data watches and child watches.  getData() and
  exists() set data watches. getChildren() sets child
  watches. Alternatively, it may help to think of watches being
  set according to the kind of data returned. getData() and
  exists() return information about the data of the node,
  whereas getChildren() returns a list of children.  Thus,
  setData() will trigger data watches for the znode being set
  (assuming the set is successful). A successful create() will
  trigger a data watch for the znode being created and a child
  watch for the parent znode. A successful delete() will trigger
  both a data watch and a child watch (since there can be no
  more children) for a znode being deleted as well as a child
  watch for the parent znode.
  > **watch设置的数据** 这是指节点可以改变的不同方式。 将 ZooKeeper 视为维护两个监视列表会有所帮助：数据监视和子监视。 
  > getData() 和 exists() 设置数据监视。 getChildren() 设置子节点监视。 或者，考虑根据返回的数据类型设置watch可能会有所帮助。 
  > getData() 和 exists() 返回有关节点数据的信息，而 getChildren() 返回子节点列表。 
  > 因此， setData() 将触发正在设置的 znode 的数据监视（假设设置成功）。 成功的 create() 将触发正在创建的 znode 的数据监视和父 znode 的子监视。 
  > 成功的 delete() 将同时触发数据监视和子监视（因为不能再有子节点）被删除的 znode 以及父 znode 的子监视。

Watches are maintained locally at the ZooKeeper server to which the
client is connected. This allows watches to be lightweight to set,
maintain, and dispatch. When a client connects to a new server, the watch
will be triggered for any session events. Watches will not be received
while disconnected from a server. When a client reconnects, any previously
registered watches will be reregistered and triggered if needed. In
general this all occurs transparently. There is one case where a watch
may be missed: a watch for the existence of a znode not yet created will
be missed if the znode is created and deleted while disconnected.
> 监视在客户端连接的ZooKeeper服务器上进行本地维护。这使得监视可以轻量级地设置、维护和调度。
> 当客户端连接到新服务器时，将触发对任何会话事件的监视。从服务器断开连接时将不会接收监视。
> 当客户端重新连接时，任何先前注册的监视都将被重新注册并在需要时触发。一般来说，这一切都是透明的。
> 有一种情况可能会丢失监视：如果在断开连接时创建并删除znode，则会丢失对尚未创建的znode存在的监视。

<a name="sc_WatchSemantics"></a>

### Semantics of Watches

We can set watches with the three calls that read the state of
ZooKeeper: exists, getData, and getChildren. The following list details
the events that a watch can trigger and the calls that enable them:
> 我们可以通过读取ZooKeeper状态的三个调用设置监视：exists、getData和getChildren。
> 以下列表详细说明了监视可以触发的事件以及启用这些事件的调用：

* **Created event:**
  Enabled with a call to exists.
  > 通过调用exists启用
* **Deleted event:**
  Enabled with a call to exists, getData, and getChildren.
  > 通过调用exists, getData, and getChildren启用
* **Changed event:**
  Enabled with a call to exists and getData.
  > 通过调用exists和getData启用
* **Child event:**
  Enabled with a call to getChildren.
  > 通过调用getChildren启用

<a name="sc_WatchRemoval"></a>

### Remove Watches

We can remove the watches registered on a znode with a call to
removeWatches. Also, a ZooKeeper client can remove watches locally even
if there is no server connection by setting the local flag to true. The
following list details the events which will be triggered after the
successful watch removal.
> 我们可以通过调用removeWatches来删除znode上注册的watches。
> 此外，ZooKeeper客户机可以通过将local标志设置为true，在本地删除监视，即使没有服务器连接。
> 以下列表详细说明了成功删除监视后将触发的事件。

* **Child Remove event:**
  Watcher which was added with a call to getChildren.
* **Data Remove event:**
  Watcher which was added with a call to exists or getData.

<a name="sc_WatchGuarantees"></a>

### What ZooKeeper Guarantees about Watches
> Zookeeper对Watches的保证

With regard to watches, ZooKeeper maintains these
guarantees:
> 关于watches，ZooKeeper保证：

* Watches are ordered with respect to other events, other
  watches, and asynchronous replies. The ZooKeeper client libraries
  ensures that everything is dispatched in order.
  > 监视是根据其他事件、其他监视和异步回复进行排序的。ZooKeeper客户端库确保按顺序调度所有内容。

* A client will see a watch event for a znode it is watching
  before seeing the new data that corresponds to that znode.
  > 在看到与该znode对应的新数据之前，客户端将看到它正在监视的znode的监视事件。

* The order of watch events from ZooKeeper corresponds to the
  order of the updates as seen by the ZooKeeper service.
  > ZooKeeper监视事件的顺序与ZooKeeper服务看到的更新顺序相对应。

<a name="sc_WatchRememberThese"></a>

### Things to Remember about Watches
> 关于Watches要记住的事情

* Watches are one time triggers; if you get a watch event and
  you want to get notified of future changes, you must set another
  watch.
  > watches是一次性触发器；如果您获得了一个监视事件，并且希望获得有关未来更改的通知，则必须设置另一个监视。

* Because watches are one time triggers and there is latency
  between getting the event and sending a new request to get a watch
  you cannot reliably see every change that happens to a node in
  ZooKeeper. Be prepared to handle the case where the znode changes
  multiple times between getting the event and setting the watch
  again. (You may not care, but at least realize it may
  happen.)
  > 因为标准的监视是一次性触发器，并且在获取事件和发送新请求获取监视之间存在延迟，
  > 所以您无法可靠地看到ZooKeeper中节点发生的每一个更改。
  > 准备好处理znode在获取事件和再次设置watch之间多次更改的情况(你可能不在乎，但至少要意识到这可能发生。）

* A watch object, or function/context pair, will only be
  triggered once for a given notification. For example, if the same
  watch object is registered for an exists and a getData call for the
  same file and that file is then deleted, the watch object would
  only be invoked once with the deletion notification for the file.
  > 对于给定的通知，监视对象或函数/上下文对只会触发一次。例如，如果为exists注册了相同的监视对象，
  >并且为同一个文件调用了getData，然后删除了该文件，则该监视对象将只被调用一次，并带有该文件的删除通知。

* When you disconnect from a server (for example, when the
  server fails), you will not get any watches until the connection
  is reestablished. For this reason session events are sent to all
  outstanding watch handlers. Use session events to go into a safe
  mode: you will not be receiving events while disconnected, so your
  process should act conservatively in that mode.
  > 当您从服务器断开连接时（例如，当服务器发生故障时），在重新建立连接之前，
  >您将无法获得任何监视。因此，会话事件被发送到所有未完成的监视处理程序。
  >使用会话事件进入安全模式：在断开连接时您将不会接收事件，因此您的进程应该在该模式下保守地操作。

<a name="sc_ZooKeeperAccessControl"></a>

## ZooKeeper access control using ACLs

ZooKeeper uses ACLs to control access to its znodes (the
data nodes of a ZooKeeper data tree). The ACL implementation is
quite similar to UNIX file access permissions: it employs
permission bits to allow/disallow various operations against a
node and the scope to which the bits apply. Unlike standard UNIX
permissions, a ZooKeeper node is not limited by the three standard
scopes for user (owner of the file), group, and world
(other). ZooKeeper does not have a notion of an owner of a
znode. Instead, an ACL specifies sets of ids and permissions that
are associated with those ids.
> ZooKeeper使用acl控制对其znode（ZooKeeper数据树的数据节点）的访问。
> ACL实现与UNIX文件访问权限非常相似：它使用权限位来允许/禁止对节点和应用权限位的范围执行各种操作。
> 与标准UNIX权限不同，ZooKeeper节点不受user（文件所有者）、group和world（其他）三个标准作用域的限制。
> ZooKeeper没有znode所有者的概念。相反，ACL指定与这些id相关联的id和权限集。

Note also that an ACL pertains only to a specific znode. In
particular it does not apply to children. For example, if
_/app_ is only readable by ip:172.16.16.1 and
_/app/status_ is world readable, anyone will
be able to read _/app/status_; ACLs are not
recursive.
> 还要注意，ACL只属于特定的znode。尤其不适用于子节点。例如，如果 _/app_ 只能由ip:172.16.16.1读取，
> _/app/status_是世界可读的，那么任何人都可以读取/app/status；ACL不是递归的。

ZooKeeper supports pluggable authentication schemes. Ids are
specified using the form _scheme:expression_,
where _scheme_ is the authentication scheme
that the id corresponds to. The set of valid expressions are defined
by the scheme. For example, _ip:172.16.16.1_ is
an id for a host with the address _172.16.16.1_
using the _ip_ scheme, whereas _digest:bob:password_
is an id for the user with the name of _bob_ using
the _digest_ scheme.
> ZooKeeper支持可插入的身份验证方案。ID是使用表单指定的 _scheme:expression_，
> 其中 _scheme_ 是id对应的身份验证方案。有效表达式集由方案定义。例如，_ip:172.16.16.1_ 
> 是使用 _ip_ 方案的地址为 _172.16.16.1_ 的主机的id，而 _digest:bob:password_ 
> 是使用 _digest_ 方案的名为 _bob_ 的用户的id。

When a client connects to ZooKeeper and authenticates
itself, ZooKeeper associates all the ids that correspond to a
client with the clients connection. These ids are checked against
the ACLs of znodes when a client tries to access a node. ACLs are
made up of pairs of _(scheme:expression,
perms)_. The format of
the _expression_ is specific to the scheme. For
example, the pair _(ip:19.22.0.0/16, READ)_
gives the _READ_ permission to any clients with
an IP address that starts with 19.22.
> 当客户机连接到ZooKeeper并对其自身进行身份验证时，ZooKeeper会将客户机对应的
> 所有ID与客户机连接相关联。当客户机尝试访问节点时，将根据znode的acl检查这些id。
> ACL由成对的 _(scheme:expression, perms)_ 组成。表达式的格式特定于方案。
> 例如，该对（ip:19.22.0.0/16，READ）向ip地址以19.22开头的任何客户机授予读取权限。

<a name="sc_ACLPermissions"></a>

### ACL Permissions

ZooKeeper supports the following permissions:
> ZooKeeper支持以下权限：

* **CREATE**: you can create a child node
* **READ**: you can get data from a node and list its children.
* **WRITE**: you can set data for a node
* **DELETE**: you can delete a child node
* **ADMIN**: you can set permissions

The _CREATE_
and _DELETE_ permissions have been broken out
of the _WRITE_ permission for finer grained
access controls. The cases for _CREATE_
and _DELETE_ are the following:
> _CREATE_ 和 _DELETE_ 权限已从细粒度访问控制的 _WRITE_ 权限中分离出来。
> _CREATE_ 和 _DELETE_ 的情况如下：

You want A to be able to do a set on a ZooKeeper node, but
not be able to _CREATE_
or _DELETE_ children.
> 你希望能够在ZooKeeper节点上进行设置，但不能 _创建_ 或 _删除_ 子节点。

_CREATE_
without _DELETE_: clients create requests by
creating ZooKeeper nodes in a parent directory. You want all
clients to be able to add, but only request processor can
delete. (This is kind of like the APPEND permission for
files.)
> CREATE without DELETE：客户端通过在父目录中创建ZooKeeper节点来创建请求。
> 你希望所有客户端都可以添加，但只有请求处理者可以删除(这有点像文件的附加权限。）

Also, the _ADMIN_ permission is there
since ZooKeeper doesn’t have a notion of file owner. In some
sense the _ADMIN_ permission designates the
entity as the owner. ZooKeeper doesn’t support the LOOKUP
permission (execute permission bit on directories to allow you
to LOOKUP even though you can't list the directory). Everyone
implicitly has LOOKUP permission. This allows you to stat a
node, but nothing more. (The problem is, if you want to call
zoo_exists() on a node that doesn't exist, there is no
permission to check.)
> 而且， _ADMIN_ 权限是存在的，因为ZooKeeper没有文件所有者的概念。在某种意义上，
> _ADMIN_ 权限将实体指定为所有者。ZooKeeper不支持查找权限（在目录上执行权限位以允许您查找，即使您不能列出目录）。
> 每个人都隐式拥有查找权限。这允许您统计一个节点，但仅此而已(问题是，如果要在不存在的节点上调用zoo_exists()，则没有检查的权限。）

_ADMIN_ permission also has a special role in terms of ACLs:
in order to retrieve ACLs of a znode user has to have _READ_ or _ADMIN_
 permission, but without _ADMIN_ permission, digest hash values will be 
masked out.
> _ADMIN_ 权限在acl方面也有一个特殊的角色：为了检索znode的acl，用户必须具有 _READ_ 或_ADMIN_ 权限，
> 但如果没有_ADMIN_ 权限，摘要哈希值将被屏蔽。

<a name="sc_BuiltinACLSchemes"></a>

#### Builtin ACL Schemes

ZooKeeeper has the following built in schemes:
>Zookeeper具有以下内置方案：

* **world** has a
  single id, _anyone_, that represents
  anyone.
  > 只有一个id，_anyone_，代表任何人。
* **auth** is a special
  scheme which ignores any provided expression and instead uses the current user,
  credentials, and scheme. Any expression (whether _user_ like with SASL
  authentication or _user:password_ like with DIGEST authentication) provided is ignored
  by the ZooKeeper server when persisting the ACL. However, the expression must still be
  provided in the ACL because the ACL must match the form _scheme:expression:perms_.
  This scheme is provided as a convenience as it is a common use-case for
  a user to create a znode and then restrict access to that znode to only that user.
  If there is no authenticated user, setting an ACL with the auth scheme will fail.
  > auth是一种特殊的方案，它忽略任何提供的表达式，而是使用当前用户、凭据和方案。在持久化ACL时，
  > ZooKeeper服务器将忽略提供的任何表达式。但是，由于ACL必须与表单匹配，因此必须在ACL中提供表达式 _scheme:expression:perms_。
  > 提供此方案是为了方便用户创建znode，然后将对该znode的访问限制为仅该用户，这是一个常见的用例。
  > 如果没有经过身份验证的用户，则使用身份验证方案设置ACL将失败。
* **digest** uses
  a _username:password_ string to generate
  MD5 hash which is then used as an ACL ID
  identity. Authentication is done by sending
  the _username:password_ in clear text. When
  used in the ACL the expression will be
  the _username:base64_
  encoded _SHA1_
  password _digest_.
  > 使用_username:password_ 字符串生成MD5散列，然后将其用作ACL ID标识。通过发送 _username:password_ 明文认证。
  > 在ACL中使用时，表达式将是 _username:base64_ 编码 _SHA1_ 密码 _摘要_。
* **ip** uses the
  client host IP as an ACL ID identity. The ACL expression is of
  the form _addr/bits_ where the most
  significant _bits_
  of _addr_ are matched against the most
  significant _bits_ of the client host
  IP.
  > 使用客户端主机IP作为ACL ID标识。ACL表达式的形式为addr/bits，其中addr的最高有效位与客户机主机IP的最高有效位相匹配。
* **x509** uses the client
  X500 Principal as an ACL ID identity. The ACL expression is the exact
  X500 Principal name of a client. When using the secure port, clients
  are automatically authenticated and their auth info for the x509 scheme
  is set.
  > x509使用客户端X500主体作为ACL ID标识。ACL表达式是客户端的确切X500主体名称。
  > 使用安全端口时，客户端将自动进行身份验证，并设置x509方案的身份验证信息。


<a name="sc_ZooKeeperPluggableAuthentication"></a>

## Pluggable ZooKeeper authentication
> 可插入的ZooKeeper身份验证

ZooKeeper runs in a variety of different environments with
various different authentication schemes, so it has a completely
pluggable authentication framework. Even the builtin authentication
schemes use the pluggable authentication framework.
> ZooKeeper运行在各种不同的环境中，具有各种不同的身份验证方案，因此它有一个完全可插入的身份验证框架。
> 即使是内置的身份验证方案也使用可插入的身份验证框架。

To understand how the authentication framework works, first you must
understand the two main authentication operations. The framework
first must authenticate the client. This is usually done as soon as
the client connects to a server and consists of validating information
sent from or gathered about a client and associating it with the connection.
The second operation handled by the framework is finding the entries in an
ACL that correspond to client. ACL entries are <_idspec,
permissions_> pairs. The _idspec_ may be
a simple string match against the authentication information associated
with the connection or it may be a expression that is evaluated against that
information. It is up to the implementation of the authentication plugin
to do the match. Here is the interface that an authentication plugin must
implement:
> 要了解身份验证框架的工作原理，首先必须了解两个主要的身份验证操作。框架首先必须对客户端进行身份验证。
> 这通常在客户机连接到服务器后立即完成，包括验证从客户机发送或收集的信息，并将其与连接相关联。
> 框架处理的第二个操作是在ACL中查找与客户端相对应的条目。ACL条目是<_idspec，permissions_>对。
> _idspec_ 可以是与连接相关联的身份验证信息的简单字符串匹配，也可以是根据该信息计算的表达式。
> 由验证插件的实现来完成匹配。以下是身份验证插件必须实现的接口：


    public interface AuthenticationProvider {
        String getScheme();
        KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte authData[]);
        boolean isValid(String id);
        boolean matches(String id, String aclExpr);
        boolean isAuthenticated();
    }


The first method _getScheme_ returns the string
that identifies the plugin. Because we support multiple methods of authentication,
an authentication credential or an _idspec_ will always be
prefixed with _scheme:_. The ZooKeeper server uses the scheme
returned by the authentication plugin to determine which ids the scheme
applies to.
> 第一个方法 _getScheme_ 返回标识插件的字符串。因为我们支持多种身份验证方法，
> 所以身份验证凭证或idspec总是有 _scheme:_ 前缀. ZooKeeper服务器使用身份验证插件返回的scheme来确定该方案应用于哪个id。

_handleAuthentication_ is called when a client
sends authentication information to be associated with a connection. The
client specifies the scheme to which the information corresponds. The
ZooKeeper server passes the information to the authentication plugin whose
_getScheme_ matches the scheme passed by the client. The
implementor of _handleAuthentication_ will usually return
an error if it determines that the information is bad, or it will associate information
with the connection using _cnxn.getAuthInfo().add(new Id(getScheme(), data))_.
> 当客户端发送要与连接关联的身份验证信息时，将调用 _handleAuthentication_。客户机指定信息对应的方案。
> ZooKeeper服务器将信息传递给身份验证插件，该插件的 _getScheme_ 与客户端传递的方案匹配。
> 如果 _handleAuthentication_ 的实现程序确定信息不正确，则通常会返回错误，
> 或者使用 _cnxn.getAuthInfo().add(new Id(getScheme(), data))_ 将信息与连接关联。

The authentication plugin is involved in both setting and using ACLs. When an
ACL is set for a znode, the ZooKeeper server will pass the id part of the entry to
the _isValid(String id)_ method. It is up to the plugin to verify
that the id has a correct form. For example, _ip:172.16.0.0/16_
is a valid id, but _ip:host.com_ is not. If the new ACL includes
an "auth" entry, _isAuthenticated_ is used to see if the
authentication information for this scheme that is assocatied with the connection
should be added to the ACL. Some schemes
should not be included in auth. For example, the IP address of the client is not
considered as an id that should be added to the ACL if auth is specified.
> 身份验证插件参与ACL的设置和使用。当为znode设置ACL时，ZooKeeper服务器将把条目的id部分传递给 _isValid(String id)_ 方法。
> 由插件来验证id的格式是否正确。例如， _ip:172.16.0.0/16_ 是有效的id，但 _ip:host.com_ 不是。
> 如果新ACL包含"auth" 条目，则使用 _isAuthenticated_ 查看是否应将与连接关联的此方案的身份验证信息添加到ACL。
> 有些方案不应该包含在auth中。例如，如果指定了auth，则客户端的IP地址不被视为应添加到ACL的id。

ZooKeeper invokes _matches(String id, String aclExpr)_ when checking an ACL. It
needs to match authentication information of the client against the relevant ACL
entries. To find the entries which apply to the client, the ZooKeeper server will
find the scheme of each entry and if there is authentication information
from that client for that scheme, _matches(String id, String aclExpr)_
will be called with _id_ set to the authentication information
that was previously added to the connection by _handleAuthentication_ and
_aclExpr_ set to the id of the ACL entry. The authentication plugin
uses its own logic and matching scheme to determine if _id_ is included
in _aclExpr_.
> ZooKeeper在检查ACL时调用 _matches(String id, String aclExpr)_ 。它需要将客户端的
> 身份验证信息与相关ACL条目相匹配。要查找应用于客户端的条目，ZooKeeper服务器将查找每个条目的方案，
> 如果该方案有来自该客户端的身份验证信息，则 _matches(String id, String aclExpr)_ 将被调用，
> _id_ 设置为先前通过 _handleAuthentication_ 添加到连接的身份验证信息，_aclExpr_ 设置为ACL条目的id。
> 身份验证插件使用自己的逻辑和匹配方案来确定 _aclExpr_中是否包含 _id_ 。

There are two built in authentication plugins: _ip_ and
_digest_. Additional plugins can adding using system properties. At
startup the ZooKeeper server will look for system properties that start with
"zookeeper.authProvider." and interpret the value of those properties as the class name
of an authentication plugin. These properties can be set using the
_-Dzookeeeper.authProvider.X=com.f.MyAuth_ or adding entries such as
the following in the server configuration file:
> 有两个内置的身份验证插件： _ip_ 和 _digest_。可以使用系统属性添加其他插件。
> 在启动时，ZooKeeper服务器将查找以"zookeeper.authProvider."开头的系统属性，
> 并将这些属性的值解释为身份验证插件的类名。可以使用 _-Dzookeeeper.authProvider.X=com.f.MyAuth_ 
> 或在服务器配置文件中添加以下项来设置这些属性：


    authProvider.1=com.f.MyAuth
    authProvider.2=com.f.MyAuth2


Care should be taking to ensure that the suffix on the property is unique. If there are
duplicates such as _-Dzookeeeper.authProvider.X=com.f.MyAuth -Dzookeeper.authProvider.X=com.f.MyAuth2_,
only one will be used. Also all servers must have the same plugins defined, otherwise clients using
the authentication schemes provided by the plugins will have problems connecting to some servers.
> 应注意确保属性上的后缀是唯一的。如果存在重复项，如 _-Dzookeeeper.authProvider.X=com.f.MyAuth -Dzookeeper.authProvider.X=com.f.MyAuth2_，
> 则只使用一个。此外，所有服务器都必须定义相同的插件，否则使用插件提供的身份验证方案的客户端将在连接到某些服务器时遇到问题。

<a name="ch_zkGuarantees"></a>

## Consistency Guarantees
> 一致性保证

ZooKeeper is a high performance, scalable service. Both reads and
write operations are designed to be fast, though reads are faster than
writes. The reason for this is that in the case of reads, ZooKeeper can
serve older data, which in turn is due to ZooKeeper's consistency
guarantees:

* *Sequential Consistency* :
    Updates from a client will be applied in the order that they
    were sent.

* *Atomicity* :
    Updates either succeed or fail -- there are no partial
    results.

* *Single System Image* :
    A client will see the same view of the service regardless of
    the server that it connects to.

* *Reliability* :
    Once an update has been applied, it will persist from that
    time forward until a client overwrites the update. This guarantee
    has two corollaries:
    1. If a client gets a successful return code, the update will
      have been applied. On some failures (communication errors,
      timeouts, etc) the client will not know if the update has
      applied or not. We take steps to minimize the failures, but the
      guarantee is only present with successful return codes.
      (This is called the _monotonicity condition_ in Paxos.)
    1. Any updates that are seen by the client, through a read
      request or successful update, will never be rolled back when
      recovering from server failures.

* *Timeliness* :
    The clients view of the system is guaranteed to be up-to-date
    within a certain time bound (on the order of tens of seconds).
    Either system changes will be seen by a client within this bound, or
    the client will detect a service outage.

Using these consistency guarantees it is easy to build higher level
functions such as leader election, barriers, queues, and read/write
revocable locks solely at the ZooKeeper client (no additions needed to
ZooKeeper). See [Recipes and Solutions](recipes.html)
for more details.

######Note

>Sometimes developers mistakenly assume one other guarantee that
ZooKeeper does _not_ in fact make. This is:
> * Simultaneously Consistent Cross-Client Views* :
    ZooKeeper does not guarantee that at every instance in
    time, two different clients will have identical views of
    ZooKeeper data. Due to factors like network delays, one client
    may perform an update before another client gets notified of the
    change. Consider the scenario of two clients, A and B. If client
    A sets the value of a znode /a from 0 to 1, then tells client B
    to read /a, client B may read the old value of 0, depending on
    which server it is connected to. If it
    is important that Client A and Client B read the same value,
    Client B should should call the **sync()** method from the ZooKeeper API
    method before it performs its read.
    So, ZooKeeper by itself doesn't guarantee that changes occur
    synchronously across all servers, but ZooKeeper
    primitives can be used to construct higher level functions that
    provide useful client synchronization. (For more information,
    see the [ZooKeeper Recipes](recipes.html).

<a name="ch_bindings"></a>

## Bindings

The ZooKeeper client libraries come in two languages: Java and C.
The following sections describe these.

<a name="Java+Binding"></a>

### Java Binding

There are two packages that make up the ZooKeeper Java binding:
**org.apache.zookeeper** and **org.apache.zookeeper.data**. The rest of the
packages that make up ZooKeeper are used internally or are part of the
server implementation. The **org.apache.zookeeper.data** package is made up of
generated classes that are used simply as containers.

The main class used by a ZooKeeper Java client is the **ZooKeeper** class. Its two constructors differ only
by an optional session id and password. ZooKeeper supports session
recovery accross instances of a process. A Java program may save its
session id and password to stable storage, restart, and recover the
session that was used by the earlier instance of the program.

When a ZooKeeper object is created, two threads are created as
well: an IO thread and an event thread. All IO happens on the IO thread
(using Java NIO). All event callbacks happen on the event thread.
Session maintenance such as reconnecting to ZooKeeper servers and
maintaining heartbeat is done on the IO thread. Responses for
synchronous methods are also processed in the IO thread. All responses
to asynchronous methods and watch events are processed on the event
thread. There are a few things to notice that result from this
design:

* All completions for asynchronous calls and watcher callbacks
  will be made in order, one at a time. The caller can do any
  processing they wish, but no other callbacks will be processed
  during that time.
* Callbacks do not block the processing of the IO thread or the
  processing of the synchronous calls.
* Synchronous calls may not return in the correct order. For
  example, assume a client does the following processing: issues an
  asynchronous read of node **/a** with
  _watch_ set to true, and then in the completion
  callback of the read it does a synchronous read of **/a**. (Maybe not good practice, but not illegal
  either, and it makes for a simple example.)
  Note that if there is a change to **/a** between the asynchronous read and the
  synchronous read, the client library will receive the watch event
  saying **/a** changed before the
  response for the synchronous read, but because the completion
  callback is blocking the event queue, the synchronous read will
  return with the new value of **/a**
  before the watch event is processed.

Finally, the rules associated with shutdown are straightforward:
once a ZooKeeper object is closed or receives a fatal event
(SESSION_EXPIRED and AUTH_FAILED), the ZooKeeper object becomes invalid.
On a close, the two threads shut down and any further access on zookeeper
handle is undefined behavior and should be avoided.

<a name="sc_java_client_configuration"></a>

#### Client Configuration Parameters

The following list contains configuration properties for the Java client. You can set any
of these properties using Java system properties. For server properties, please check the
[Server configuration section of the Admin Guide](zookeeperAdmin.html#sc_configuration).
The ZooKeeper Wiki also has useful pages about
[ZooKeeper SSL support](https://cwiki.apache.org/confluence/display/ZOOKEEPER/ZooKeeper+SSL+User+Guide), 
and [SASL authentication for ZooKeeper](https://cwiki.apache.org/confluence/display/ZOOKEEPER/ZooKeeper+and+SASL).


* *zookeeper.sasl.client* :
    Set the value to **false** to disable
    SASL authentication. Default is **true**.

* *zookeeper.sasl.clientconfig* :
    Specifies the context key in the JAAS login file. Default is "Client".

* *zookeeper.server.principal* :
    Specifies the server principal to be used by the client for authentication, while connecting to the zookeeper
    server, when Kerberos authentication is enabled. If this configuration is provided, then 
    the ZooKeeper client will NOT USE any of the following parameters to determine the server principal: 
    zookeeper.sasl.client.username, zookeeper.sasl.client.canonicalize.hostname, zookeeper.server.realm
    Note: this config parameter is working only for ZooKeeper 3.5.7+, 3.6.0+

* *zookeeper.sasl.client.username* :
    Traditionally, a principal is divided into three parts: the primary, the instance, and the realm.
    The format of a typical Kerberos V5 principal is primary/instance@REALM.
    zookeeper.sasl.client.username specifies the primary part of the server principal. Default
    is "zookeeper". Instance part is derived from the server IP. Finally server's principal is
    username/IP@realm, where username is the value of zookeeper.sasl.client.username, IP is
    the server IP, and realm is the value of zookeeper.server.realm.

* *zookeeper.sasl.client.canonicalize.hostname* :
    Expecting the zookeeper.server.principal parameter is not provided, the ZooKeeper client will try to
    determine the 'instance' (host) part of the ZooKeeper server principal. First it takes the hostname provided 
    as the ZooKeeper server connection string. Then it tries to 'canonicalize' the address by getting
    the fully qualified domain name belonging to the address. You can disable this 'canonicalization'
    by setting: zookeeper.sasl.client.canonicalize.hostname=false

* *zookeeper.server.realm* :
    Realm part of the server principal. By default it is the client principal realm.

* *zookeeper.disableAutoWatchReset* :
    This switch controls whether automatic watch resetting is enabled. Clients automatically
    reset watches during session reconnect by default, this option allows the client to turn off
    this behavior by setting zookeeper.disableAutoWatchReset to **true**.

* *zookeeper.client.secure* :
    **New in 3.5.5:**
    If you want to connect to the server secure client port, you need to set this property to
    **true**
    on the client. This will connect to server using SSL with specified credentials. Note that
    it requires the Netty client.

* *zookeeper.clientCnxnSocket* :
    Specifies which ClientCnxnSocket to be used. Possible values are
    **org.apache.zookeeper.ClientCnxnSocketNIO**
    and
    **org.apache.zookeeper.ClientCnxnSocketNetty**
    . Default is
    **org.apache.zookeeper.ClientCnxnSocketNIO**
    . If you want to connect to server's secure client port, you need to set this property to
    **org.apache.zookeeper.ClientCnxnSocketNetty**
    on client.

* *zookeeper.ssl.keyStore.location and zookeeper.ssl.keyStore.password* :
    **New in 3.5.5:**
    Specifies the file path to a JKS containing the local credentials to be used for SSL connections,
    and the password to unlock the file.

* *zookeeper.ssl.trustStore.location and zookeeper.ssl.trustStore.password* :
    **New in 3.5.5:**
    Specifies the file path to a JKS containing the remote credentials to be used for SSL connections,
    and the password to unlock the file.

* *zookeeper.ssl.keyStore.type* and *zookeeper.ssl.trustStore.type*:
    **New in 3.5.5:**
    Specifies the file format of keys/trust store files used to establish TLS connection to the ZooKeeper server. 
    Values: JKS, PEM, PKCS12 or null (detect by filename). Default: null.
    **New in 3.6.3, 3.7.0:**
    The format BCFKS was added.

* *jute.maxbuffer* :
    It specifies the maximum size of the incoming data from the server. The default value is 4194304
    Bytes , or just 4 MB. This is really a sanity check. The ZooKeeper server is designed to store and send
    data on the order of kilobytes. If incoming data length is more than this value, an IOException
    is raised.

* *zookeeper.kinit* :
    Specifies path to kinit binary. Default is "/usr/bin/kinit".

<a name="C+Binding"></a>

### C Binding

The C binding has a single-threaded and multi-threaded library.
The multi-threaded library is easiest to use and is most similar to the
Java API. This library will create an IO thread and an event dispatch
thread for handling connection maintenance and callbacks. The
single-threaded library allows ZooKeeper to be used in event driven
applications by exposing the event loop used in the multi-threaded
library.

The package includes two shared libraries: zookeeper_st and
zookeeper_mt. The former only provides the asynchronous APIs and
callbacks for integrating into the application's event loop. The only
reason this library exists is to support the platforms were a
_pthread_ library is not available or is unstable
(i.e. FreeBSD 4.x). In all other cases, application developers should
link with zookeeper_mt, as it includes support for both Sync and Async
API.

<a name="Installation"></a>

#### Installation

If you're building the client from a check-out from the Apache
repository, follow the steps outlined below. If you're building from a
project source package downloaded from apache, skip to step **3**.

1. Run `ant compile_jute` from the ZooKeeper
  top level directory (*.../trunk*).
  This will create a directory named "generated" under
  *.../trunk/zookeeper-client/zookeeper-client-c*.
1. Change directory to the*.../trunk/zookeeper-client/zookeeper-client-c*
  and run `autoreconf -if` to bootstrap **autoconf**, **automake** and **libtool**. Make sure you have **autoconf version 2.59** or greater installed.
  Skip to step**4**.
1. If you are building from a project source package,
  unzip/untar the source tarball and cd to the*
              zookeeper-x.x.x/zookeeper-client/zookeeper-client-c* directory.
1. Run `./configure <your-options>` to
  generate the makefile. Here are some of options the **configure** utility supports that can be
  useful in this step:
  * `--enable-debug`
    Enables optimization and enables debug info compiler
    options. (Disabled by default.)
  * `--without-syncapi`
    Disables Sync API support; zookeeper_mt library won't be
    built. (Enabled by default.)
  * `--disable-static`
    Do not build static libraries. (Enabled by
    default.)
  * `--disable-shared`
    Do not build shared libraries. (Enabled by
    default.)
######Note
>See INSTALL for general information about running **configure**.
1. Run `make` or `make
  install` to build the libraries and install them.
1. To generate doxygen documentation for the ZooKeeper API, run
  `make doxygen-doc`. All documentation will be
  placed in a new subfolder named docs. By default, this command
  only generates HTML. For information on other document formats,
  run `./configure --help`

<a name="Building+Your+Own+C+Client"></a>

#### Building Your Own C Client

In order to be able to use the ZooKeeper C API in your application
you have to remember to

1. Include ZooKeeper header: `#include <zookeeper/zookeeper.h>`
1. If you are building a multithreaded client, compile with
  `-DTHREADED` compiler flag to enable the multi-threaded version of
  the library, and then link against against the
  _zookeeper_mt_ library. If you are building a
  single-threaded client, do not compile with `-DTHREADED`, and be
  sure to link against the_zookeeper_st_library.

######Note
>See *.../trunk/zookeeper-client/zookeeper-client-c/src/cli.c*
for an example of a C client implementation

<a name="ch_guideToZkOperations"></a>

## Building Blocks: A Guide to ZooKeeper Operations

This section surveys all the operations a developer can perform
against a ZooKeeper server. It is lower level information than the earlier
concepts chapters in this manual, but higher level than the ZooKeeper API
Reference. 

<a name="sc_errorsZk"></a>

### Handling Errors

Both the Java and C client bindings may report errors. The Java client binding does so by throwing KeeperException, calling code() on the exception will return the specific error code. The C client binding returns an error code as defined in the enum ZOO_ERRORS. API callbacks indicate result code for both language bindings. See the API documentation (javadoc for Java, doxygen for C) for full details on the possible errors and their meaning.

<a name="sc_connectingToZk"></a>

### Connecting to ZooKeeper

Before we begin, you will have to set up a running Zookeeper server so that we can start developing the client. For C client bindings, we will be using the multithreaded library(zookeeper_mt) with a simple example written in C. To establish a connection with Zookeeper server, we make use of C API - _zookeeper_init_ with the following signature:

    int zookeeper_init(const char *host, watcher_fn fn, int recv_timeout, const clientid_t *clientid, void *context, int flags);

* **host* :
    Connection string to zookeeper server in the format of host:port. If there are multiple servers, use comma as separator after specifying the host:port pairs. Eg: "127.0.0.1:2181,127.0.0.1:3001,127.0.0.1:3002"

* *fn* :
    Watcher function to process events when a notification is triggered.

* *recv_timeout* :
    Session expiration time in milliseconds.

* *clientid* :
    We can specify 0 for a new session. If a session has already establish previously, we could provide that client ID and it would reconnect to that previous session.

* *context* :
    Context object that can be associated with the zkhandle_t handler. If it is not used, we can set it to 0.

* *flags* :
    In an initiation, we can leave it for 0.

We will demonstrate client that outputs "Connected to Zookeeper" after successful connection or an error message otherwise. Let's call the following code _zkClient.cc_ :


    #include <stdio.h>
    #include <zookeeper/zookeeper.h>
    #include <errno.h>
    using namespace std;

    // Keeping track of the connection state
    static int connected = 0;
    static int expired   = 0;

    // *zkHandler handles the connection with Zookeeper
    static zhandle_t *zkHandler;

    // watcher function would process events
    void watcher(zhandle_t *zkH, int type, int state, const char *path, void *watcherCtx)
    {
        if (type == ZOO_SESSION_EVENT) {

            // state refers to states of zookeeper connection.
            // To keep it simple, we would demonstrate these 3: ZOO_EXPIRED_SESSION_STATE, ZOO_CONNECTED_STATE, ZOO_NOTCONNECTED_STATE
            // If you are using ACL, you should be aware of an authentication failure state - ZOO_AUTH_FAILED_STATE
            if (state == ZOO_CONNECTED_STATE) {
                connected = 1;
            } else if (state == ZOO_NOTCONNECTED_STATE ) {
                connected = 0;
            } else if (state == ZOO_EXPIRED_SESSION_STATE) {
                expired = 1;
                connected = 0;
                zookeeper_close(zkH);
            }
        }
    }

    int main(){
        zoo_set_debug_level(ZOO_LOG_LEVEL_DEBUG);

        // zookeeper_init returns the handler upon a successful connection, null otherwise
        zkHandler = zookeeper_init("localhost:2181", watcher, 10000, 0, 0, 0);

        if (!zkHandler) {
            return errno;
        }else{
            printf("Connection established with Zookeeper. \n");
        }

        // Close Zookeeper connection
        zookeeper_close(zkHandler);

        return 0;
    }


Compile the code with the multithreaded library mentioned before.

`> g++ -Iinclude/ zkClient.cpp -lzookeeper_mt -o Client`

Run the client.

`> ./Client`

From the output, you should see "Connected to Zookeeper" along with Zookeeper's DEBUG messages if the connection is successful.

<a name="ch_gotchas"></a>

## Gotchas: Common Problems and Troubleshooting

So now you know ZooKeeper. It's fast, simple, your application
works, but wait ... something's wrong. Here are some pitfalls that
ZooKeeper users fall into:

1. If you are using watches, you must look for the connected watch
  event. When a ZooKeeper client disconnects from a server, you will
  not receive notification of changes until reconnected. If you are
  watching for a znode to come into existence, you will miss the event
  if the znode is created and deleted while you are disconnected.
1. You must test ZooKeeper server failures. The ZooKeeper service
  can survive failures as long as a majority of servers are active. The
  question to ask is: can your application handle it? In the real world
  a client's connection to ZooKeeper can break. (ZooKeeper server
  failures and network partitions are common reasons for connection
  loss.) The ZooKeeper client library takes care of recovering your
  connection and letting you know what happened, but you must make sure
  that you recover your state and any outstanding requests that failed.
  Find out if you got it right in the test lab, not in production - test
  with a ZooKeeper service made up of a several of servers and subject
  them to reboots.
1. The list of ZooKeeper servers used by the client must match the
  list of ZooKeeper servers that each ZooKeeper server has. Things can
  work, although not optimally, if the client list is a subset of the
  real list of ZooKeeper servers, but not if the client lists ZooKeeper
  servers not in the ZooKeeper cluster.
1. Be careful where you put that transaction log. The most
  performance-critical part of ZooKeeper is the transaction log.
  ZooKeeper must sync transactions to media before it returns a
  response. A dedicated transaction log device is key to consistent good
  performance. Putting the log on a busy device will adversely effect
  performance. If you only have one storage device, put trace files on
  NFS and increase the snapshotCount; it doesn't eliminate the problem,
  but it can mitigate it.
1. Set your Java max heap size correctly. It is very important to
  _avoid swapping._ Going to disk unnecessarily will
  almost certainly degrade your performance unacceptably. Remember, in
  ZooKeeper, everything is ordered, so if one request hits the disk, all
  other queued requests hit the disk.
  To avoid swapping, try to set the heapsize to the amount of
  physical memory you have, minus the amount needed by the OS and cache.
  The best way to determine an optimal heap size for your configurations
  is to _run load tests_. If for some reason you
  can't, be conservative in your estimates and choose a number well
  below the limit that would cause your machine to swap. For example, on
  a 4G machine, a 3G heap is a conservative estimate to start
  with.

## Links to Other Information

Outside the formal documentation, there're several other sources of
information for ZooKeeper developers.

* *[API Reference](https://zookeeper.apache.org/doc/current/apidocs/zookeeper-server/index.html)* :
    The complete reference to the ZooKeeper API

* *[ZooKeeper Talk at the Hadoop Summit 2008](https://www.youtube.com/watch?v=rXI9xiesUV8)* :
    A video introduction to ZooKeeper, by Benjamin Reed of Yahoo!
    Research

* *[Barrier and Queue Tutorial](https://cwiki.apache.org/confluence/display/ZOOKEEPER/Tutorial)* :
    The excellent Java tutorial by Flavio Junqueira, implementing
    simple barriers and producer-consumer queues using ZooKeeper.

* *[ZooKeeper - A Reliable, Scalable Distributed Coordination System](https://cwiki.apache.org/confluence/display/ZOOKEEPER/ZooKeeperArticles)* :
    An article by Todd Hoff (07/15/2008)

* *[ZooKeeper Recipes](recipes.html)* :
    Pseudo-level discussion of the implementation of various
    synchronization solutions with ZooKeeper: Event Handles, Queues,
    Locks, and Two-phase Commits.
