本文翻译自docker官网：[https://docs.docker.com/network/bridge/](https://docs.docker.com/network/bridge/)

#Use bridge networks

In terms of networking, a bridge network is a Link Layer device
which forwards traffic between network segments. A bridge can be a hardware
device or a software device running within a host machine's kernel.
> 在网络方面，网桥网络是在网段之间转发业务的链路层设备。网桥可以是运行在主机内核中的硬件设备或软件设备。

In terms of Docker, a bridge network uses a software bridge which allows
containers connected to the same bridge network to communicate, while providing
isolation from containers which are not connected to that bridge network. The
Docker bridge driver automatically installs rules in the host machine so that
containers on different bridge networks cannot communicate directly with each
other.
> 就Docker而言，网桥网络使用软件网桥，该网桥允许连接到同一网桥网络的容器进行通信，同时提供与未连接到该网桥网络的容器的隔离。
> Docker网桥驱动程序会自动在主机中安装规则，以便不同网桥网络上的容器无法直接相互通信。

Bridge networks apply to containers running on the **same** Docker daemon host.
For communication among containers running on different Docker daemon hosts, you
can either manage routing at the OS level, or you can use an
[overlay network](overlay.md).
> 网桥网络适用于在**相同**Docker守护程序主机上运行的容器。
> 对于运行在不同Docker守护程序主机上的容器之间的通信，可以在操作系统级别管理路由，也可以使用[overlay network](https://docs.docker.com/network/overlay/) 。

When you start Docker, a [default bridge network](#use-the-default-bridge-network) (also
called `bridge`) is created automatically, and newly-started containers connect
to it unless otherwise specified. You can also create user-defined custom bridge
networks. **User-defined bridge networks are superior to the default `bridge`
network.**
> 启动Docker时，会自动创建一个默认网桥网络（也称为`bridge`），新启动的容器会连接到该网络，除非另有指定。
> 也可以创建用户定义的自定义网桥网络。**用户定义的网桥网络优于默认的网桥网络。**

## Differences between user-defined bridges and the default bridge
> 用户定义桥与默认桥之间的差异

- **User-defined bridges provide automatic DNS resolution between containers**.
  > **用户定义的网桥提供容器之间的自动DNS解析。**

  Containers on the default bridge network can only access each other by IP
  addresses, unless you use the [`--link` option](links.md), which is
  considered legacy. On a user-defined bridge network, containers can resolve
  each other by name or alias.
  > 默认网桥网络上的容器只能通过IP地址相互访问，除非使用`--link`选项，这被认为是遗留的。
  > 在用户定义的网桥网络上，容器可以通过名称或别名相互解析。

  Imagine an application with a web front-end and a database back-end. If you call
  your containers `web` and `db`, the web container can connect to the db container
  at `db`, no matter which Docker host the application stack is running on.
  > 假设一个应用程序有一个web前端和一个数据库后端。
  > 如果您将容器称为为`web`和`db`，那么无论应用程序堆栈运行在哪个Docker主机上，web容器都可以连接到`db`处的db容器。

  If you run the same application stack on the default bridge network, you need
  to manually create links between the containers (using the legacy `--link`
  flag). These links need to be created in both directions, so you can see this
  gets complex with more than two containers which need to communicate.
  Alternatively, you can manipulate the `/etc/hosts` files within the containers,
  but this creates problems that are difficult to debug.
  > 如果在默认网桥网络上运行相同的应用程序堆栈，则需要在容器之间手动创建链接（使用遗留的`--link`标志）。
  > 这些链接需要在两个方向上创建，因此您可以看到，对于需要通信的两个以上容器来说，这变得很复杂。
  > 或者，可以在容器中操作`/etc/hosts`文件，但这会产生难以调试的问题。

- **User-defined bridges provide better isolation**.
  > **User-defined bridges 提供更好的隔离**

  All containers without a `--network` specified, are attached to the default bridge network. This can be a risk, as unrelated stacks/services/containers are then able to communicate.
  > 所有未指定`--network`的容器都连接到默认网桥网络。这可能是一个风险，因为不相关的堆栈/服务/容器能够进行通信。

  Using a user-defined network provides a scoped network in which only containers attached to that network are able to communicate.
  > 使用user-defined network提供了一个作用域网络，其中只有连接到该网络的容器才能进行通信。

- **Containers can be attached and detached from user-defined networks on the fly**.
  > **容器可以动态地从用户定义的网络连接和分离。**

  During a container's lifetime, you can connect or disconnect it from
  user-defined networks on the fly. To remove a container from the default
  bridge network, you need to stop the container and recreate it with different
  network options.
  > 在容器的生命周期内，您可以动态地将其与用户定义的网络连接或断开连接。
  > 要从默认网桥网络中删除容器，需要停止容器并使用不同的网络选项重新创建它。

- **Each user-defined network creates a configurable bridge**.
  > **每个用户定义的网络创建一个可配置的网桥。**

  If your containers use the default bridge network, you can configure it, but
  all the containers use the same settings, such as MTU and `iptables` rules.
  In addition, configuring the default bridge network happens outside of Docker
  itself, and requires a restart of Docker.
  > 如果您的容器使用默认网桥网络，您可以对其进行配置，但所有容器都使用相同的设置，例如MTU和`iptables`规则。
  > 此外，配置默认网桥网络发生在Docker本身之外，需要重新启动Docker。

  User-defined bridge networks are created and configured using
  `docker network create`. If different groups of applications have different
  network requirements, you can configure each user-defined bridge separately,
  as you create it.
  > 使用`docker network create`创建和配置用户定义的网桥网络。
  > 如果不同的应用程序组具有不同的网络需求，则可以在创建每个用户定义的网桥时分别对其进行配置。

- **Linked containers on the default bridge network share environment variables**.
  > **默认网桥网络上的链接容器共享环境变量。**

  Originally, the only way to share environment variables between two containers
  was to link them using the [`--link` flag](links.md). This type of
  variable sharing is not possible with user-defined networks. However, there
  are superior ways to share environment variables. A few ideas:
  > 最初，在两个容器之间共享环境变量的唯一方法是使用`--link`标志链接它们。
  > 这种类型的变量共享在用户定义的网络中是不可能的。但是，有更好的方法来共享环境变量。一些想法：

    - Multiple containers can mount a file or directory containing the shared
      information, using a Docker volume.
      > 多个容器可以使用Docker卷装载包含共享信息的文件或目录。

    - Multiple containers can be started together using `docker-compose` and the
      compose file can define the shared variables.
      > 使用`docker-compose`可以同时启动多个容器，compose文件可以定义共享变量。

    - You can use swarm services instead of standalone containers, and take
      advantage of shared [secrets](../engine/swarm/secrets.md) and
      [configs](../engine/swarm/configs.md).
      > 您可以使用swarm服务而不是独立容器，并利用共享机密和配置。

Containers connected to the same user-defined bridge network effectively expose all ports
to each other. For a port to be accessible to containers or non-Docker hosts on
different networks, that port must be _published_ using the `-p` or `--publish`
flag.
> 连接到同一用户定义网桥网络的容器有效地将所有端口相互公开。
> 对于不同网络上的容器或非Docker主机可以访问的端口，必须使用`-p`或`--publish`标志 _发布_ 该端口。

## Manage a user-defined bridge

Use the `docker network create` command to create a user-defined bridge
network.
> 使用`docker network create`命令创建用户定义的网桥网络。

```bash
$ docker network create my-net
```

You can specify the subnet, the IP address range, the gateway, and other
options. See the
[docker network create](../engine/reference/commandline/network_create.md#specify-advanced-options)
reference or the output of `docker network create --help` for details.
> 您可以指定子网、IP地址范围、网关和其他选项。
> 有关详细信息，请参阅[docker network create](https://docs.docker.com/engine/reference/commandline/network_create/#specify-advanced-options) 引用
> 或`docker network create --help`的输出详情。

Use the `docker network rm` command to remove a user-defined bridge
network. If containers are currently connected to the network,
[disconnect them](#disconnect-a-container-from-a-user-defined-bridge)
first.
> 使用`docker network rm`命令删除用户定义的网桥网络。
> 如果容器当前已连接到网络，请先断开[disconnect them](https://docs.docker.com/network/bridge/#disconnect-a-container-from-a-user-defined-bridge) 它们的连接。

```bash
$ docker network rm my-net
```

> **What's really happening?**
>
> When you create or remove a user-defined bridge or connect or disconnect a
> container from a user-defined bridge, Docker uses tools specific to the
> operating system to manage the underlying network infrastructure (such as adding
> or removing bridge devices or configuring `iptables` rules on Linux). These
> details should be considered implementation details. Let Docker manage your
> user-defined networks for you.   
> 当您创建或删除用户定义的网桥或连接或断开容器与用户定义网桥的连接时，
> Docker使用特定于操作系统的工具来管理底层网络基础设施（例如添加或删除网桥设备或在Linux上配置`iptables`规则）。
> 这些细节应视为实施细节。让Docker为您管理用户定义的网络。

## Connect a container to a user-defined bridge
> 将容器连接到用户定义的网桥

When you create a new container, you can specify one or more `--network` flags.
This example connects a Nginx container to the `my-net` network. It also
publishes port 80 in the container to port 8080 on the Docker host, so external
clients can access that port. Any other container connected to the `my-net`
network has access to all ports on the `my-nginx` container, and vice versa.
> 当你创建新容器时，可以指定一个或多个`--network`标志。本例将Nginx容器连接到`my-net`网络。
> 它还将容器中的端口80发布到Docker主机上的端口8080，以便外部客户端可以访问该端口。
> 连接到`my-net`网络的任何其他容器都可以访问`my-nginx`容器上的所有端口，反之亦然。

```bash
$ docker create --name my-nginx \
  --network my-net \
  --publish 8080:80 \
  nginx:latest
```

To connect a **running** container to an existing user-defined bridge, use the
`docker network connect` command. The following command connects an already-running
`my-nginx` container to an already-existing `my-net` network:
> 要将**正在运行的**容器连接到现有的用户定义网桥，请使用`docker network connect`命令。
> 以下命令将已运行的`my-nginx`容器连接到已存在的`my-net`网络：

```bash
$ docker network connect my-net my-nginx
```

## Disconnect a container from a user-defined bridge
> 断开容器与用户定义网桥的连接

To disconnect a running container from a user-defined bridge, use the `docker
network disconnect` command. The following command disconnects the `my-nginx`
container from the `my-net` network.
> 要从用户定义的网桥断开正在运行的容器的连接，请使用`docker network disconnect`命令。
> 下面的命令断开my-nginx`容器与`my-net`网络的连接。

```bash
$ docker network disconnect my-net my-nginx
```

## Use IPv6

If you need IPv6 support for Docker containers, you need to
[enable the option](../config/daemon/ipv6.md) on the Docker daemon and reload its
configuration, before creating any IPv6 networks or assigning containers IPv6
addresses.
> 如果需要对Docker容器提供IPv6支持，则需要在创建任何IPv6网络或分配容器IPv6地址之前，
> 在Docker守护程序上启用该选项并重新加载其配置。

When you create your network, you can specify the `--ipv6` flag to enable
IPv6. You can't selectively disable IPv6 support on the default `bridge` network.
> 创建网络时，可以指定`--ipv6`标志以启用ipv6。不能有选择地禁用默认`bridge`网络上的IPv6支持。

## Enable forwarding from Docker containers to the outside world
> 启用从Docker容器到外部世界的转发

By default, traffic from containers connected to the default bridge network is
**not** forwarded to the outside world. To enable forwarding, you need to change
two settings. These are not Docker commands and they affect the Docker host's
kernel.
> 默认情况下，来自连接到默认网桥网络的容器的流量**不会**转发到外部世界。
> 要启用转发，您需要更改两个设置。这些不是Docker命令，它们影响Docker主机的内核。

1.  Configure the Linux kernel to allow IP forwarding.
    > 配置Linux内核以允许IP转发。

    ```bash
    $ sysctl net.ipv4.conf.all.forwarding=1
    ```

2.  Change the policy for the `iptables` `FORWARD` policy from `DROP` to
    `ACCEPT`.
    > 将`iptables` `FORWARD`策略从`DROP`更改为`ACCEPT`。

    ```bash
    $ sudo iptables -P FORWARD ACCEPT
    ```

These settings do not persist across a reboot, so you may need to add them to a
start-up script.
> 这些设置不会在重新启动期间持续存在，因此您可能需要将它们添加到启动脚本中。

## Use the default bridge network

The default `bridge` network is considered a legacy detail of Docker and is not
recommended for production use. Configuring it is a manual operation, and it has
[technical shortcomings](#differences-between-user-defined-bridges-and-the-default-bridge).
> 默认`bridge`网络被认为是Docker的遗留细节，不建议在生产中使用。配置它是一种手动操作，并且有技术缺陷。

### Connect a container to the default bridge network

If you do not specify a network using the `--network` flag, and you do specify a
network driver, your container is connected to the default `bridge` network by
default. Containers connected to the default `bridge` network can communicate,
but only by IP address, unless they are linked using the
[legacy `--link` flag](links.md).
> 如果没有使用`--network`标志指定网络，并且确实指定了网络驱动程序，则默认情况下，容器将连接到默认`bridge`网络。
> 连接到默认`bridge`网络的容器可以进行通信，但只能通过IP地址进行通信，除非它们使用遗留的 `--link`标志进行链接。

### Configure the default bridge network

To configure the default `bridge` network, you specify options in `daemon.json`.
Here is an example `daemon.json` with several options specified. Only specify
the settings you need to customize.
> 要配置默认`bridge`网络，请在`daemon.json`中指定选项。
> 下面是一个指定了几个选项`daemon.json`例子。只指定需要自定义的设置。

```json
{
  "bip": "192.168.1.5/24",
  "fixed-cidr": "192.168.1.5/25",
  "fixed-cidr-v6": "2001:db8::/64",
  "mtu": 1500,
  "default-gateway": "10.20.1.1",
  "default-gateway-v6": "2001:db8:abcd::89",
  "dns": ["10.20.1.2","10.20.1.3"]
}
```

Restart Docker for the changes to take effect.
> 重新启动Docker以使更改生效。

### Use IPv6 with the default bridge network

If you configure Docker for IPv6 support (see [Use IPv6](#use-ipv6)), the
default bridge network is also configured for IPv6 automatically. Unlike
user-defined bridges, you can't selectively disable IPv6 on the default bridge.
> 如果将Docker配置为支持IPv6（请参阅[Use IPv6](#use-ipv6) ），则默认网桥网络也会自动配置为支持IPv6。
> 与用户定义的网桥不同，不能在默认网桥上有选择地禁用IPv6。

## Next steps

- Go through the [standalone networking tutorial](network-tutorial-standalone.md)
- Learn about [networking from the container's point of view](../config/containers/container-networking.md)
- Learn about [overlay networks](overlay.md)
- Learn about [Macvlan networks](macvlan.md)