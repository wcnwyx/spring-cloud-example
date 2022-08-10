本文翻译自zookeeper官网[https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperStarted.md](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperStarted.md)

# ZooKeeper Getting Started Guide

## Getting Started: Coordinating Distributed Applications with ZooKeeper
> 入门：使用ZooKeeper协调分布式应用程序

This document contains information to get you started quickly with
ZooKeeper. It is aimed primarily at developers hoping to try it out, and
contains simple installation instructions for a single ZooKeeper server, a
few commands to verify that it is running, and a simple programming
example. Finally, as a convenience, there are a few sections regarding
more complicated installations, for example running replicated
deployments, and optimizing the transaction log. However for the complete
instructions for commercial deployments, please refer to the [ZooKeeper
Administrator's Guide](zookeeperAdmin.html).
> 本文档包含帮助您快速使用ZooKeeper的信息。它主要针对希望试用它的开发人员，包含单个
>ZooKeeper服务器的简单安装说明、一些验证它是否正在运行的命令，以及一个简单的编程示例。
>最后，为了方便起见，这里有几个小节介绍更复杂的安装，例如运行复制部署和优化事务日志。
>不过，有关商业部署的完整说明，请参阅 [ZooKeeper Administrator's Guide](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperAdmin.html)。

### Pre-requisites
> 预备条件

See [System Requirements](zookeeperAdmin.html#sc_systemReq) in the Admin guide.
> 请参阅管理指南中的 [系统要求](https://zookeeper.apache.org/doc/r3.5.9/zookeeperAdmin.html#sc_systemReq)。


### Download
> 下载

To get a ZooKeeper distribution, download a recent
[stable](http://zookeeper.apache.org/releases.html) release from one of the Apache Download
Mirrors.
> 要获得ZooKeeper发行版，请从Apache下载镜像之一下载最新的稳定版本。


### Standalone Operation
> 单机操作

Setting up a ZooKeeper server in standalone mode is
straightforward. The server is contained in a single JAR file,
so installation consists of creating a configuration.
> 在单机模式下设置ZooKeeper服务器非常简单。服务器包含在一个JAR文件中，
> 因此安装包括创建一个配置。

Once you've downloaded a stable ZooKeeper release unpack
it and cd to the root
> 一旦你下载了一个稳定的ZooKeeper版本，把它解包，然后cd到根目录下

To start ZooKeeper you need a configuration file. Here is a sample,
create it in **conf/zoo.cfg**:
> 要启动ZooKeeper，您需要一个配置文件。下面是一个示例，请在conf/zoo.cfg中创建它：


    tickTime=2000
    dataDir=/var/lib/zookeeper
    clientPort=2181


This file can be called anything, but for the sake of this
discussion call
it **conf/zoo.cfg**. Change the
value of **dataDir** to specify an
existing (empty to start with) directory.  Here are the meanings
for each of the fields:
> 这个文件可以被称为任何名称，但是为了便于讨论，可以将其命名为conf/zoo.cfg。
> 更改dataDir的值以指定现有的（空的）目录。以下是每个字段的含义：

* ***tickTime*** :
    the basic time unit in milliseconds used by ZooKeeper. It is
    used to do heartbeats and the minimum session timeout will be
    twice the tickTime.
    > ZooKeeper使用的基本时间单位是毫秒。它是用来做心跳和最小会话超时，是两倍的滴答时间。

* ***dataDir*** :
    the location to store the in-memory database snapshots and,
    unless specified otherwise, the transaction log of updates to the
    database.
    > 存储内存中数据库快照的位置，以及数据库更新的事务日志（除非另有指定）。

* ***clientPort*** :
    the port to listen for client connections
    > 监听客户端连接的端口

Now that you created the configuration file, you can start
ZooKeeper:
> 现在您已经创建了配置文件，可以启动ZooKeeper了：


    bin/zkServer.sh start


ZooKeeper logs messages using log4j -- more detail
available in the
[Logging](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.html#Logging)
section of the Programmer's Guide. You will see log messages
coming to the console (default) and/or a log file depending on
the log4j configuration.
> ZooKeeper使用log4j记录消息——更多详细信息请参见程序员指南的日志部分。
> 根据log4j配置，您将看到日志消息到达控制台（默认）和/或日志文件。

The steps outlined here run ZooKeeper in standalone mode. There is
no replication, so if ZooKeeper process fails, the service will go down.
This is fine for most development situations, but to run ZooKeeper in
replicated mode, please see [Running Replicated
ZooKeeper](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperStarted.md#sc_RunningReplicatedZooKeeper).
> 这里概述的步骤在单机模式下运行ZooKeeper。没有复制，因此如果ZooKeeper进程失败，
> 服务将停止。这对于大多数开发情况来说都很好，但是要在复制模式下运行ZooKeeper，
> 请参阅[Running Replicated ZooKeeper](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperStarted.md#sc_RunningReplicatedZooKeeper).


### Managing ZooKeeper Storage
> 管理Zookeeper的存储

For long running production systems ZooKeeper storage must
be managed externally (dataDir and logs). See the section on
[maintenance](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperAdmin.html#sc_maintenance) for
more details.
> 对于长时间运行的生产系统，必须从外部管理ZooKeeper存储（dataDir和日志）。
> 有关更多详细信息，请参阅[维护](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperAdmin.html#sc_maintenance) 部分。


### Connecting to ZooKeeper
> 连接到ZooKeeper


    $ bin/zkCli.sh -server 127.0.0.1:2181


This lets you perform simple, file-like operations.
> 这使您可以执行简单的、类似文件的操作。

Once you have connected, you should see something like:
> 一旦你连接上了，你将看到以下内容：


    Connecting to localhost:2181
    log4j:WARN No appenders could be found for logger (org.apache.zookeeper.ZooKeeper).
    log4j:WARN Please initialize the log4j system properly.
    Welcome to ZooKeeper!
    JLine support is enabled
    [zkshell: 0]

From the shell, type `help` to get a listing of commands that can be executed from the client, as in:
> 在shell中，键入“help”以获取可从客户端执行的命令列表，如下所示


    [zkshell: 0] help
    ZooKeeper host:port cmd args
        get path [watch]
        ls path [watch]
        set path data [version]
        delquota [-n|-b] path
        quit
        printwatches on|off
        create path data acl
        stat path [watch]
        listquota path
        history
        setAcl path acl
        getAcl path
        sync path
        redo cmdno
        addauth scheme auth
        delete path [version]
        deleteall path
        setquota -n|-b val path


From here, you can try a few simple commands to get a feel for this simple command line interface.  First, start by issuing the list command, as
in `ls`, yielding:
> 从这里，您可以尝试几个简单的命令来感受这个简单的命令行界面。首先，从发出list命令开始，如ls中所示，生成：


    [zkshell: 8] ls /
    [zookeeper]


Next, create a new znode by running `create /zk_test my_data`. This creates a new znode and associates the string "my_data" with the node.
You should see:
> 接下来，通过运行`create /zk_test my_data`创建一个新的znode。
> 这将创建一个新的znode并将字符串"my_data"与节点相关联。您应该看到：


    [zkshell: 9] create /zk_test my_data
    Created /zk_test


Issue another `ls /` command to see what the directory looks like:
> 发出另一个`ls /`命令来查看目录：


    [zkshell: 11] ls /
    [zookeeper, zk_test]


Notice that the zk_test directory has now been created.
> 注意，现在已经创建了zk_test目录。

Next, verify that the data was associated with the znode by running the `get` command, as in:
> 接下来，通过运行`get`命令验证数据是否与znode关联，如中所示：


    [zkshell: 12] get /zk_test
    my_data
    cZxid = 5
    ctime = Fri Jun 05 13:57:06 PDT 2009
    mZxid = 5
    mtime = Fri Jun 05 13:57:06 PDT 2009
    pZxid = 5
    cversion = 0
    dataVersion = 0
    aclVersion = 0
    ephemeralOwner = 0
    dataLength = 7
    numChildren = 0


We can change the data associated with zk_test by issuing the `set` command, as in:
> 我们可以通过发出`set`命令来更改与zk_test相关的数据，如下所示：


    [zkshell: 14] set /zk_test junk
    cZxid = 5
    ctime = Fri Jun 05 13:57:06 PDT 2009
    mZxid = 6
    mtime = Fri Jun 05 14:01:52 PDT 2009
    pZxid = 5
    cversion = 0
    dataVersion = 1
    aclVersion = 0
    ephemeralOwner = 0
    dataLength = 4
    numChildren = 0
    [zkshell: 15] get /zk_test
    junk
    cZxid = 5
    ctime = Fri Jun 05 13:57:06 PDT 2009
    mZxid = 6
    mtime = Fri Jun 05 14:01:52 PDT 2009
    pZxid = 5
    cversion = 0
    dataVersion = 1
    aclVersion = 0
    ephemeralOwner = 0
    dataLength = 4
    numChildren = 0


(Notice we did a `get` after setting the data and it did, indeed, change.
> 注意，在设置数据之后，我们做了一个`get`，它确实发生了变化。

Finally, let's `delete` the node by issuing:
> 最后，让我们通过发出以下命令来`delete`节点：


    [zkshell: 16] delete /zk_test
    [zkshell: 17] ls /
    [zookeeper]
    [zkshell: 18]


That's it for now.  To explore more, continue with the rest of this document and see the [Programmer's Guide](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.html).
> 现在就这样。要了解更多信息，请继续阅读本文档的其余部分，并参阅[Programmer's Guide](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.html)。

### Programming to ZooKeeper
> 对ZooKeeper编程

ZooKeeper has a Java bindings and C bindings. They are
functionally equivalent. The C bindings exist in two variants: single
threaded and multi-threaded. These differ only in how the messaging loop
is done. For more information, see the [Programming
Examples in the ZooKeeper Programmer's Guide](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.html#ch_programStructureWithExample) for
sample code using of the different APIs.
> ZooKeeper有Java绑定和C绑定。它们在功能上是等价的。
> C绑定有两种变体：单线程和多线程。它们的区别仅在于消息传递循环是如何完成的。
> 有关更多信息，请[Programming Examples in the ZooKeeper Programmer's Guide](https://github.com/apache/zookeeper/blob/branch-3.5.9/zookeeper-docs/src/main/resources/markdown/zookeeperProgrammers.html#ch_programStructureWithExample)，
> 以获取使用不同API的示例代码。

### Running Replicated ZooKeeper
> 运行复制的（多个进程）ZooKeeper

Running ZooKeeper in standalone mode is convenient for evaluation,
some development, and testing. But in production, you should run
ZooKeeper in replicated mode. A replicated group of servers in the same
application is called a _quorum_, and in replicated
mode, all servers in the quorum have copies of the same configuration
file.
> 在单机模式下运行ZooKeeper便于评估、某些开发和测试。但在生产环境中，
> 应该以复制模式运行ZooKeeper。同一应用程序中的复制服务器组称为 _quorum_，
> 在复制模式下，quorum中的所有服务器都具有相同配置文件的副本。

######Note
>For replicated mode, a minimum of three servers are required,
and it is strongly recommended that you have an odd number of
servers. If you only have two servers, then you are in a
situation where if one of them fails, there are not enough
machines to form a majority quorum. Two servers is inherently
**less** stable than a single server, because there are two single
points of failure.
The required
**conf/zoo.cfg**
file for replicated mode is similar to the one used in standalone
mode, but with a few differences. Here is an example:
> 对于复制模式，至少需要三台服务器，强烈建议您使用奇数台服务器。如果您只有两个服务器，
> 那么您就处于这样一种情况：如果其中一个服务器出现故障，那么就没有足够的计算机来形成多数仲裁。
> 两台服务器本质上不如一台服务器稳定，因为存在两个单点故障。

    tickTime=2000
    dataDir=/var/lib/zookeeper
    clientPort=2181
    initLimit=5
    syncLimit=2
    server.1=zoo1:2888:3888
    server.2=zoo2:2888:3888
    server.3=zoo3:2888:3888

The new entry, **initLimit** is
timeouts ZooKeeper uses to limit the length of time the ZooKeeper
servers in quorum have to connect to a leader. The entry **syncLimit** limits how far out of date a server can
be from a leader.
> 新条目**initLimit**是超时ZooKeeper用于限制仲裁中ZooKeeper服务器必须连接到leader的时间长度。
> 条目**syncLimit**限制服务器与leader之间的过期距离。

With both of these timeouts, you specify the unit of time using
**tickTime**. In this example, the timeout
for initLimit is 5 ticks at 2000 milleseconds a tick, or 10
seconds.
> 对于这两个超时，可以使用**tickTime**指定时间单位。
> 在本例中，initLimit的超时时间为5个滴答，每滴答2000毫秒，即10秒。

The entries of the form _server.X_ list the
servers that make up the ZooKeeper service. When the server starts up,
it knows which server it is by looking for the file
_myid_ in the data directory. That file has the
contains the server number, in ASCII.
> 表单 _server.X_ 的条目列出了组成ZooKeeper服务的服务器。当服务器启动时，
> 它通过在数据目录中查找文件_myid_来知道它是哪台服务器。该文件包含服务器号（ASCII）。

Finally, note the two port numbers after each server
name: " 2888" and "3888". Peers use the former port to connect
to other peers. Such a connection is necessary so that peers
can communicate, for example, to agree upon the order of
updates. More specifically, a ZooKeeper server uses this port
to connect followers to the leader. When a new leader arises, a
follower opens a TCP connection to the leader using this
port. Because the default leader election also uses TCP, we
currently require another port for leader election. This is the
second port in the server entry.
> 最后，注意每个服务器名称后面的两个端口号："2888"和"3888"。
> 对等机使用前一个端口连接到其他对等机。这样的连接是必要的，以便对等方可以通信，
> 例如，商定更新的顺序。更具体地说，ZooKeeper服务器使用此端口将followers连接到leader。
> 当一个新的leader出现时，一个follower使用这个端口打开一个到leader的TCP连接。
> 因为默认的leader选举也使用TCP，所以我们目前需要另一个端口来进行leader选举。
> 这是服务器条目中的第二个端口。

######Note
>If you want to test multiple servers on a single
machine, specify the servername
as _localhost_ with unique quorum &
leader election ports (i.e. 2888:3888, 2889:3889, 2890:3890 in
the example above) for each server.X in that server's config
file. Of course separate _dataDir_s and
distinct _clientPort_s are also necessary
(in the above replicated example, running on a
single _localhost_, you would still have
three config files).
> 如果要在一台计算机上测试多个服务器，请为该服务器的配置文件中的每个server.X指定servername为_localhost_，
> 并具有唯一的仲裁和leader选择端口（即上面示例中的2888:3888、2889:3889、2890:3890）。
> 当然，还需要单独的_dataDir_和不同的_clientPort_（在上面的复制示例中，在单个localhost上运行时，仍然有三个配置文件）。

>Please be aware that setting up multiple servers on a single
machine will not create any redundancy. If something were to
happen which caused the machine to die, all of the zookeeper
servers would be offline. Full redundancy requires that each
server have its own machine. It must be a completely separate
physical server. Multiple virtual machines on the same physical
host are still vulnerable to the complete failure of that host.
> 请注意，在一台机器上设置多个服务器不会产生任何冗余。如果发生了导致机器死机的事情，
> 所有zookeeper服务器都将脱机。完全冗余要求每个服务器都有自己的机器。
> 它必须是一个完全独立的物理服务器。同一物理主机上的多个虚拟机仍然容易受到该主机完全故障的影响。

### Other Optimizations
> 其它优化

There are a couple of other configuration parameters that can
greatly increase performance:
> 有几个其他配置参数可以大大提高性能：

* To get low latencies on updates it is important to
  have a dedicated transaction log directory. By default
  transaction logs are put in the same directory as the data
  snapshots and _myid_ file. The dataLogDir
  parameters indicates a different directory to use for the
  transaction logs.
  > 为了获得低延迟的更新，有一个专用的事务日志目录是很重要的。
  > 默认情况下，事务日志与数据快照和 _myid_ 文件放在同一目录中。
  > dataLogDir参数表示用于事务日志的不同目录。