本文翻译自docker官网：[https://docs.docker.com/storage/](https://docs.docker.com/storage/)

#Manage data in Docker(Storage overview)

By default all files created inside a container are stored on a writable
container layer. This means that:
> 默认情况下，在容器内创建的所有文件都存储在可写容器层上。这意味着：

- The data doesn't persist when that container no longer exists, and it can be
  difficult to get the data out of the container if another process needs it.
  > 当容器不再存在时，数据不会持久存在，如果另一个进程需要数据，则很难从容器中取出数据。
- A container's writable layer is tightly coupled to the host machine
  where the container is running. You can't easily move the data somewhere else.
  > 容器的可写层与运行容器的主机紧密耦合。你不能轻易地把数据移到别的地方。
- Writing into a container's writable layer requires a
  [storage driver](/storage/storagedriver/) to manage the
  filesystem. The storage driver provides a union filesystem, using the Linux
  kernel. This extra abstraction reduces performance as compared to using
  _data volumes_, which write directly to the host filesystem.
  > 写入容器的可写层需要[storage driver](https://docs.docker.com/storage/storagedriver/) 来管理文件系统。
  > 存储驱动程序使用Linux内核提供了一个union文件系统。
  > 与直接写入主机文件系统的 _数据卷_ 相比，这种额外的抽象降低了性能。

Docker has two options for containers to store files in the host machine, so
that the files are persisted even after the container stops: _volumes_, and
_bind mounts_. If you're running Docker on Linux you can also use a _tmpfs mount_.
If you're running Docker on Windows you can also use a _named pipe_.
> Docker为容器提供了两个在主机中存储文件的选项，这样即使在容器停止后文件也会被持久化：_volumes_ 卷和 _bind mounts_ 绑定装载。
> 如果你在Linux上运行Docker，你也可以使用 _tmpfs mount_ tmpfs挂载。如果在Windows上运行Docker，还可以使用 _named pipe_ 命名管道。

Keep reading for more information about these two ways of persisting data.
> 请继续阅读，了解有关这两种数据持久化方法的更多信息。

## Choose the right type of mount
> 选择正确的挂载类型

No matter which type of mount you choose to use, the data looks the same from
within the container. It is exposed as either a directory or an individual file
in the container's filesystem.
> 无论您选择使用哪种装载类型，容器中的数据看起来都是相同的。它以目录或容器文件系统中的单个文件的形式公开。

An easy way to visualize the difference among volumes, bind mounts, and `tmpfs`
mounts is to think about where the data lives on the Docker host.
> 卷、绑定挂载和`tmpfs`挂载之间的差异用一个简单方法设想就是考虑数据在Docker主机上的位置。

![装载类型及其在Docker主机上的位置](https://docs.docker.com/storage/images/types-of-mounts.png)

- **Volumes** are stored in a part of the host filesystem which is _managed by
  Docker_ (`/var/lib/docker/volumes/` on Linux). Non-Docker processes should not
  modify this part of the filesystem. Volumes are the best way to persist data
  in Docker.
  > **Volumes**存储在由Docker管理的主机文件系统的一部分中（在Linux上是`/var/lib/docker/volumes/`）。
  > 非Docker进程不应该修改文件系统的这一部分。卷是在Docker中保存数据的最佳方式。

- **Bind mounts** may be stored *anywhere* on the host system. They may even be
  important system files or directories. Non-Docker processes on the Docker host
  or a Docker container can modify them at any time.
  > **Bind mounts**可以存储在主机系统的*任何位置*。它们甚至可能是重要的系统文件或目录。
  > Docker主机或Docker容器上的非Docker进程可以随时修改它们。

- **`tmpfs` mounts** are stored in the host system's memory only, and are never
  written to the host system's filesystem.
  > **`tmpfs` 挂载**只存储在主机系统的内存中，从不写入主机系统的文件系统。

### More details about mount types
> 有关装载类型的详细信息

- **[Volumes](volumes.md)**: Created and managed by Docker. You can create a
  volume explicitly using the `docker volume create` command, or Docker can
  create a volume during container or service creation.
  > 由Docker创建和管理。
  > 您可以使用`docker volume create`命令显式创建卷，或者docker可以在容器或服务创建期间创建卷。

  When you create a volume, it is stored within a directory on the Docker
  host. When you mount the volume into a container, this directory is what is
  mounted into the container. This is similar to the way that bind mounts work,
  except that volumes are managed by Docker and are isolated from the core
  functionality of the host machine.
  > 创建卷时，它存储在Docker主机上的目录中。将卷装入容器时，此目录就是装入容器的目录。
  > 这与绑定装载的工作方式类似，只是卷由Docker管理，并且与主机的核心功能隔离。

  A given volume can be mounted into multiple containers simultaneously. When no
  running container is using a volume, the volume is still available to Docker
  and is not removed automatically. You can remove unused volumes using `docker
  volume prune`.
  > 一个给定的卷可以同时装入多个容器。当没有正在运行的容器正在使用卷时，Docker仍然可以使用该卷，
  > 并且不会自动删除该卷。您可以使用`docker volume prune`删除未使用的卷。

  When you mount a volume, it may be **named** or **anonymous**. Anonymous
  volumes are not given an explicit name when they are first mounted into a
  container, so Docker gives them a random name that is guaranteed to be unique
  within a given Docker host. Besides the name, named and anonymous volumes
  behave in the same ways.
  > 当你装载卷时，它可以是**命名的**或**匿名的**。匿名卷在第一次装入容器时没有给出显式名称，
  > 因此Docker会为它们提供一个随机名称，该名称在给定Docker主机中保证是唯一的。
  > 除了名称之外，命名卷和匿名卷的行为方式相同。

  Volumes also support the use of _volume drivers_, which allow you to store
  your data on remote hosts or cloud providers, among other possibilities.
  > 卷还支持使用 _卷驱动程序_ ，允许您将数据存储在远程主机或云提供商上。

- **[Bind mounts](bind-mounts.md)**: Available since the early days of Docker.
  Bind mounts have limited functionality compared to volumes. When you use a
  bind mount, a file or directory on the _host machine_ is mounted into a
  container. The file or directory is referenced by its full path on the host
  machine. The file or directory does not need to exist on the Docker host
  already. It is created on demand if it does not yet exist. Bind mounts are
  very performant, but they rely on the host machine's filesystem having a
  specific directory structure available. If you are developing new Docker
  applications, consider using named volumes instead. You can't use
  Docker CLI commands to directly manage bind mounts.
  > 从Docker早期就有了。与卷相比，绑定装载的功能有限。当你使用绑定装载时，_主机上的_ 一个文件或目录将装载到容器中。
  > 文件或目录由其在主机上的完整路径引用。Docker主机上不需要已经存在该文件或目录。如果它还不存在，则按需创建。
  > 绑定挂载的性能非常好，但它们依赖于具有特定目录结构的主机文件系统。
  > 如果您正在开发新的Docker应用程序，请考虑改用命名卷。你不能使用Docker CLI命令直接管理绑定装载。

  > Bind mounts allow access to sensitive files  
  > 绑定装载允许访问敏感文件
  >
  > One side effect of using bind mounts, for better or for worse,
  > is that you can change the **host** filesystem via processes running in a
  > **container**, including creating, modifying, or deleting important system
  > files or directories. This is a powerful ability which can have security
  > implications, including impacting non-Docker processes on the host system.  
  > 不管好坏，使用绑定挂载的一个副作用是，您可以通过在容器中运行的进程来更改主机文件系统，包括创建、修改或删除重要的系统文件或目录。
  > 这是一种强大的功能，可能会带来安全隐患，包括影响主机系统上的非Docker进程。

- **[tmpfs mounts](tmpfs.md)**: A `tmpfs` mount is not persisted on disk, either
  on the Docker host or within a container. It can be used by a container during
  the lifetime of the container, to store non-persistent state or sensitive
  information. For instance, internally, swarm services use `tmpfs` mounts to
  mount [secrets](../engine/swarm/secrets.md) into a service's containers.
  > `tmpfs`挂载不会持久化在磁盘上，无论是在Docker主机上还是在容器中。
  > 它可以在容器的生存期内由容器使用，以存储非持久状态或敏感信息。
  > 例如，在内部，swarm服务使用`tmpfs`挂载将机密`secrets`挂载到服务的容器中。

- **[named pipes](https://docs.microsoft.com/en-us/windows/desktop/ipc/named-pipes)**: An `npipe`
  mount can be used for communication between the Docker host and a container. Common use case is
  to run a third-party tool inside of a container and connect to the Docker Engine API using a named pipe.
  > `npipe`挂载可以用于Docker主机和容器之间的通信。常见的用例是在容器内运行第三方工具，并使用named pipe(命名管道)连接到Docker引擎API。

Bind mounts and volumes can both be mounted into containers using the `-v` or
`--volume` flag, but the syntax for each is slightly different. For `tmpfs`
mounts, you can use the `--tmpfs` flag. We recommend using the `--mount` flag
for both containers and services, for bind mounts, volumes, or `tmpfs` mounts,
as the syntax is more clear.
> 绑定装载和卷都可以使用`-v`或`--volume`标志装载到容器中，但它们的语法略有不同。
> 对于`tmpfs`挂载，可以使用`--tmpfs`标志。
> 对于绑定装载、卷或`tmpfs`装载，我们建议对容器和服务使用`--mount`标志，因为语法更清楚。

## Good use cases for volumes
> 卷的良好用例

Volumes are the preferred way to persist data in Docker containers and services.
Some use cases for volumes include:
> 卷是在Docker容器和服务中持久化数据的首选方法。卷的一些用例包括：

- Sharing data among multiple running containers. If you don't explicitly create
  it, a volume is created the first time it is mounted into a container. When
  that container stops or is removed, the volume still exists. Multiple
  containers can mount the same volume simultaneously, either read-write or
  read-only. Volumes are only removed when you explicitly remove them.
  > 在多个正在运行的容器之间共享数据。如果您没有显式地创建它，那么第一次将卷装入容器时创建它。
  > 当容器停止或被移除时，卷仍然存在。多个容器可以同时装载同一个卷，可以是读写的，也可以是只读的。
  > 只有在显式删除卷时才会删除它们。

- When the Docker host is not guaranteed to have a given directory or file
  structure. Volumes help you decouple the configuration of the Docker host
  from the container runtime.
  > 当Docker主机不能保证具有给定的目录或文件结构时。卷可以帮助您将Docker主机的配置与容器运行时解耦。

- When you want to store your container's data on a remote host or a cloud
  provider, rather than locally.
  > 当你想你的容器数据存储在远程主机或云提供商上，而不是本地。

- When you need to back up, restore, or migrate data from one Docker
  host to another, volumes are a better choice. You can stop containers using
  the volume, then back up the volume's directory
  (such as `/var/lib/docker/volumes/<volume-name>`).
  > 当您需要备份、恢复或将数据从一个Docker主机迁移到另一个Docker主机时，卷是更好的选择。
  > 您可以停止使用该卷的容器，然后备份该卷的目录（例如`/var/lib/docker/volumes/<volume-name>`）。

- When your application requires high-performance I/O on Docker Desktop. Volumes
  are stored in the Linux VM rather than the host, which means that the reads and writes
  have much lower latency and higher throughput.
  > 当应用程序需要Docker桌面上的高性能I/O时。卷存储在Linux VM而不是主机中，这意味着读写具有更低的延迟和更高的吞吐量。

- When your application requires fully native file system behavior on Docker
  Desktop. For example, a database engine requires precise control over disk
  flushing to guarantee transaction durability. Volumes are stored in the Linux
  VM and can make these guarantees, whereas bind mounts are remoted to macOS or
  Windows, where the file systems behave slightly differently.
  > 当应用程序需要Docker桌面上的全部的本机文件系统行为时。
  > 例如，数据库引擎需要对磁盘刷新进行精确控制，以保证事务的持久性。
  > 卷存储在Linux VM中，可以提供这些保证，而绑定挂载是远程到macOS或Windows的，在macOS或Windows中，文件系统的行为略有不同。

## Good use cases for bind mounts
> 绑定挂载的良好用例

In general, you should use volumes where possible. Bind mounts are appropriate
for the following types of use case:
> 一般来说，您应该尽可能使用卷。绑定装载适用于以下类型的用例：

- Sharing configuration files from the host machine to containers. This is how
  Docker provides DNS resolution to containers by default, by mounting
  `/etc/resolv.conf` from the host machine into each container.

- Sharing source code or build artifacts between a development environment on
  the Docker host and a container. For instance, you may mount a Maven `target/`
  directory into a container, and each time you build the Maven project on the
  Docker host, the container gets access to the rebuilt artifacts.

  If you use Docker for development this way, your production Dockerfile would
  copy the production-ready artifacts directly into the image, rather than
  relying on a bind mount.

- When the file or directory structure of the Docker host is guaranteed to be
  consistent with the bind mounts the containers require.

## Good use cases for tmpfs mounts

`tmpfs` mounts are best used for cases when you do not want the data to persist
either on the host machine or within the container. This may be for security
reasons or to protect the performance of the container when your application
needs to write a large volume of non-persistent state data.

## Tips for using bind mounts or volumes

If you use either bind mounts or volumes, keep the following in mind:

- If you mount an **empty volume** into a directory in the container in which files
  or directories exist, these files or directories are propagated (copied)
  into the volume. Similarly, if you start a container and specify a volume which
  does not already exist, an empty volume is created for you.
  This is a good way to pre-populate data that another container needs.

- If you mount a **bind mount or non-empty volume** into a directory in the container
  in which some files or directories exist, these files or directories are
  obscured by the mount, just as if you saved files into `/mnt` on a Linux host
  and then mounted a USB drive into `/mnt`. The contents of `/mnt` would be
  obscured by the contents of the USB drive until the USB drive were unmounted.
  The obscured files are not removed or altered, but are not accessible while the
  bind mount or volume is mounted.

## Next steps

- Learn more about [volumes](volumes.md).
- Learn more about [bind mounts](bind-mounts.md).
- Learn more about [tmpfs mounts](tmpfs.md).
- Learn more about [storage drivers](/storage/storagedriver/), which
  are not related to bind mounts or volumes, but allow you to store data in a
  container's writable layer.