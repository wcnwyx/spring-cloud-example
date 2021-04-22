本文翻译自docker官网：[https://docs.docker.com/network/macvlan/](https://docs.docker.com/network/macvlan/)

#Use macvlan networks

Some applications, especially legacy applications or applications which monitor
network traffic, expect to be directly connected to the physical network. In
this type of situation, you can use the `macvlan` network driver to assign a MAC
address to each container's virtual network interface, making it appear to be
a physical network interface directly connected to the physical network. In this
case, you need to designate a physical interface on your Docker host to use for
the `macvlan`, as well as the subnet and gateway of the `macvlan`. You can even
isolate your `macvlan` networks using different physical network interfaces.
Keep the following things in mind:
> 一些应用程序，特别是遗留应用程序或监视网络流量的应用程序，期望直接连接到物理网络。
> 在这种情况下，可以使用`macvlan`网络驱动程序为每个容器分配一个MAC地址的虚拟网络接口，
> 使其看起来像是一个直接连接到物理网络的物理网络接口。在这种情况下，您需要在Docker主机上为`macvlan`指定一个物理接口，
> 以及`macvlan`的子网和网关。您甚至可以使用不同的物理网络接口隔离`macvlan`网络。记住以下几点：

- It is very easy to unintentionally damage your network due to IP address
  exhaustion or to "VLAN spread", which is a situation in which you have an
  inappropriately large number of unique MAC addresses in your network.
  > 很容易由于IP地址耗尽或"VLAN spread"无意的破坏你的网络，这是指您的网络中具有不适当的大量唯一MAC地址的情况。

- Your networking equipment needs to be able to handle "promiscuous mode",
  where one physical interface can be assigned multiple MAC addresses.
  > 您的网络设备需要能够处理"promiscuous mode(混杂模式)"，即一个物理接口可以分配多个MAC地址。

- If your application can work using a bridge (on a single Docker host) or
  overlay (to communicate across multiple Docker hosts), these solutions may be
  better in the long term.
  > 如果您的应用程序可以使用bridge网桥（在单个Docker主机上）或overlay覆盖（在多个Docker主机上进行通信）工作，
  > 那么从长远来看，这些解决方案可能会更好。

## Create a macvlan network

When you create a `macvlan` network, it can either be in bridge mode or 802.1q
trunk bridge mode.
> 当你创建`macvlan`网络时，它可以处于网桥模式或802.1q中继网桥模式。

- In bridge mode, `macvlan` traffic goes through a physical device on the host.
  > 在网桥模式下，`macvlan`流量通过主机上的物理设备。

- In 802.1q trunk bridge mode, traffic goes through an 802.1q sub-interface
  which Docker creates on the fly. This allows you to control routing and
  filtering at a more granular level.
  > 在802.1q中继桥模式下，流量通过Docker动态创建的802.1q子接口。这允许您在更精细的级别上控制路由和筛选。

### Bridge mode

To create a `macvlan` network which bridges with a given physical network
interface, use `--driver macvlan` with the `docker network create` command. You
also need to specify the `parent`, which is the interface the traffic will
physically go through on the Docker host.
> 要创建一个连接给定物理网络接口的`macvlan`网络，请使用`--driver macvlan`和`docker network create`命令。
> 您还需要指定父级，即流量将在Docker主机上实际通过的接口。

```bash
$ docker network create -d macvlan \
  --subnet=172.16.86.0/24 \
  --gateway=172.16.86.1 \
  -o parent=eth0 pub_net
```

If you need to exclude IP addresses from being used in the `macvlan` network, such
as when a given IP address is already in use, use `--aux-addresses`:
> 如果需要排除在`macvlan`网络中使用的IP地址，例如当给定的IP地址已在使用时，请使用`--aux addresses`:

```bash
$ docker network create -d macvlan \
  --subnet=192.168.32.0/24 \
  --ip-range=192.168.32.128/25 \
  --gateway=192.168.32.254 \
  --aux-address="my-router=192.168.32.129" \
  -o parent=eth0 macnet32
```

### 802.1q trunk bridge mode

If you specify a `parent` interface name with a dot included, such as `eth0.50`,
Docker interprets that as a sub-interface of `eth0` and creates the sub-interface
automatically.
> 如果指定包含点的`parent`父接口名称（如`eth0.50`），Docker会将其解释为eth0的子接口，并自动创建子接口。

```bash
$ docker network create -d macvlan \
    --subnet=192.168.50.0/24 \
    --gateway=192.168.50.1 \
    -o parent=eth0.50 macvlan50
```

### Use an ipvlan instead of macvlan
> 使用ipvlan代替macvlan

In the above example, you are still using a L3 bridge. You can use `ipvlan`
instead, and get an L2 bridge. Specify `-o ipvlan_mode=l2`.
> 在上面的示例中，您仍然使用L3网桥。您可以改用`ipvlan`，并获得一个L2网桥。指定`-o ipvlan_mode=l2`。

```bash
$ docker network create -d ipvlan \
    --subnet=192.168.210.0/24 \
    --subnet=192.168.212.0/24 \
    --gateway=192.168.210.254 \
    --gateway=192.168.212.254 \
     -o ipvlan_mode=l2 -o parent=eth0 ipvlan210
```

## Use IPv6

If you have [configured the Docker daemon to allow IPv6](../config/daemon/ipv6.md),
you can use dual-stack IPv4/IPv6 `macvlan` networks.
> 如果已将Docker守护程序配置为允许IPv6，则可以使用双堆栈IPv4/IPv6 `macvlan`网络。

```bash
$ docker network create -d macvlan \
    --subnet=192.168.216.0/24 --subnet=192.168.218.0/24 \
    --gateway=192.168.216.1 --gateway=192.168.218.1 \
    --subnet=2001:db8:abc8::/64 --gateway=2001:db8:abc8::10 \
     -o parent=eth0.218 \
     -o macvlan_mode=bridge macvlan216
```

## Next steps

- Go through the [macvlan networking tutorial](network-tutorial-macvlan.md)
- Learn about [networking from the container's point of view](../config/containers/container-networking.md)
- Learn about [bridge networks](bridge.md)
- Learn about [overlay networks](overlay.md)
- Learn about [host networking](host.md)
- Learn about [Macvlan networks](macvlan.md)