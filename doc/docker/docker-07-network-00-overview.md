本文翻译自docker官网：[https://docs.docker.com/network/](https://docs.docker.com/network/)

#Networking overview

One of the reasons Docker containers and services are so powerful is that
you can connect them together, or connect them to non-Docker workloads. Docker
containers and services do not even need to be aware that they are deployed on
Docker, or whether their peers are also Docker workloads or not. Whether your
Docker hosts run Linux, Windows, or a mix of the two, you can use Docker to
manage them in a platform-agnostic way.
> Docker容器和服务如此强大的原因之一是您可以将它们连接在一起，或者将它们连接到非Docker工作负载。
> Docker容器和服务甚至不需要知道它们是部署在Docker上的，或者它们的对等方是否也是Docker工作负载。
> 无论Docker主机运行Linux、Windows还是两者的混合，都可以使用Docker以平台无关的方式管理它们。

This topic defines some basic Docker networking concepts and prepares you to
design and deploy your applications to take full advantage of these
capabilities.
> 本主题定义了一些基本的Docker网络概念，并为您设计和部署应用程序以充分利用这些功能做好准备。

## Scope of this topic
> 本专题的范围

This topic does **not** go into OS-specific details about how Docker networks
work, so you will not find information about how Docker manipulates `iptables`
rules on Linux or how it manipulates routing rules on Windows servers, and you
will not find detailed information about how Docker forms and encapsulates
packets or handles encryption. See [Docker and iptables](iptables.md).
> 本主题不涉及Docker网络在指定操作系统上如何工作的特定细节，
> 因此您将找不到有关Docker如何在Linux上操纵`iptables`规则或如何在Windows服务器上操纵路由规则的信息，
> 也找不到有关Docker如何形成和封装数据包或处理加密的详细信息。请参见[Docker and iptables](https://docs.docker.com/network/iptables/) 。

In addition, this topic does not provide any tutorials for how to create,
manage, and use Docker networks. Each section includes links to relevant
tutorials and command references.
> 此外，本主题不提供有关如何创建、管理和使用Docker网络的任何教程。
> 每个部分都包含指向相关教程和命令参考的链接。

## Network drivers

Docker's networking subsystem is pluggable, using drivers. Several drivers
exist by default, and provide core networking functionality:
> Docker的网络子系统是可插拔的，使用驱动程序。默认情况下存在多个驱动程序，并提供核心网络功能：

- `bridge`: The default network driver. If you don't specify a driver, this is
  the type of network you are creating. **Bridge networks are usually used when
  your applications run in standalone containers that need to communicate.** See
  [bridge networks](bridge.md).
  > 默认网络驱动程序。如果未指定驱动程序，则这是正在创建的网络类型。
  > **当应用程序在需要通信的独立容器中运行时，通常使用桥接网络。**
  > 参考 [bridge networks](https://docs.docker.com/network/bridge/) 。

- `host`: For standalone containers, remove network isolation between the
  container and the Docker host, and use the host's networking directly. See
  [use the host network](host.md).
  > 对于独立容器，移除容器和Docker主机之间的网络隔离，并直接使用主机的网络。
  > 参考：[use the host network](https://docs.docker.com/network/host/)

- `overlay`: Overlay networks connect multiple Docker daemons together and
  enable swarm services to communicate with each other. You can also use overlay
  networks to facilitate communication between a swarm service and a standalone
  container, or between two standalone containers on different Docker daemons.
  This strategy removes the need to do OS-level routing between these
  containers. See [overlay networks](overlay.md).
  > Overlay网络将多个Docker守护进程连接在一起，并使swarm服务能够相互通信。
  > 您还可以使用Overlay网络来促进swarm服务和独立容器之间的通信，或者在不同Docker守护进程上的两个独立容器之间的通信。
  > 此策略消除了在这些容器之间执行操作系统级路由的需要。
  > 参考 [overlay networks](https://docs.docker.com/network/overlay/)

- `macvlan`: Macvlan networks allow you to assign a MAC address to a container,
  making it appear as a physical device on your network. The Docker daemon
  routes traffic to containers by their MAC addresses. Using the `macvlan`
  driver is sometimes the best choice when dealing with legacy applications that
  expect to be directly connected to the physical network, rather than routed
  through the Docker host's network stack. See
  [Macvlan networks](macvlan.md).
  > Macvlan网络允许您将MAC地址分配给容器，使其显示为网络上的物理设备。
  > Docker守护进程通过容器的MAC地址将流量路由到容器。
  > 有时使用`macvlan`驱动程序是最佳选择，比如在处理遗留应用程序时，希望直接连接到物理网络而不是通过Docker主机的网络堆栈路由。
  > 参考 [Macvlan networks](https://docs.docker.com/network/macvlan/)

- `none`: For this container, disable all networking. Usually used in
  conjunction with a custom network driver. `none` is not available for swarm
  services. See
  [disable container networking](none.md).
  > 对于此容器，禁用所有网络。通常与自定义网络驱动程序一起使用。swarm服务不可用`none`。
  > 参考 [disable container networking](https://docs.docker.com/network/none/)

- [Network plugins](/engine/extend/plugins_services/): You can install and use
  third-party network plugins with Docker. These plugins are available from
  [Docker Hub](https://hub.docker.com/search?category=network&q=&type=plugin)
  or from third-party vendors. See the vendor's documentation for installing and
  using a given network plugin.
  > 您可以通过Docker安装和使用第三方网络插件。这些插件可从Docker Hub或第三方供应商处获得。
  > 有关安装和使用给定网络插件的信息，请参阅供应商文档。


### Network driver summary
> 网络驱动程序摘要

- **User-defined bridge networks** are best when you need multiple containers to
  communicate on the same Docker host.
  > 当您需要多个容器在同一Docker主机上通信时，**User-defined bridge networks**是最好的。
- **Host networks** are best when the network stack should not be isolated from
  the Docker host, but you want other aspects of the container to be isolated.
  > 当网络堆栈不应与Docker主机隔离，但您希望隔离容器的其他方面时，**Host networks**是最好的。
- **Overlay networks** are best when you need containers running on different
  Docker hosts to communicate, or when multiple applications work together using
  swarm services.
  > 当您需要运行在不同Docker主机上的容器进行通信时，或者当多个应用程序使用swarm服务协同工作时，**Overlay networks**是最好的。
- **Macvlan networks** are best when you are migrating from a VM setup or
  need your containers to look like physical hosts on your network, each with a
  unique MAC address.
  > 当你需要从VM设置迁移或需要容器看起来像网络上的物理主机，每个容器都有一个唯一的MAC地址时，**Macvlan networks**是最好的。
- **Third-party network plugins** allow you to integrate Docker with specialized
  network stacks.
  > **Third-party network plugins**允许您将Docker与专用网络堆栈集成。

## Networking tutorials

Now that you understand the basics about Docker networks, deepen your
understanding using the following tutorials:
> 现在您已经了解了Docker networks的基础知识，请使用以下教程加深您的理解：

- [Standalone networking tutorial](https://docs.docker.com/network/network-tutorial-standalone/)
- [Host networking tutorial](https://docs.docker.com/network/network-tutorial-host/)
- [Overlay networking tutorial](https://docs.docker.com/network/network-tutorial-overlay/)
- [Macvlan networking tutorial](https://docs.docker.com/network/network-tutorial-macvlan/)