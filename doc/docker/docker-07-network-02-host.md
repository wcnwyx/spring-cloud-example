本文翻译自docker官网：[https://docs.docker.com/network/host/](https://docs.docker.com/network/host/)

#Use host networking

If you use the `host` network mode for a container, that container's network
stack is not isolated from the Docker host (the container shares the host's
networking namespace), and the container does not get its own IP-address allocated.
For instance, if you run a container which binds to port 80 and you use `host`
networking, the container's application is available on port 80 on the host's IP
address.
> 如果对容器使用`host`网络模式，则该容器的网络堆栈不会与Docker主机隔离（该容器共享主机的网络命名空间），
> 并且该容器不会获得自己分配的IP地址。例如，如果您运行一个绑定到端口80的容器并使用`host`网络，
> 则该容器的应用程序在主机IP地址的端口80上可用。

> **Note**: Given that the container does not have its own IP-address when using
> `host` mode networking, [port-mapping](overlay.md#publish-ports) does not
> take effect, and the `-p`, `--publish`, `-P`, and `--publish-all` option are
> ignored, producing a warning instead:  
> 假设容器在使用`host`模式网络时没有自己的IP地址，则端口映射不会生效，
> 并且会忽略`-p`、`-publish`、`-P`和`--publish all`选项，从而生成警告：
>
> ```
> WARNING: Published ports are discarded when using host network mode
> ```

Host mode networking can be useful to optimize performance, and in situations where
a container needs to handle a large range of ports, as it does not require network
address translation (NAT), and no "userland-proxy" is created for each port.
> Host模式网络对于优化性能非常有用，在容器需要处理大量端口的情况下，
> 因为它不需要网络地址转换（NAT），并且不为每个端口创建"userland-proxy"。

The host networking driver only works on Linux hosts, and is not supported on
Docker Desktop for Mac, Docker Desktop for Windows, or Docker EE for Windows Server.
> host网络驱动程序仅适用于Linux主机，
> 在Docker Desktop for Mac、Docker Desktop for Windows或Docker EE for Windows Server上不受支持。

You can also use a `host` network for a swarm service, by passing `--network host`
to the `docker service create` command. In this case, control traffic (traffic
related to managing the swarm and the service) is still sent across an overlay
network, but the individual swarm service containers send data using the Docker
daemon's host network and ports. This creates some extra limitations. For instance,
if a service container binds to port 80, only one service container can run on a
given swarm node.
> 您还可以将`host`网络用于swarm服务，方法是将`--network host`传递给`docker service create`命令。
> 在这种情况下，控制流量（与管理swarm和服务相关的流量）仍然通过overlay网络发送，
> 但是各个swarm服务容器使用Docker守护进程的host网络和端口发送数据。
> 这造成了一些额外的限制。例如，如果服务容器绑定到端口80，那么在给定的swarm节点上只能运行一个服务容器。

## Next steps

- Go through the [host networking tutorial](network-tutorial-host.md)
- Learn about [networking from the container's point of view](../config/containers/container-networking.md)
- Learn about [bridge networks](bridge.md)
- Learn about [overlay networks](overlay.md)
- Learn about [Macvlan networks](macvlan.md)