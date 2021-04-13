本文翻译自docker官网：[https://docs.docker.com/get-started/overview/#docker-architecture](https://docs.docker.com/get-started/overview/#docker-architecture)

#Docker overview

Docker is an open platform for developing, shipping, and running applications.
Docker enables you to separate your applications from your infrastructure so
you can deliver software quickly. With Docker, you can manage your infrastructure
in the same ways you manage your applications. By taking advantage of Docker's
methodologies for shipping, testing, and deploying code quickly, you can
significantly reduce the delay between writing code and running it in production.
> Docker是一个用于开发、发布、运行程序的开放平台。
> Docker可以让你将应用程序和基础结构分开，以便快速交付软件。
> 使用Docker，你可以用管理应用程序相同的方法来管理基础设施。
> 通过利用Docker的方法快速发布、测试和代码，
> 你可以明显减少编写代码和在生产环境中运行代码之间的延迟。

## The Docker platform

Docker provides the ability to package and run an application in a loosely isolated
environment called a container. The isolation and security allow you to run many
containers simultaneously on a given host. Containers are lightweight and contain
everything needed to run the application, so you do not need to rely on what is
currently installed on the host. You can easily share containers while you work,
and be sure that everyone you share with gets the same container that works in the
same way.
> Docker提供了在称为容器（container）的松散隔离环境中打包盒运行程序的能力。
> 隔离和安全性允许你一个给定的主机上同时运行多个容器。
> 容器是轻量级的，包含运行应用程序所需要的所有内容，因此不需要主机上当前安装的内容。
> 你可以轻松地在工作时共享容器，并确保分享于每个人的都可以获得相同的容器，并以相同的方式工作。

Docker provides tooling and a platform to manage the lifecycle of your containers:
> Docker提供工具和平台来管理你的容器生命周期：

* Develop your application and its supporting components using containers.
  > 使用容器开发应用程序机器支持组建。
* The container becomes the unit for distributing and testing your application.
  > 容器成为分发和测试应用程序的单元。
* When you're ready, deploy your application into your production environment,
  as a container or an orchestrated service. This works the same whether your
  production environment is a local data center, a cloud provider, or a hybrid
  of the two.
  > 准备好后，将应用程序作为一个容器或者一个编排的服务部署到生产环境中。
  > 无论你的生产环境是本地数据中心、云提供商或者两者的混合体，都一样的工作。

## What can I use Docker for?

**Fast, consistent delivery of your applications**
> **快速、一致地交付你的应用程序**

Docker streamlines the development lifecycle by allowing developers to work in
standardized environments using local containers which provide your applications
and services. Containers are great for continuous integration and continuous
delivery (CI/CD) workflows.
> Docker通过允许开发人员在标准化的环境中使用本地容器（这些容器提供你的应用程序和服务）来简化开发生命周期。
> 容器非常适合于连续集成和连续交付（DI/CD）工作流。

Consider the following example scenario:
> 考虑以下示例场景：

- Your developers write code locally and share their work with their colleagues
  using Docker containers.
  > 你的开发人员在本地编写代码，并使用Docker容器与同事共享他们的工作。
  
- They use Docker to push their applications into a test environment and execute
  automated and manual tests.
  > 他们使用Docker将应用程序推入测试环境并执行自动和手动测试。
  
- When developers find bugs, they can fix them in the development environment
  and redeploy them to the test environment for testing and validation.
  > 当开发人员发现bug时，他们可以在开发环境中修复它们，并将它们重新部署到测试环境中进行测试和验证。

- When testing is complete, getting the fix to the customer is as simple as
  pushing the updated image to the production environment.
  > 测试完成后，向客户提供修复就如同将更新的镜像推送到生产环境一样简单。

**Responsive deployment and scaling**
>**快速部署和扩展**

Docker's container-based platform allows for highly portable workloads. Docker
containers can run on a developer's local laptop, on physical or virtual
machines in a data center, on cloud providers, or in a mixture of environments.
> Docker基于容器的平台允许高度可移植的工作负载。
> Docker容器可以在开发人员的本地笔记本电脑、数据中心的物理或虚拟机、云提供商或多种环境中运行。

Docker's portability and lightweight nature also make it easy to dynamically
manage workloads, scaling up or tearing down applications and services as
business needs dictate, in near real time.
> Docker的可移植性和轻量级特性还使得动态管理工作负载、根据业务需求以近乎实时的方式扩展或者删除应用程序和服务变得非常容易

**Running more workloads on the same hardware**
> **在同一个硬件上运行更多工作负载**

Docker is lightweight and fast. It provides a viable, cost-effective alternative
to hypervisor-based virtual machines, so you can use more of your compute
capacity to achieve your business goals. Docker is perfect for high density
environments and for small and medium deployments where you need to do more with
fewer resources.
> Docker是轻量级并且快速的。它是基于hypervisor的虚拟机提供了一个可行的、经济高效的替代方案，
> 因此你可以使用更多的计算能力来实现业务目标。
> Docker非常适合于高密度环境和中小型部署，在这些环境中，你需要用更少的资源做更多的工作。

## Docker architecture

Docker uses a client-server architecture. The Docker *client* talks to the
Docker *daemon*, which does the heavy lifting of building, running, and
distributing your Docker containers. The Docker client and daemon *can*
run on the same system, or you can connect a Docker client to a remote Docker
daemon. The Docker client and daemon communicate using a REST API, over UNIX
sockets or a network interface. Another Docker client is Docker Compose,
that lets you work with applications consisting of a set of containers.
> Docker使用客户机-服务器的架构。
> Docker客户端和Docker守护进程通信，后者负责构建、运行和分发Docker容器。
> Docker客户端和守护进程可以可以运行在同一个系统上，也可以将Docker客户端链接到远程的Docker守护进程。
> Docker客户端和守护进程使用RESTAPI通过UNIX套接字或网络接口进行通讯。
> 另一个Docker客户端是Docker Compose，它允许你处理由一组容器组成的应用程序。

![Docker Architecture Diagram](https://docs.docker.com/engine/images/architecture.svg)

### The Docker daemon

The Docker daemon (`dockerd`) listens for Docker API requests and manages Docker
objects such as images, containers, networks, and volumes. A daemon can also
communicate with other daemons to manage Docker services.
> Docker守护进程（`dockerd`）监听Docker API请求并管理对象，如镜像、容器、网络和卷。
> 守护进程还可以与其它守护进程通讯来管理Docker服务。

### The Docker client

The Docker client (`docker`) is the primary way that many Docker users interact
with Docker. When you use commands such as `docker run`, the client sends these
commands to `dockerd`, which carries them out. The `docker` command uses the
Docker API. The Docker client can communicate with more than one daemon.
> Docker客户端（`docker`）是许多用户与Docker交互的主要方式。
> 当你使用如`docker run`之类的指令时，客户端将这些指令发送到`dockerd`，由dockerd执行这些指令。
> `docker`指令使用Docker API。Docker客户端可以与多个守护进程通讯。

### Docker registries

A Docker _registry_ stores Docker images. Docker Hub is a public
registry that anyone can use, and Docker is configured to look for images on
Docker Hub by default. You can even run your own private registry.
> 用于存储Docker镜像的registry。
> Docker Hub是一个任何人都可以使用的公共registry，默认情况下，
> Docker配置为在Docker Hub上查找镜像。
> 你甚至可以运行自己私有的registry。

When you use the `docker pull` or `docker run` commands, the required images are
pulled from your configured registry. When you use the `docker push` command,
your image is pushed to your configured registry.
> 当你使用`docker pull`或`docker run`指令时，将从配置的registry中提取所需的镜像。
> 当你使用`docker push`指令时，镜像将被推送到配置的registry中。

### Docker objects

When you use Docker, you are creating and using images, containers, networks,
volumes, plugins, and other objects. This section is a brief overview of some
of those objects.
> 当你使用Docker，你要创建和使用镜像、容器、网络、卷和其它对象。
> 本节简要概述了其中一些对象。

#### Images

An _image_ is a read-only template with instructions for creating a Docker
container. Often, an image is _based on_ another image, with some additional
customization. For example, you may build an image which is based on the `ubuntu`
image, but installs the Apache web server and your application, as well as the
configuration details needed to make your application run.
> 镜像（image）是一个只读模板，其中包含创建Docker容器的说明。
> 通常情况下，一个镜像是基于另外一个景象，并进行了一些额外的定制。
> 例如，你可以构建一个给予`ubuntu`镜像的镜像，但是安装了Apache web服务器和应用程序，
> 以及运行应用程序所需要的配置细节。

You might create your own images or you might only use those created by others
and published in a registry. To build your own image, you create a _Dockerfile_
with a simple syntax for defining the steps needed to create the image and run
it. Each instruction in a Dockerfile creates a layer in the image. When you
change the Dockerfile and rebuild the image, only those layers which have
changed are rebuilt. This is part of what makes images so lightweight, small,
and fast, when compared to other virtualization technologies.
> 你可以创建你自己的镜像，也可以只使用别人创建好的并发布到registry的镜像。
> 要构建自己的镜像，可以使用简单的语法创建`Dockerfile`，用于定义创建和运行镜像所需的步骤。
> Dockerfile中每一个指令都会在镜像中创建一个layer(层).
> 当你改变Dockerfile并且重建镜像时，只会重新构建那些改变的layer。
> 与其他虚拟化技术相比，这是使镜像轻量级、小型和快速的原因之一。

#### Containers

A container is a runnable instance of an image. You can create, start, stop,
move, or delete a container using the Docker API or CLI. You can connect a
container to one or more networks, attach storage to it, or even create a new
image based on its current state.
> 容器（container）是镜像的可运行实例。你可以使用Docker API或CLI来创建、启动、停止、移动或删除容器。
> 你可以将容器链接到一个或多个网络，将存储链接到容器，甚至可以基于当前状态创建一个新的镜像。

By default, a container is relatively well isolated from other containers and
its host machine. You can control how isolated a container's network, storage,
or other underlying subsystems are from other containers or from the host
machine.
> 默认情况下，容器和其他容器以及主机是相对隔离的。
> 你可以控制容器的网络、存储或其它底层子系统与其它容器或主机的隔离程度。

A container is defined by its image as well as any configuration options you
provide to it when you create or start it. When a container is removed, any changes to
its state that are not stored in persistent storage disappear.
> 容器由它的镜像以及你在创建或者启动时提供给它的任何配置项定义。
> 当容器移除是，对其状态的任何未存储在持久化存储中的更改都将消失。

##### Example `docker run` command

The following command runs an `ubuntu` container, attaches interactively to your
local command-line session, and runs `/bin/bash`.
> 下面的指令运行一个`ubuntu`指令，以交互方式链接到本地命令行会话，并运行`/bin/bash`。

```bash
$ docker run -i -t ubuntu /bin/bash
```

When you run this command, the following happens (assuming you are using
the default registry configuration):
> 当你执行这个指令，发生以下情况（假设你使用的是默认registry配置）：

1.  If you do not have the `ubuntu` image locally, Docker pulls it from your
    configured registry, as though you had run `docker pull ubuntu` manually.
    > 如果你本地没有`ubuntu`镜像，Docker会从你配置的registry中获取它，
    > 就像你手动运行`docker pull ubuntu`一样

2.  Docker creates a new container, as though you had run a `docker container create`
    command manually.
    > Docker创建一个新的容器，就像你手动运行`docker container create`

3.  Docker allocates a read-write filesystem to the container, as its final
    layer. This allows a running container to create or modify files and
    directories in its local filesystem.
    > Docker分配一个`read-write`文件系统给容器，作为其最后一层。
    > 这允许正在运行的容器在其本地文件系统中创建或修改文件和目录。

4.  Docker creates a network interface to connect the container to the default
    network, since you did not specify any networking options. This includes
    assigning an IP address to the container. By default, containers can
    connect to external networks using the host machine's network connection.
    > Docker将创建一个网络接口，将容器链接到默认网络，因为你没有制定任何网络选项。
    > 这包含为容器分配IP地址。
    > 默认情况下，容器可以使用主机的网络链接到外部网络。

5.  Docker starts the container and executes `/bin/bash`. Because the container
    is running interactively and attached to your terminal (due to the `-i` and `-t`
    flags), you can provide input using your keyboard while the output is logged to
    your terminal.
    > Docker启动容器并执行`/bin/bash`。
    > 由于容器以交互方式运行并连接到你的终端（由于 -i和-t标志），
    > 你可以在将输出记录到终端时使用键盘提供输入。

6.  When you type `exit` to terminate the `/bin/bash` command, the container
    stops but is not removed. You can start it again or remove it.
    > 当你键入`exit`以终止`/bin/bash`指令时，容器将停止，但不会被删除。
    > 你可以重新启动或者删除它。

## The underlying technology（底层技术）
Docker is written in the [Go programming language](https://golang.org/) and takes
advantage of several features of the Linux kernel to deliver its functionality.
Docker uses a technology called `namespaces` to provide the isolated workspace
called the *container*. When you run a container, Docker creates a set of
*namespaces* for that container.
> Docker使用Go变成语言编写的，它利用Linux内核的一些特性来提供其功能。
> Docker使用名为`namespace`的技术来提供名为`container`的隔离工作区。
> 当你启动一个容器时，Docker会为该容器创建一组`namespace`。

These namespaces provide a layer of isolation. Each aspect of a container runs
in a separate namespace and its access is limited to that namespace.
> 这些namespace提供了一层隔离。
> 容器的每个方面都在一个单独的namespace中运行，其访问权限仅限于该namespace。
