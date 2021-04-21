本文翻译自docker官网：[https://docs.docker.com/network/overlay/](https://docs.docker.com/network/overlay/)

#Use overlay networks

The `overlay` network driver creates a distributed network among multiple
Docker daemon hosts. This network sits on top of (overlays) the host-specific
networks, allowing containers connected to it (including swarm service
containers) to communicate securely when encryption is enabled. Docker
transparently handles routing of each packet to and from the correct Docker
daemon host and the correct destination container.
> `overlay`网络驱动程序在多个Docker守护程序主机之间创建一个分布式网络。
> 这个网络位于（覆盖）主机特定网络之上，允许连接到它的容器（包括swarm服务容器）在启用加密时安全地通信。
> Docker透明地处理每个数据包往返于正确的Docker守护程序主机和正确的目标容器的路由。

When you initialize a swarm or join a Docker host to an existing swarm, two
new networks are created on that Docker host:
> 当你初始化群或将Docker主机加入现有群时，将在该Docker主机上创建两个新网络：

- an overlay network called `ingress`, which handles control and data traffic
  related to swarm services. When you create a swarm service and do not
  connect it to a user-defined overlay network, it connects to the `ingress`
  network by default.
  > 一种称为`ingress`的overlay网络，处理与swarm服务相关的控制和数据通信。
  > 当您创建swarm服务而不将其连接到用户定义的覆盖网络时，默认情况下，它将连接到`ingress`网络。
- a bridge network called `docker_gwbridge`, which connects the individual
  Docker daemon to the other daemons participating in the swarm.
  > 一个称为`docker_gwbridge`的网桥网络，它将单个docker守护进程连接到参与swarm的其他守护进程。

You can create user-defined `overlay` networks using `docker network create`,
in the same way that you can create user-defined `bridge` networks. Services
or containers can be connected to more than one network at a time. Services or
containers can only communicate across networks they are each connected to.
> 可以使用`docker network create`创建用户定义的`overlay`网络，方法与创建用户定义的`bridge`网络相同。
> 服务或容器一次可以连接到多个网络。服务或容器只能通过各自连接的网络进行通信。

Although you can connect both swarm services and standalone containers to an
overlay network, the default behaviors and configuration concerns are different.
For that reason, the rest of this topic is divided into operations that apply to
all overlay networks, those that apply to swarm service networks, and those that
apply to overlay networks used by standalone containers.
> 尽管您可以将swarm服务和独立容器连接到overlay网络，但默认行为和配置关注点是不同的。
> 因此，本主题的其余部分分为适用于所有overlay网络的操作、适用于swarm服务网络的操作和适用于独立容器使用的overlay网络的操作。

## Operations for all overlay networks
> overlay网络的所有操作

### Create an overlay network
> 创建一个overlay网络

> **Prerequisites**:先决条件
>
> - Firewall rules for Docker daemons using overlay networks（使用覆盖网络的Docker守护程序的防火墙规则）  
    >
    >   You need the following ports open to traffic to and from each Docker host participating on an overlay network:  
    >   你需要开放以下端口，以便和参与overlay网络的每个Docker主机进行通信：   
>   - TCP port 2377 for cluster management communications（用于群集管理通信的TCP端口2377）
>   - TCP and UDP port 7946 for communication among nodes（用于节点间通信的TCP和UDP端口7946）
>   - UDP port 4789 for overlay network traffic（overlay网络流量的UDP端口4789）
>
> - Before you can create an overlay network, you need to either initialize your
>   Docker daemon as a swarm manager using `docker swarm init` or join it to an
>   existing swarm using `docker swarm join`. Either of these creates the default
>   `ingress` overlay network which is used by swarm services by default. You need
>   to do this even if you never plan to use swarm services. Afterward, you can
>   create additional user-defined overlay networks.   
> 在创建覆盖网络之前，您需要使用`docker swarm init`将Docker守护程序初始化为swarm manager，
> 或者使用`docker swarm join`将其连接到现有的swarm。其中任何一个都会创建默认的`ingress` overlay网络，
> swarm服务默认使用该网络。即使你从未打算使用swarm服务，你也需要这么做。之后，可以创建其他用户定义的覆盖网络。

To create an overlay network for use with swarm services, use a command like
the following:
> 要创建用于swarm服务的overlay网络，请使用如下命令：

```bash
$ docker network create -d overlay my-overlay
```

To create an overlay network which can be used by swarm services **or**
standalone containers to communicate with other standalone containers running on
other Docker daemons, add the `--attachable` flag:
> 要创建swarm服务**或**独立容器可用于与其他Docker守护进程上运行的其他独立容器通信的overlay网络，请添加`--attachable`标志：

```bash
$ docker network create -d overlay --attachable my-attachable-overlay
```

You can specify the IP address range, subnet, gateway, and other options. See
`docker network create --help` for details.
> 您可以指定IP地址范围、子网、网关和其他选项。有关详细信息，请参见`docker network create --help`。

### Encrypt traffic on an overlay network
> 加密overlay网络上的流量

All swarm service management traffic is encrypted by default, using the
[AES algorithm](https://en.wikipedia.org/wiki/Galois/Counter_Mode) in
GCM mode. Manager nodes in the swarm rotate the key used to encrypt gossip data
every 12 hours.
> 默认情况下，所有swarm服务管理流量都是在GCM模式下使用AES算法加密的。
> 群中的管理节点每12小时轮换一次用于加密八卦数据的密钥。

To encrypt application data as well, add `--opt encrypted` when creating the
overlay network. This enables IPSEC encryption at the level of the vxlan. This
encryption imposes a non-negligible performance penalty, so you should test this
option before using it in production.
> 要同时加密应用程序数据，请在创建覆盖网络时添加`--opt encrypted`。这将在vxlan级别启用IPSEC加密。
> 这种加密会带来不可忽略的性能损失，因此您应该在生产中使用此选项之前测试它。

When you enable overlay encryption, Docker creates IPSEC tunnels between all the
nodes where tasks are scheduled for services attached to the overlay network.
These tunnels also use the AES algorithm in GCM mode and manager nodes
automatically rotate the keys every 12 hours.
> 启用覆盖加密时，Docker将在为连接到overlay网络的服务计划任务的所有节点之间创建IPSEC隧道。
> 这些隧道还在GCM模式下使用AES算法，管理器节点每12小时自动旋转一次密钥。

> **Do not attach Windows nodes to encrypted overlay networks.**  
> **不要将Windows节点附加到加密的overlay网络。**
>
> Overlay network encryption is not supported on Windows. If a Windows node
> attempts to connect to an encrypted overlay network, no error is detected but
> the node cannot communicate.  
> Windows不支持overlay网络加密。如果Windows节点尝试连接到加密的overlay网络，则不会检测到错误，但该节点无法通信。

#### Swarm mode overlay networks and standalone containers
> Swarm模式overlay网络和独立容器

You can use the overlay network feature with both `--opt encrypted --attachable`
and attach unmanaged containers to that network:
> 您可以将overlay网络功能与`--opt encrypted --attachable`和将非托管容器附加到该网络一起使用：

```bash
$ docker network create --opt encrypted --driver overlay --attachable my-attachable-multi-host-network
```

### Customize the default ingress network
> 自定义默认入口网络

Most users never need to configure the `ingress` network, but Docker allows you
to do so. This can be useful if the automatically-chosen subnet conflicts with
one that already exists on your network, or you need to customize other low-level
network settings such as the MTU.
> 大多数用户不需要配置`ingress`网络，但Docker允许您这样做。如果自动选择的子网与网络上已存在的子网冲突，
> 或者需要自定义其他低级网络设置（如MTU），则此功能非常有用。

Customizing the `ingress` network involves removing and recreating it. This is
usually done before you create any services in the swarm. If you have existing
services which publish ports, those services need to be removed before you can
remove the `ingress` network.
> 自定义`ingress`网络包括删除和重新创建它。这通常是在swarm中创建任何服务之前完成的。
> 如果您有发布端口的现有服务，则需要先删除这些服务，然后才能删除`ingress`网络。

During the time that no `ingress` network exists, existing services which do not
publish ports continue to function but are not load-balanced. This affects
services which publish ports, such as a WordPress service which publishes port
80.
> 在不存在`ingress`网络的期间，不发布端口的现有服务继续工作，但负载不平衡。
> 这会影响发布端口的服务，例如发布端口80的WordPress服务。

1.  Inspect the `ingress` network using `docker network inspect ingress`, and
    remove any services whose containers are connected to it. These are services
    that publish ports, such as a WordPress service which publishes port 80. If
    all such services are not stopped, the next step fails.
    > 使用`docker network inspect ingress`检查`ingress`网络，并删除与其容器连接的所有服务。
    > 这些是发布端口的服务，例如发布端口80的WordPress服务。如果没有停止所有这些服务，下一步就会失败。

2.  Remove the existing `ingress` network:
    >删除现有的`ingress`网络:

    ```bash
    $ docker network rm ingress

    WARNING! Before removing the routing-mesh network, make sure all the nodes
    in your swarm run the same docker engine version. Otherwise, removal may not
    be effective and functionality of newly created ingress networks will be
    impaired.
    Are you sure you want to continue? [y/N]
    ```

3.  Create a new overlay network using the `--ingress` flag, along  with the
    custom options you want to set. This example sets the MTU to 1200, sets
    the subnet to `10.11.0.0/16`, and sets the gateway to `10.11.0.2`.
    > 使用`--ingress`标志以及要设置的自定义选项创建一个新的overlay网络。
    > 本例将MTU设置为1200，将子网设置为`10.11.0.0/16`，并将网关设置为`10.11.0.2`。

    ```bash
    $ docker network create \
      --driver overlay \
      --ingress \
      --subnet=10.11.0.0/16 \
      --gateway=10.11.0.2 \
      --opt com.docker.network.driver.mtu=1200 \
      my-ingress
    ```

    > **Note**: You can name your `ingress` network something other than
    > `ingress`, but you can only have one. An attempt to create a second one
    > fails.  
    > 您可以将`ingress`网络命名为`ingress`以外的名称，但您只能有一个。尝试创建第二个失败。

4.  Restart the services that you stopped in the first step.
> 重新启动第一步中停止的服务。

### Customize the docker_gwbridge interface
> 自定义docker_gwbridge接口

The `docker_gwbridge` is a virtual bridge that connects the overlay networks
(including the `ingress` network) to an individual Docker daemon's physical
network. Docker creates it automatically when you initialize a swarm or join a
Docker host to a swarm, but it is not a Docker device. It exists in the kernel
of the Docker host. If you need to customize its settings, you must do so before
joining the Docker host to the swarm, or after temporarily removing the host
from the swarm.
> `docker_gwbridge`是一个虚拟网桥，它将覆盖网络（包括`ingress`网络）连接到单个docker守护程序的物理网络。
> Docker在初始化swarm或将Docker主机加入swarm时自动创建它，但它不是Docker设备。它存在于Docker主机的内核中。
> 如果需要自定义其设置，则必须在将Docker主机加入swarm之前或从swarm中临时移除主机之后进行自定义。

1.  Stop Docker.
> 停止Docker。

2.  Delete the existing `docker_gwbridge` interface.
> 删除已存在的`docker_gwbridge`接口。

    ```bash
    $ sudo ip link set docker_gwbridge down

    $ sudo ip link del dev docker_gwbridge
    ```

3.  Start Docker. Do not join or initialize the swarm.
> 启动Docker。不要加入或初始化swarm。

4.  Create or re-create the `docker_gwbridge` bridge manually with your custom
    settings, using the `docker network create` command.
    This example uses the subnet `10.11.0.0/16`. For a full list of customizable
    options, see [Bridge driver options](../engine/reference/commandline/network_create.md#bridge-driver-options).
    > 使用`docker network create`命令，使用自定义设置手动创建或重新创建`docker_gwbridge`。
    > 本例使用子网10.11.0.0/16。有关可自定义选项的完整列表，请参见[Bridge driver options](https://docs.docker.com/engine/reference/commandline/network_create/#bridge-driver-options) 。

    ```bash
    $ docker network create \
    --subnet 10.11.0.0/16 \
    --opt com.docker.network.bridge.name=docker_gwbridge \
    --opt com.docker.network.bridge.enable_icc=false \
    --opt com.docker.network.bridge.enable_ip_masquerade=true \
    docker_gwbridge
    ```

5.  Initialize or join the swarm. Since the bridge already exists, Docker does
    not create it with automatic settings.
    > 初始化或加入swarm。由于网桥已经存在，Docker不会使用自动设置来创建它。

## Operations for swarm services

### Publish ports on an overlay network
> 在overlay网络上发布端口

Swarm services connected to the same overlay network effectively expose all
ports to each other. For a port to be accessible outside of the service, that
port must be _published_ using the `-p` or `--publish` flag on `docker service
create` or `docker service update`. Both the legacy colon-separated syntax and
the newer comma-separated value syntax are supported. The longer syntax is
preferred because it is somewhat self-documenting.
> 连接到同一覆盖网络的swarm服务有效地将所有端口相互公开。要在服务外部访问端口，
> 必须在docker service create`或`docker service update`上使用`-p`或`--publish`标志发布该端口。
> 支持传统的冒号分隔语法和较新的逗号分隔值语法。最好使用较长的语法，因为它有点自我记录(self-documenting)。

<table>
<thead>
<tr>
<th>Flag value</th>
<th>Description</th>
</tr>
</thead>
<tr>
<td><tt>-p 8080:80</tt> or<br /><tt>-p published=8080,target=80</tt></td>
<td>Map TCP port 80 on the service to port 8080 on the routing mesh.</td>
</tr>
<tr>
<td><tt>-p 8080:80/udp</tt> or<br /><tt>-p published=8080,target=80,protocol=udp</tt></td>
<td>Map UDP port 80 on the service to port 8080 on the routing mesh.</td>
</tr>
<tr>
<td><tt>-p 8080:80/tcp -p 8080:80/udp</tt> or <br /><tt>-p published=8080,target=80,protocol=tcp -p published=8080,target=80,protocol=udp</tt></td>
<td>Map TCP port 80 on the service to TCP port 8080 on the routing mesh, and map UDP port 80 on the service to UDP port 8080 on the routing mesh.</td>
</tr>
</table>

### Bypass the routing mesh for a swarm service
> 绕过swarm服务的路由网

By default, swarm services which publish ports do so using the routing mesh.
When you connect to a published port on any swarm node (whether it is running a
given service or not), you are redirected to a worker which is running that
service, transparently. Effectively, Docker acts as a load balancer for your
swarm services. Services using the routing mesh are running in _virtual IP (VIP)
mode_. Even a service running on each node (by means of the `--mode global`
flag) uses the routing mesh. When using the routing mesh, there is no guarantee
about which Docker node services client requests.
> 默认情况下，swarm服务使用路由网格进行发布端口。
> 当您连接到任何swarm节点上的已发布端口（无论它是否运行给定的服务）时，您都会被重定向到运行该服务的worker，这是透明的。
> Docker实际上是swarm服务的负载均衡器。使用路由网格的服务以 _虚拟IP（VIP）模式_ 运行。
> 甚至在每个节点上运行的服务（通过`--mode global`标志）也使用路由网格。
> 使用路由网格时，无法保证哪个Docker节点为客户端请求提供服务。

To bypass the routing mesh, you can start a service using _DNS Round Robin
(DNSRR) mode_, by setting the `--endpoint-mode` flag to `dnsrr`. You must run
your own load balancer in front of the service. A DNS query for the service name
on the Docker host returns a list of IP addresses for the nodes running the
service. Configure your load balancer to consume this list and balance the
traffic across the nodes.
> 要绕过路由网格，可以通过`--endpoint-mode`标志设置为`dnsrr`，使用 _DNS Round Robin (DNSRR)模式_ 启动服务。
> 您必须在服务前面运行自己的负载平衡器。Docker主机上服务名称的DNS查询返回运行该服务的节点的IP地址列表。
> 配置负载平衡器以使用此列表并平衡节点间的通信量。

### Separate control and data traffic
> 独立的控制和数据通信

By default, control traffic relating to swarm management and traffic to and from
your applications runs over the same network, though the swarm control traffic
is encrypted. You can configure Docker to use separate network interfaces for
handling the two different types of traffic. When you initialize or join the
swarm, specify `--advertise-addr` and `--datapath-addr` separately. You must do
this for each node joining the swarm.
> 默认情况下，尽管swarm控制流量是加密的，与swarm管理相关的控制流量以及应用程序之间的流量是在同一网络上运行的。
> 您可以将Docker配置为使用单独的网络接口来处理两种不同类型的流量。
> 初始化或加入swarm时，分别指定`--advertise-addr`和`--datapath-addr`。对于加入swarm的每个节点，必须这样做。

## Operations for standalone containers on overlay networks
> overlay网络上独立容器的操作

### Attach a standalone container to an overlay network
> 将独立容器附加到overlay网络

The `ingress` network is created without the `--attachable` flag, which means
that only swarm services can use it, and not standalone containers. You can
connect standalone containers to user-defined overlay networks which are created
with the `--attachable` flag. This gives standalone containers running on
different Docker daemons the ability to communicate without the need to set up
routing on the individual Docker daemon hosts.
> 不适用`--attachable`标志创建`ingress`网络，意味着只有swarm服务可以使用它，而不是独立的容器。
> 您可以将独立容器连接到使用`--attachable`标志创建的用户定义的覆盖网络。
> 这使运行在不同Docker守护进程上的独立容器能够进行通信，而无需在各个Docker守护程序主机上设置路由。

### Publish ports

| Flag value                      | Description                                                                                                                                     |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `-p 8080:80`                    | Map TCP port 80 in the container to port 8080 on the overlay network.                                                                               |
| `-p 8080:80/udp`                | Map UDP port 80 in the container to port 8080 on the overlay network.                                                                               |
| `-p 8080:80/sctp`               | Map SCTP port 80 in the container to port 8080 on the overlay network.                                                                              |
| `-p 8080:80/tcp -p 8080:80/udp` | Map TCP port 80 in the container to TCP port 8080 on the overlay network, and map UDP port 80 in the container to UDP port 8080 on the overlay network. |

### Container discovery

For most situations, you should connect to the service name, which is load-balanced and handled by all containers ("tasks") backing the service. To get a list of all tasks backing the service, do a DNS lookup for `tasks.<service-name>.`
> 对于大多数情况，你应该连接到服务名称，该名称是负载平衡的，由支持该服务的所有容器（"tasks"）处理。要获取支持服务的所有任务的列表，请执行DNS查找`tasks.<service-name>`。

## Next steps

- Go through the [overlay networking tutorial](network-tutorial-overlay.md)
- Learn about [networking from the container's point of view](../config/containers/container-networking.md)
- Learn about [standalone bridge networks](bridge.md)
- Learn about [Macvlan networks](macvlan.md)