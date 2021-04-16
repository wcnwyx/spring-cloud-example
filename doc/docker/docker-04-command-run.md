本文翻译自docker官网：[https://github.com/docker/cli/blob/master/docs/reference/commandline/run.md](https://github.com/docker/cli/blob/master/docs/reference/commandline/run.md)

# run

```markdown
Usage:  docker run [OPTIONS] IMAGE [COMMAND] [ARG...]

Run a command in a new container

Options:
      --add-host value                Add a custom host-to-IP mapping (host:ip) (default [])
  -a, --attach value                  Attach to STDIN, STDOUT or STDERR (default [])
      --blkio-weight value            Block IO (relative weight), between 10 and 1000
      --blkio-weight-device value     Block IO weight (relative device weight) (default [])
      --cap-add value                 Add Linux capabilities (default [])
      --cap-drop value                Drop Linux capabilities (default [])
      --cgroupns string               Cgroup namespace to use
                                      'host':    Run the container in the Docker host's cgroup namespace
                                      'private': Run the container in its own private cgroup namespace
                                      '':        Use the default Docker daemon cgroup namespace specified by the `--default-cgroupns-mode` option
      --cgroup-parent string          Optional parent cgroup for the container
      --cidfile string                Write the container ID to the file
      --cpu-count int                 The number of CPUs available for execution by the container.
                                      Windows daemon only. On Windows Server containers, this is
                                      approximated as a percentage of total CPU usage.
      --cpu-percent int               Limit percentage of CPU available for execution
                                      by the container. Windows daemon only.
                                      The processor resource controls are mutually
                                      exclusive, the order of precedence is CPUCount
                                      first, then CPUShares, and CPUPercent last.
      --cpu-period int                Limit CPU CFS (Completely Fair Scheduler) period
      --cpu-quota int                 Limit CPU CFS (Completely Fair Scheduler) quota
  -c, --cpu-shares int                CPU shares (relative weight)
      --cpus NanoCPUs                 Number of CPUs (default 0.000)
      --cpu-rt-period int             Limit the CPU real-time period in microseconds
      --cpu-rt-runtime int            Limit the CPU real-time runtime in microseconds
      --cpuset-cpus string            CPUs in which to allow execution (0-3, 0,1)
      --cpuset-mems string            MEMs in which to allow execution (0-3, 0,1)
  -d, --detach                        Run container in background and print container ID
      --detach-keys string            Override the key sequence for detaching a container
      --device value                  Add a host device to the container (default [])
      --device-cgroup-rule value      Add a rule to the cgroup allowed devices list
      --device-read-bps value         Limit read rate (bytes per second) from a device (default [])
      --device-read-iops value        Limit read rate (IO per second) from a device (default [])
      --device-write-bps value        Limit write rate (bytes per second) to a device (default [])
      --device-write-iops value       Limit write rate (IO per second) to a device (default [])
      --disable-content-trust         Skip image verification (default true)
      --dns value                     Set custom DNS servers (default [])
      --dns-option value              Set DNS options (default [])
      --dns-search value              Set custom DNS search domains (default [])
      --domainname string             Container NIS domain name
      --entrypoint string             Overwrite the default ENTRYPOINT of the image
  -e, --env value                     Set environment variables (default [])
      --env-file value                Read in a file of environment variables (default [])
      --expose value                  Expose a port or a range of ports (default [])
      --group-add value               Add additional groups to join (default [])
      --health-cmd string             Command to run to check health
      --health-interval duration      Time between running the check (ns|us|ms|s|m|h) (default 0s)
      --health-retries int            Consecutive failures needed to report unhealthy
      --health-timeout duration       Maximum time to allow one check to run (ns|us|ms|s|m|h) (default 0s)
      --health-start-period duration  Start period for the container to initialize before counting retries towards unstable (ns|us|ms|s|m|h) (default 0s)
      --help                          Print usage
  -h, --hostname string               Container host name
      --init                          Run an init inside the container that forwards signals and reaps processes
  -i, --interactive                   Keep STDIN open even if not attached
      --io-maxbandwidth string        Maximum IO bandwidth limit for the system drive (Windows only)
                                      (Windows only). The format is `<number><unit>`.
                                      Unit is optional and can be `b` (bytes per second),
                                      `k` (kilobytes per second), `m` (megabytes per second),
                                      or `g` (gigabytes per second). If you omit the unit,
                                      the system uses bytes per second.
                                      --io-maxbandwidth and --io-maxiops are mutually exclusive options.
      --io-maxiops uint               Maximum IOps limit for the system drive (Windows only)
      --ip string                     IPv4 address (e.g., 172.30.100.104)
      --ip6 string                    IPv6 address (e.g., 2001:db8::33)
      --ipc string                    IPC namespace to use
      --isolation string              Container isolation technology
      --kernel-memory string          Kernel memory limit
  -l, --label value                   Set meta data on a container (default [])
      --label-file value              Read in a line delimited file of labels (default [])
      --link value                    Add link to another container (default [])
      --link-local-ip value           Container IPv4/IPv6 link-local addresses (default [])
      --log-driver string             Logging driver for the container
      --log-opt value                 Log driver options (default [])
      --mac-address string            Container MAC address (e.g., 92:d0:c6:0a:29:33)
  -m, --memory string                 Memory limit
      --memory-reservation string     Memory soft limit
      --memory-swap string            Swap limit equal to memory plus swap: '-1' to enable unlimited swap
      --memory-swappiness int         Tune container memory swappiness (0 to 100) (default -1)
      --mount value                   Attach a filesystem mount to the container (default [])
      --name string                   Assign a name to the container
      --network-alias value           Add network-scoped alias for the container (default [])
      --network string                Connect a container to a network
                                      'bridge': create a network stack on the default Docker bridge
                                      'none': no networking
                                      'container:<name|id>': reuse another container's network stack
                                      'host': use the Docker host network stack
                                      '<network-name>|<network-id>': connect to a user-defined network
      --no-healthcheck                Disable any container-specified HEALTHCHECK
      --oom-kill-disable              Disable OOM Killer
      --oom-score-adj int             Tune host's OOM preferences (-1000 to 1000)
      --pid string                    PID namespace to use
      --pids-limit int                Tune container pids limit (set -1 for unlimited)
      --privileged                    Give extended privileges to this container
  -p, --publish value                 Publish a container's port(s) to the host (default [])
  -P, --publish-all                   Publish all exposed ports to random ports
      --read-only                     Mount the container's root filesystem as read only
      --restart string                Restart policy to apply when a container exits (default "no")
                                      Possible values are : no, on-failure[:max-retry], always, unless-stopped
      --rm                            Automatically remove the container when it exits
      --runtime string                Runtime to use for this container
      --security-opt value            Security Options (default [])
      --shm-size bytes                Size of /dev/shm
                                      The format is `<number><unit>`. `number` must be greater than `0`.
                                      Unit is optional and can be `b` (bytes), `k` (kilobytes), `m` (megabytes),
                                      or `g` (gigabytes). If you omit the unit, the system uses bytes.
      --sig-proxy                     Proxy received signals to the process (default true)
      --stop-signal string            Signal to stop a container (default "SIGTERM")
      --stop-timeout=10               Timeout (in seconds) to stop a container
      --storage-opt value             Storage driver options for the container (default [])
      --sysctl value                  Sysctl options (default map[])
      --tmpfs value                   Mount a tmpfs directory (default [])
  -t, --tty                           Allocate a pseudo-TTY
      --ulimit value                  Ulimit options (default [])
  -u, --user string                   Username or UID (format: <name|uid>[:<group|gid>])
      --userns string                 User namespace to use
                                      'host': Use the Docker host user namespace
                                      '': Use the Docker daemon user namespace specified by `--userns-remap` option.
      --uts string                    UTS namespace to use
  -v, --volume value                  Bind mount a volume (default []). The format
                                      is `[host-src:]container-dest[:<options>]`.
                                      The comma-delimited `options` are [rw|ro],
                                      [z|Z], [[r]shared|[r]slave|[r]private],
                                      [delegated|cached|consistent], and
                                      [nocopy]. The 'host-src' is an absolute path
                                      or a name value.
      --volume-driver string          Optional volume driver for the container
      --volumes-from value            Mount volumes from the specified container(s) (default [])
  -w, --workdir string                Working directory inside the container
```

## Description

The `docker run` command first `creates` a writeable container layer over the
specified image, and then `starts` it using the specified command. That is,
`docker run` is equivalent to the API `/containers/create` then
`/containers/(id)/start`. A stopped container can be restarted with all its
previous changes intact using `docker start`. See `docker ps -a` to view a list
of all containers.
> `docker run`指令首先会在指定的镜像上创建一个可写的容器层，然后使用指定指令启动它。
> `docker run`相当于API `/containers/create` 然后`/containers/(id)/start`。
> 一个停止的容器可以使用`docker start`重新启动，并且之前的所有更改都完好无损。

The `docker run` command can be used in combination with `docker commit` to
[*change the command that a container runs*](https://github.com/docker/cli/blob/master/docs/reference/commandline/commit.md). There is additional detailed information about `docker run` in the [Docker run reference](https://github.com/docker/cli/blob/master/docs/reference/run.md).
> `docker run`指令可以和`docker commit`指令结合使用，以更改容器运行的指令。
> `docker run`的其他详细信息在 [Docker run reference](https://github.com/docker/cli/blob/master/docs/reference/run.md).

For information on connecting a container to a network, see the ["*Docker network overview*"](https://docs.docker.com/network/).
> 有关将容器网络的信息，请参考 ["*Docker network overview*"](https://docs.docker.com/network/)

## Examples

### Assign name and allocate pseudo-TTY (--name, -it)
>指定名称并分配伪TTY（使用 --name, -it)

```bash
$ docker run --name test -it debian

root@d6c0fe130dba:/# exit 13
$ echo $?
13
$ docker ps -a | grep test
d6c0fe130dba        debian:7            "/bin/bash"         26 seconds ago      Exited (13) 17 seconds ago                         test
```

This example runs a container named `test` using the `debian:latest`
image. The `-it` instructs Docker to allocate a pseudo-TTY connected to
the container's stdin; creating an interactive `bash` shell in the container.
In the example, the `bash` shell is quit by entering
`exit 13`. This exit code is passed on to the caller of
`docker run`, and is recorded in the `test` container's metadata.
> 此例子使用`debian:latest`镜像运行了一个容器，并命名为`test`。
> `-it`指示Docker分配一个链接到容器stdin的伪TTY;在容器中创建交互式`bash`shell。
> 在该例子中，通过输入`exit 13`来退出`bash`shell。
> 这个推出代码被传递给`docker run`的调用者，并记录在测试容器的元数据中。

### Capture container ID (--cidfile)
> 捕获容器id（--cidfile)

```bash
$ docker run --cidfile /tmp/docker_test.cid ubuntu echo "test"
```

This will create a container and print `test` to the console. The `cidfile`
flag makes Docker attempt to create a new file and write the container ID to it.
If the file exists already, Docker will return an error. Docker will close this
file when `docker run` exits.
> 这将创建一个容器并打印`test`到控制台。
> `cidfile`标志使Docker尝试创建一个新文件并将容器 ID写入其中。
> 如果该文件已经存在，Dokcer将返回一个错误。当`docker run`退出时Docker将关闭该文件。

### Full container capabilities (--privileged)
> 完整的容器功能(--privileged 特权)

```bash
$ docker run -t -i --rm ubuntu bash
root@bc338942ef20:/# mount -t tmpfs none /mnt
mount: permission denied
```

This will *not* work, because by default, most potentially dangerous kernel
capabilities are dropped; including `cap_sys_admin` (which is required to mount
filesystems). However, the `--privileged` flag will allow it to run:
> 这将行不通，因为默认情况下，大多数潜在危险内核功能都会被丢弃；包括`cap_sys_admin`（装载文件系统需要）
> 但是 `--privileged`标识将允许它执行：

```bash
$ docker run -t -i --privileged ubuntu bash
root@50e3f57e16e6:/# mount -t tmpfs none /mnt
root@50e3f57e16e6:/# df -h
Filesystem      Size  Used Avail Use% Mounted on
none            1.9G     0  1.9G   0% /mnt
```

The `--privileged` flag gives *all* capabilities to the container, and it also
lifts all the limitations enforced by the `device` cgroup controller. In other
words, the container can then do almost everything that the host can do. This
flag exists to allow special use-cases, like running Docker within Docker.
> `--privileged` 标志给了容器所有的功能，它还解除了设备cgroup控制器强制执行的所有权限。
> 换句话说，容器可以主机可以执行的几乎所有操作。
> 此标志存在是为了允许特殊的用力，像是Docker中运行Docker（套娃）。

### Set working directory (-w)
> 设置工作目录

```bash
$ docker  run -w /path/to/dir/ -i -t  ubuntu pwd
```

The `-w` lets the command being executed inside directory given, here
`/path/to/dir/`. If the path does not exist it is created inside the container.
> `-w` 让指令在给定的目录中执行，这里`/path/to/dir`。
> 如果该路径不存在，则在容器内不创建该路径。

### Set storage driver options per container
> 设置每个容器存储驱动程序选项

```bash
$ docker run -it --storage-opt size=120G fedora /bin/bash
```

This (size) will allow to set the container rootfs size to 120G at creation time.
This option is only available for the `devicemapper`, `btrfs`, `overlay2`,
`windowsfilter` and `zfs` graph drivers.
For the `devicemapper`, `btrfs`, `windowsfilter` and `zfs` graph drivers,
user cannot pass a size less than the Default BaseFS Size.
For the `overlay2` storage driver, the size option is only available if the
backing fs is `xfs` and mounted with the `pquota` mount option.
Under these conditions, user can pass any size less than the backing fs size.
> 这个（size)允许在创建容器时将容器rootfs大小设置为120G。
> 此选项仅适用于`devicemapper`、 `btrfs`、`overlay2`、 `windowsfilter` 和 `zfs`图形驱动。
> 对于`devicemapper`、 `btrfs`、`overlay2`、 `windowsfilter` 和 `zfs`图形驱动，用户不能传递小于默认BaseFs大小的size。
> 对于`overlay2`驱动程序，只有在backing fs是`xfs`并且使用`pquota`安装选项安装的情况下size选项才有用。
> 在这些条件下，用户可以传递任何小于backing fs大小的size。

### Mount tmpfs (--tmpfs)
> 安装临时文件系统

```bash
$ docker run -d --tmpfs /run:rw,noexec,nosuid,size=65536k my_image
```

The `--tmpfs` flag mounts an empty tmpfs into the container with the `rw`,
`noexec`, `nosuid`, `size=65536k` options.
> `--tempfs` 标志将空的tempfs装在到容器中，使用`rw`,`noexec`, `nosuid`, `size=65536k`选项

### Mount volume (-v, --read-only)

```bash
$ docker  run  -v `pwd`:`pwd` -w `pwd` -i -t  ubuntu pwd
```

The `-v` flag mounts the current working directory into the container. The `-w`
lets the command being executed inside the current working directory, by
changing into the directory to the value returned by `pwd`. So this
combination executes the command using the container, but inside the
current working directory.
> `v`标志系那个当前工作目录装载到容器中。`-w`允许在当前工作目录中执行指令，通过将`pwd`返回的值更改到目录中。
> 所以这个组合使用容器执行命令，但是在当前工作目录中。

```bash
$ docker run -v /doesnt/exist:/foo -w /foo -i -t ubuntu bash
```

When the host directory of a bind-mounted volume doesn't exist, Docker
will automatically create this directory on the host for you. In the
example above, Docker will create the `/doesnt/exist`
folder before starting your container.
> 当绑定装载卷的主机目录不存在时，docker将自动为你在主机上创建该目录。
> 在上面的例子中，Docker在启动容器前将创建`/doesnt/exist`文件夹。

```bash
$ docker run --read-only -v /icanwrite busybox touch /icanwrite/here
```

Volumes can be used in combination with `--read-only` to control where
a container writes files. The `--read-only` flag mounts the container's root
filesystem as read only prohibiting writes to locations other than the
specified volumes for the container.
> 卷可以和`--read-only`结合使用，以控制容器写文件。
> `--read-only`标志将容器的根文件系统装载为只读，禁止写入容器的指定卷以外的位置

```bash
$ docker run -t -i -v /var/run/docker.sock:/var/run/docker.sock -v /path/to/static-docker-binary:/usr/bin/docker busybox sh
```

By bind-mounting the docker unix socket and statically linked docker
binary (refer to [get the linux binary](https://docs.docker.com/engine/install/binaries/#install-static-binaries)),
you give the container the full access to create and manipulate the host's
Docker daemon.
> 通过绑定挂载docker unix套接字和静态链接的docker二进制文件,
> 你授予容器创建和操作主机Docker守护进程的完全访问权。

On Windows, the paths must be specified using Windows-style semantics.
> 在Windos系统中，必须使用Windows样式的语义指定路径

```powershell
PS C:\> docker run -v c:\foo:c:\dest microsoft/nanoserver cmd /s /c type c:\dest\somefile.txt
Contents of file

PS C:\> docker run -v c:\foo:d: microsoft/nanoserver cmd /s /c type d:\somefile.txt
Contents of file
```

The following examples will fail when using Windows-based containers, as the
destination of a volume or bind mount inside the container must be one of:
a non-existing or empty directory; or a drive other than C:. Further, the source
of a bind mount must be a local directory, not a file.
> 当使用基于windows的容器时，以下示例将失效，
> 因为容器中的卷或绑定装载的目标必须是以下目录之一：不存在或空目录；或C：以外的驱动器。
> 此外，绑定装载的源必须是本地目录，而不是文件

```powershell
net use z: \\remotemachine\share
docker run -v z:\foo:c:\dest ...
docker run -v \\uncpath\to\directory:c:\dest ...
docker run -v c:\foo\somefile.txt:c:\dest ...
docker run -v c:\foo:c: ...
docker run -v c:\foo:c:\existing-directory-with-contents ...
```

For in-depth information about volumes, refer to [manage data in containers](https://docs.docker.com/storage/volumes/)
> 有关卷的详细信息，请参考[manage data in containers](https://docs.docker.com/storage/volumes/)


### Add bind mounts or volumes using the --mount flag
> 使用--mount标志添加绑定 mounts 或 volumes

The `--mount` flag allows you to mount volumes, host-directories and `tmpfs`
mounts in a container.
> `--mount`标志允许您在容器中装载卷、主机目录和tmpfs装载。

The `--mount` flag supports most options that are supported by the `-v` or the
`--volume` flag, but uses a different syntax. For in-depth information on the
`--mount` flag, and a comparison between `--volume` and `--mount`, refer to
the [service create command reference](service_create.md#add-bind-mounts-volumes-or-memory-filesystems).
> `--mount`标志支持`-v`或`--volume`标志支持的大多数选项，但使用不同的语法。
> 有关`--mount`标志的详细信息，以及`--volume`和`--mount`之间的比较，请参阅[service create command reference](https://github.com/docker/cli/blob/master/docs/reference/commandline/service_create.md#add-bind-mounts-volumes-or-memory-filesystems)

Even though there is no plan to deprecate `--volume`, usage of `--mount` is recommended.
> 即使没有计划否决`--volume`，也建议使用`--mount`。

Examples:

```bash
$ docker run --read-only --mount type=volume,target=/icanwrite busybox touch /icanwrite/here
```

```bash
$ docker run -t -i --mount type=bind,src=/data,dst=/data busybox sh
```

### Publish or expose port (-p, --expose)
> 公开或暴露端口

```bash
$ docker run -p 127.0.0.1:80:8080/tcp ubuntu bash
```

This binds port `8080` of the container to TCP port `80` on `127.0.0.1` of the host
machine. You can also specify `udp` and `sctp` ports.
The [Docker User Guide](https://docs.docker.com/network/links/)
explains in detail how to manipulate ports in Docker.
> 这会容器的`8080`端口绑定到主机`127.0.0.1`的TCP端口`80`上。你还可以指定`udp`和`sctp`端口。
> [Docker User Guide](https://docs.docker.com/network/links/) 中详细说明如何在Docker中操作端口。

Note that ports which are not bound to the host (i.e., `-p 80:80` instead of
`-p 127.0.0.1:80:80`) will be accessible from the outside. This also applies if
you configured UFW to block this specific port, as Docker manages his
own iptables rules. [Read more](https://docs.docker.com/network/iptables/)
> 请注意，可以从外部访问未绑定到主机的端口（即-p 80:80而不是-p 127.0.0.1:80:80）。
> 如果你将UFW配置为阻止此特定端口，这也适用，因为Docker管理自己的iptables规则。

```bash
$ docker run --expose 80 ubuntu bash
```

This exposes port `80` of the container without publishing the port to the host
system's interfaces.
> 这将公开容器的端口80，而不将端口发布到主机系统的接口

### Set environment variables (-e, --env, --env-file)
> 设置环境变量

```bash
$ docker run -e MYVAR1 --env MYVAR2=foo --env-file ./env.list ubuntu bash
```

Use the `-e`, `--env`, and `--env-file` flags to set simple (non-array)
environment variables in the container you're running, or overwrite variables
that are defined in the Dockerfile of the image you're running.
> 使用`-e`、`--env`和`--env-file` 标志 来设置正在运行的容器中的简单（非数组）环境变量，
> 或者覆盖正在运行的映像的Dockerfile中定义的变量

You can define the variable and its value when running the container:
> 在运行容器时，你可以定义变量及其值：

```bash
$ docker run --env VAR1=value1 --env VAR2=value2 ubuntu env | grep VAR
VAR1=value1
VAR2=value2
```

You can also use variables that you've exported to your local environment:
> 你还可以使用导出到本地环境变量的变量：

```bash
export VAR1=value1
export VAR2=value2

$ docker run --env VAR1 --env VAR2 ubuntu env | grep VAR
VAR1=value1
VAR2=value2
```

When running the command, the Docker CLI client checks the value the variable
has in your local environment and passes it to the container.
If no `=` is provided and that variable is not exported in your local
environment, the variable won't be set in the container.
> 运行该命令时，Docker CLI客户端将检查该变量在本地环境中的值，并将其传递给容器。
> 如果没有提供=并且该变量未在本地环境中导出，则变量将不会设置在容器中。

You can also load the environment variables from a file. This file should use
the syntax `<variable>=value` (which sets the variable to the given value) or
`<variable>` (which takes the value from the local environment), and `#` for comments.
> 你也可以从文件中加载环境变量。
> 此文件应该使用语法`<variable>=value`（将变量设置为给定值），或`<variable>`（从本地环境获取），`#`作为注释。

```bash
$ cat env.list
# This is a comment
VAR1=value1
VAR2=value2
USER

$ docker run --env-file env.list ubuntu env | grep VAR
VAR1=value1
VAR2=value2
USER=denis
```

### Set metadata on container (-l, --label, --label-file)

A label is a `key=value` pair that applies metadata to a container. To label a container with two labels:
> 一个标签（label）是将元数据据应用于容器的key=value对。用两个标签来标记容器：

```bash
$ docker run -l my-label --label com.example.foo=bar ubuntu bash
```

The `my-label` key doesn't specify a value so the label defaults to an empty
string (`""`). To add multiple labels, repeat the label flag (`-l` or `--label`).
> `my-lable`这个key没有指定一个值，因此该标签默认空字符串（`"""`）。
> 要添加多个标签，请重复标签标志（`-l` or `--label`）

The `key=value` must be unique to avoid overwriting the label value. If you
specify labels with identical keys but different values, each subsequent value
overwrites the previous. Docker uses the last `key=value` you supply.
> `key=value`必须是唯一的，以避免覆盖标签值。
> 如果制定具有相同key但value不同的标签，则每个后续值都将覆盖上一个值。
> Docker使用你最后提供的`key=value`。

Use the `--label-file` flag to load multiple labels from a file. Delimit each
label in the file with an EOL mark. The example below loads labels from a
labels file in the current directory:
> 使用`--label-file`标志从一个文件加载多个标签。 使用 EOL标记来分隔文件中的每个标签。
> 下面的示例是从当前目录的标签文件来加载标签：

```bash
$ docker run --label-file ./labels ubuntu bash
```

The label-file format is similar to the format for loading environment
variables. (Unlike environment variables, labels are not visible to processes
running inside a container.) The following example illustrates a label-file
format:
> 标签文件（label-file)的格式类似于加载环境变量的格式。（与环境变量不同的是，标签对于容器内的进程是不可以见的）
> 下面的示例演示了标签文件的格式：

```console
com.example.label1="a label"

# this is a comment
com.example.label2=another\ label
com.example.label3
```

You can load multiple label-files by supplying multiple  `--label-file` flags.
> 通过提供多个`--label-file`标志来加载多个标签文件。

For additional information on working with labels, see [*Labels - custom
metadata in Docker*](https://docs.docker.com/config/labels-custom-metadata/) in
the Docker User Guide.
> 有关标签的更多详情请参考[*Labels - custom
metadata in Docker*](https://docs.docker.com/config/labels-custom-metadata/)

### Connect a container to a network (--network)
> 将容器连接到一个网络（--network）

When you start a container use the `--network` flag to connect it to a network.
This adds the `busybox` container to the `my-net` network.
> 当你启动容器时，为了将它链接到一个网络，请使用`--network`标志启动。
> 这将 `busybox`容器添加到 `my-net`网络。

```bash
$ docker run -itd --network=my-net busybox
```

You can also choose the IP addresses for the container with `--ip` and `--ip6`
flags when you start the container on a user-defined network.
> 在用户自定义的网络上启动一个容器时，你还可以通过`--ip` 和 `--ip6`的标志来选择容器的IP。

```bash
$ docker run -itd --network=my-net --ip=10.10.9.75 busybox
```

If you want to add a running container to a network use the `docker network connect` subcommand.
> 如果你想将运行中的容器添加到网络中，请使用`docker network connect`子命令。

You can connect multiple containers to the same network. Once connected, the
containers can communicate easily need only another container's IP address
or name. For `overlay` networks or custom plugins that support multi-host
connectivity, containers connected to the same multi-host network but launched
from different Engines can also communicate in this way.
> 你可以将多个容器链接到相同的网络。
> 一旦链接山，容器只需要另一个容器的IP地址或者名字可以方便的通信了。
> 对于支持multi-host链接的`overlay`网络或自定义插件，链接到同一个multi-host的容器，
> 但是从不同启动引擎启动的容器也可以通过这种方式来进行通信。

> **Note**
>
> Service discovery is unavailable on the default bridge network. Containers can
> communicate via their IP addresses by default. To communicate by name, they
> must be linked.
> 服务发现在默认的网桥(bridge)网络上不可用。默认情况下容器通过他们的IP地址通信。
> 如果要通过名字来通信，它们必须连接起来。

You can disconnect a container from a network using the `docker network
disconnect` command.
> 你可以使用`docker network disconnect`指令来容器和网络的链接。

### Mount volumes from container (--volumes-from)
> 从容器挂载卷

```bash
$ docker run --volumes-from 777f7dc92da7 --volumes-from ba8c0c54f0f2:ro -i -t ubuntu pwd
```

The `--volumes-from` flag mounts all the defined volumes from the referenced
containers. Containers can be specified by repetitions of the `--volumes-from`
argument. The container ID may be optionally suffixed with `:ro` or `:rw` to
mount the volumes in read-only or read-write mode, respectively. By default,
the volumes are mounted in the same mode (read write or read only) as
the reference container.
> `--volumes-from`标志从引用容器装载所有定义的卷。容器可以通过重复`--volumes-from`参数来指定。
> 容器id可以可选的加上`:ro`或者`:rw`后最，用来分别以只读(read-only)、读写(read-write)模式转载卷。
> 默认情况下，卷将以和引用容器相同的模式（读写模式或者只读模式）来装载

Labeling systems like SELinux require that proper labels are placed on volume
content mounted into a container. Without a label, the security system might
prevent the processes running inside the container from using the content. By
default, Docker does not change the labels set by the OS.
> 像 SELinux这样的标签系统要求在装入容器的卷内容上放置适当的标签。
> 如果没有标签，安全系统可能会阻止容器内运行的进程使用内容。
> 默认情况下，Docker不会改变操作系统设置的标签。

To change the label in the container context, you can add either of two suffixes
`:z` or `:Z` to the volume mount. These suffixes tell Docker to relabel file
objects on the shared volumes. The `z` option tells Docker that two containers
share the volume content. As a result, Docker labels the content with a shared
content label. Shared volume labels allow all containers to read/write content.
The `Z` option tells Docker to label the content with a private unshared label.
Only the current container can use a private volume.
> 要改变容器上下文中的标签，你可以向卷装载添加两个后缀`:z` or `:Z`。
> 这些后缀告诉Docker重新标记共享卷上的文件对象。
> `z`选项告诉Docker两个容器共享卷内容。因此，Docker使用共享标签来标记内容。共享卷标志允许所有容器读/写内容。
> `Z`选项告诉Docker使用私有的非共享标签来标记内容。只有当前容器才能使用专用卷。

### Attach to STDIN/STDOUT/STDERR (-a)
> 链接到 STDIN/STDOUT/STDERR

The `-a` flag tells `docker run` to bind to the container's `STDIN`, `STDOUT`
or `STDERR`. This makes it possible to manipulate the output and input as
needed.
> `-a` 标志告诉 `docker run` 绑定到容器的 `STDIN`、 `STDOUT`或 `STDERR`。
> 这使得可以根据需要操纵输出和输入。

```bash
$ echo "test" | docker run -i -a stdin ubuntu cat -
```

This pipes data into a container and prints the container's ID by attaching
only to the container's `STDIN`.
> 这将数据管道化到容器中，并通过仅连接到容器的STDIN来打印容器的ID。

```bash
$ docker run -a stderr ubuntu echo test
```

This isn't going to print anything unless there's an error because we've
only attached to the `STDERR` of the container. The container's logs
still store what's been written to `STDERR` and `STDOUT`.
> 除非出现错误，否则不会打印任何内容，因为我们只链接到容器的`STDERR`。
> 容器的日志仍然存储写入`STDERR` and `STDOUT`的内容。

```bash
$ cat somefile | docker run -i -a stdin mybuilder dobuild
```

This is how piping a file into a container could be done for a build.
The container's ID will be printed after the build is done and the build
logs could be retrieved using `docker logs`. This is
useful if you need to pipe a file or something else into a container and
retrieve the container's ID once the container has finished running.
> 这就是将文件管道化到容器的方法。
> 容器的ID将在构建完成后打印，并且可以使用`docker logs`检索构建日志。
> 如果你需要将文件或者其他内容通过管道传输到容器，并在容器运行完成后检索容器的ID，那么这将非常有用。

### Add host device to container (--device)
> 将主机设备添加到容器。

```bash
$ docker run --device=/dev/sdc:/dev/xvdc \
             --device=/dev/sdd --device=/dev/zero:/dev/nulo \
             -i -t \
             ubuntu ls -l /dev/{xvdc,sdd,nulo}

brw-rw---- 1 root disk 8, 2 Feb  9 16:05 /dev/xvdc
brw-rw---- 1 root disk 8, 3 Feb  9 16:05 /dev/sdd
crw-rw-rw- 1 root root 1, 5 Feb  9 16:05 /dev/nulo
```

It is often necessary to directly expose devices to a container. The `--device`
option enables that. For example, a specific block storage device or loop
device or audio device can be added to an otherwise unprivileged container
(without the `--privileged` flag) and have the application directly access it.
> 通常需要将设备直接暴露给容器。`--device`选项可以做到。
> 例如，可以将特定的存储块设备、循环设备或者音频设备添加到一个没有特权的容器（没有 `--privileged`标志），
> 并让应用程序直接使用它。

By default, the container will be able to `read`, `write` and `mknod` these devices.
This can be overridden using a third `:rwm` set of options to each `--device`
flag. If the container is running in privileged mode, then the permissions specified
will be ignored.
> 默认情况下，容器将能够读、写和管理这些设备。可以使用三个`:rwm`选项集合来覆盖每个`--device`标志。
> 如果容器以特权模式运行，则指定的权限将被忽略。

```bash
$ docker run --device=/dev/sda:/dev/xvdc --rm -it ubuntu fdisk  /dev/xvdc

Command (m for help): q
$ docker run --device=/dev/sda:/dev/xvdc:r --rm -it ubuntu fdisk  /dev/xvdc
You will not be able to write the partition table.

Command (m for help): q

$ docker run --device=/dev/sda:/dev/xvdc:rw --rm -it ubuntu fdisk  /dev/xvdc

Command (m for help): q

$ docker run --device=/dev/sda:/dev/xvdc:m --rm -it ubuntu fdisk  /dev/xvdc
fdisk: unable to open /dev/xvdc: Operation not permitted
```

> **Note**
>
> The `--device` option cannot be safely used with ephemeral devices. Block devices
> that may be removed should not be added to untrusted containers with `--device`.
> `--device`选项不能安全的用于临时设备。
> 不应该适用`--device`将可能被删除的块设备添加到不受信任的容器中。

For Windows, the format of the string passed to the `--device` option is in
the form of `--device=<IdType>/<Id>`. Beginning with Windows Server 2019
and Windows 10 October 2018 Update, Windows only supports an IdType of
`class` and the Id as a [device interface class
GUID](https://docs.microsoft.com/en-us/windows-hardware/drivers/install/overview-of-device-interface-classes).
Refer to the table defined in the [Windows container
docs](https://docs.microsoft.com/en-us/virtualization/windowscontainers/deploy-containers/hardware-devices-in-containers)
for a list of container-supported device interface class GUIDs.
> 对于Windows，传递给`--device`选项的字符串格式为`--device=<IdType>/<Id>`。
> 从Windows Server 2019 到 Windows 10 October 2018 Update,Windows仅支持类的IdType和作为设备接口类GUID的Id。
> 有关容器支持的设备接口类guid的列表，请参阅Windows容器文档中定义的表。

If this option is specified for a process-isolated Windows container, _all_
devices that implement the requested device interface class GUID are made
available in the container. For example, the command below makes all COM
ports on the host visible in the container.
> 如果为进程隔离的Windows容器指定了此选项，实现请求的设备接口类GUID的所有设备都在容器中可用。
> 例如，下面的命令使主机上的所有COM端口在容器中可见。

```powershell
PS C:\> docker run --device=class/86E0D1E0-8089-11D0-9CE4-08003E301F73 mcr.microsoft.com/windows/servercore:ltsc2019
```

> **Note**
>
> The `--device` option is only supported on process-isolated Windows containers.
> This option fails if the container isolation is `hyperv` or when running Linux
> Containers on Windows (LCOW).

### Access an NVIDIA GPU

The `--gpus` flag allows you to access NVIDIA GPU resources. First you need to
install [nvidia-container-runtime](https://nvidia.github.io/nvidia-container-runtime/).
Visit [Specify a container's resources](https://docs.docker.com/config/containers/resource_constraints/)
for more information.
> `--gpus`标志允许你访问 NVIDIA GPU 资源。首先需要安装 [nvidia-container-runtime](https://nvidia.github.io/nvidia-container-runtime/)。
> 访问 [Specify a container's resources](https://docs.docker.com/config/containers/resource_constraints/) 获取更多信息。

To use `--gpus`, specify which GPUs (or all) to use. If no value is provied, all
available GPUs are used. The example below exposes all available GPUs.
> 使用`--gpus` 指定哪个GPU（或者所有）使用。如果没有提供值，所有可用的GPU都被使用。
> 下面的示例公开了所有可用的GPU。

```bash
$ docker run -it --rm --gpus all ubuntu nvidia-smi
```

Use the `device` option to specify GPUs. The example below exposes a specific
GPU.
> 使用`device`选项制定GPU。下面的示例公开了一个特定的GPU。

```bash
$ docker run -it --rm --gpus device=GPU-3a23c669-1f69-c64e-cf85-44e9b07e7a2a ubuntu nvidia-smi
```

The example below exposes the first and third GPUs.
> 下面的示例公开了第一个和第三个GPU。

```bash
$ docker run -it --rm --gpus device=0,2 nvidia-smi
```

### Restart policies (--restart)
> 重启策略

Use Docker's `--restart` to specify a container's *restart policy*. A restart
policy controls whether the Docker daemon restarts a container after exit.
Docker supports the following restart policies:
> 使用 Docker的 `--restart`来指定容器的重启策略。
> 一个重启策略控制Docker守护进程是否在容器推出后重新启动它。
> Docker支持一下重启策略：

| Policy                     | Result                                                                                                                                                                                                                                                           |
|:---------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `no`                       | Do not automatically restart the container when it exits. This is the default.                                                                                                                                                                                   |
| `on-failure[:max-retries]` | Restart only if the container exits with a non-zero exit status. Optionally, limit the number of restart retries the Docker daemon attempts.                                                                                                                     |
| `unless-stopped`           | Restart the container unless it is explicitly stopped or Docker itself is stopped or restarted.                                                                                                                                                                  |
| `always`                   | Always restart the container regardless of the exit status. When you specify always, the Docker daemon will try to restart the container indefinitely. The container will also always start on daemon startup, regardless of the current state of the container. |

> `no` : 不要在容器退出时重启它，这是默认值。
> `on-failure[:max-retries]` : 只在容器以非0状态退出时才重启。（可选）限制Docker守护进程尝试重启的次数。
> `unless-stopped` : 重新启动容器，除非容器已显式停止或者Docker自身已停止或重新启动。
> `always` : 无论推出状态如何，始终重启容器。当你指定`always`，Docker守护进程将无限期地重启容器。无论容器的当前状态如何，容器总在守护进程启动时启动。

```bash
$ docker run --restart=always redis
```

This will run the `redis` container with a restart policy of **always**
so that if the container exits, Docker will restart it.
> 这将以 **always** 的重启策略运行`redis`，所以如果容器推出，Docker将重启它。

More detailed information on restart policies can be found in the
[Restart Policies (--restart)](../run.md#restart-policies---restart)
section of the Docker run reference page.
> 在Docker run 参考页有更多的关于重启策略详情  [Restart Policies (--restart)](https://github.com/docker/cli/blob/master/docs/reference/run.md#restart-policies---restart)

### Add entries to container hosts file (--add-host)
> 向容器的hosts文件添加条目

You can add other hosts into a container's `/etc/hosts` file by using one or
more `--add-host` flags. This example adds a static address for a host named
`docker`:
> 你可以通过使用一个或多个`--add-host`标志其他hosts添加到容器的`/ect/hosts`文件

```bash
$ docker run --add-host=docker:10.180.0.1 --rm -it debian

root@f38c87f2a42d:/# ping docker
PING docker (10.180.0.1): 48 data bytes
56 bytes from 10.180.0.1: icmp_seq=0 ttl=254 time=7.600 ms
56 bytes from 10.180.0.1: icmp_seq=1 ttl=254 time=30.705 ms
^C--- docker ping statistics ---
2 packets transmitted, 2 packets received, 0% packet loss
round-trip min/avg/max/stddev = 7.600/19.152/30.705/11.553 ms
```

Sometimes you need to connect to the Docker host from within your
container. To enable this, pass the Docker host's IP address to
the container using the `--add-host` flag. To find the host's address,
use the `ip addr show` command.
> 有时候你需要从容器内链接Docker主机。要使用此功能，请使用`--add-host`标志将Docker主机的IP地址传递给容器。
> 要查询主机地址，请使用`ip addr show`指令。

The flags you pass to `ip addr show` depend on whether you are
using IPv4 or IPv6 networking in your containers. Use the following
flags for IPv4 address retrieval for a network device named `eth0`:
> 传递给`ip addr show`的标志取决于容器中使用的是IPV4还是IPV6网络。
> 使用以下标志来检索名为`eth0`d额网络设备的IPV4地址。

```bash
$ HOSTIP=`ip -4 addr show scope global dev eth0 | grep inet | awk '{print $2}' | cut -d / -f 1 | sed -n 1p`
$ docker run  --add-host=docker:${HOSTIP} --rm -it debian
```

For IPv6 use the `-6` flag instead of the `-4` flag. For other network
devices, replace `eth0` with the correct device name (for example `docker0`
for the bridge device).
> 对于IPV6，使用`-6` 标志替代 `-4`标志。
> 对于其它网路哦设备，使用正确的设备名称替代`eth0`(例如 网桥设备的`docker0` )

### Set ulimits in container (--ulimit)
> 在容器中设置限制

Since setting `ulimit` settings in a container requires extra privileges not
available in the default container, you can set these using the `--ulimit` flag.
`--ulimit` is specified with a soft and hard limit as such:
`<type>=<soft limit>[:<hard limit>]`, for example:
> 在容器中如果需要默认容器中不可用的额外特权，可以使用`--ulimit`标志来设置这些特权。
> `--ulimit`被指定为软限制和应限制 比如:<type>=<soft limit>[:<hard limit>]`, 例如：

```bash
$ docker run --ulimit nofile=1024:1024 --rm debian sh -c "ulimit -n"
1024
```

> **Note**
>
> If you do not provide a `hard limit`, the `soft limit` is used
> for both values. If no `ulimits` are set, they are inherited from
> the default `ulimits` set on the daemon. The `as` option is disabled now.
> In other words, the following script is not supported:
> 如果你不提供`hard limit`， 则`soft limit` 将应用于这两个值。
> 如果为设置`ulimit`，则从守护进程上设置的默认`ulimit`继承。
>
> ```bash
> $ docker run -it --ulimit as=1024 fedora /bin/bash`
> ```

The values are sent to the appropriate `syscall` as they are set.
Docker doesn't perform any byte conversion. Take this into account when setting the values.
> 这些值在设置时被发送到相应的syscall。Dockers不执行任何字节转换。设置值时要考虑到这一点。

#### For `nproc` usage

Be careful setting `nproc` with the `ulimit` flag as `nproc` is designed by Linux to set the
maximum number of processes available to a user, not to a container.  For example, start four
containers with `daemon` user:
> 请小心使用`ulimit`标志设置`nproc`，因为`nproc`是由Linux设计的，用于设置用户可用的最大进程数，而不是容器容器可用的最大进程数。
> 例如，用`daemon`用户启动四个容器：

```bash
$ docker run -d -u daemon --ulimit nproc=3 busybox top

$ docker run -d -u daemon --ulimit nproc=3 busybox top

$ docker run -d -u daemon --ulimit nproc=3 busybox top

$ docker run -d -u daemon --ulimit nproc=3 busybox top
```

The 4th container fails and reports "[8] System error: resource temporarily unavailable" error.
This fails because the caller set `nproc=3` resulting in the first three containers using up
the three processes quota set for the `daemon` user.
> 第四个容器失败报告"[8] System error: resource temporarily unavailable"错误。
> 失败是因为调用者设置`nproc=3`导致前三个容器使用了为`daemon`用户设置的三个进程配额。

### Stop container with signal (--stop-signal)
> 带信号停止容器

The `--stop-signal` flag sets the system call signal that will be sent to the container to exit.
This signal can be a valid unsigned number that matches a position in the kernel's syscall table, for instance 9,
or a signal name in the format SIGNAME, for instance SIGKILL.
> `--stop signal`标志设置将发送到容器以退出的系统调用信号。
> 这个信号可以是一个有效的无符号数字，它与内核syscall表中的一个位置相匹配，例如9，也可以是SIGNAME格式的信号名，例如SIGKILL。

### Optional security options (--security-opt)
> 可选安全选项

On Windows, this flag can be used to specify the `credentialspec` option.
The `credentialspec` must be in the format `file://spec.txt` or `registry://keyname`.
> 在Windos上，此标志可以用于指定 `credentialspec` 选项。
> `credentialspec` 的格式必须是`file://spec.txt` or `registry://keyname`。

### Stop container with timeout (--stop-timeout)
> 使用超时来停止容器

The `--stop-timeout` flag sets the timeout (in seconds) that a pre-defined (see `--stop-signal`) system call
signal that will be sent to the container to exit. After timeout elapses the container will be killed with SIGKILL.
> `--stop timeout`标志设置将发送到容器以退出的预定义（请参阅`--stop signal`）系统调用信号的超时（以秒为单位）。
> 超时过后，容器将被SIGKILL杀死。

### Specify isolation technology for container (--isolation)
> 指定容器的隔离技术

This option is useful in situations where you are running Docker containers on
Windows. The `--isolation <value>` option sets a container's isolation technology.
On Linux, the only supported is the `default` option which uses
Linux namespaces. These two commands are equivalent on Linux:
> 此选项在Windows上运行Docker容器的情况下非常有用。`--isolation<value>`选项设置容器的隔离技术。
> 在Linux上，唯一受支持的是使用Linux namespace的默认选项。这两个命令在Linux上是等效的：

```bash
$ docker run -d busybox top
$ docker run -d --isolation default busybox top
```

On Windows, `--isolation` can take one of these values:
> 在Windows上，`--isolation` 可以采用以下值之一


| Value     | Description                                                                                                       |
|:----------|:------------------------------------------------------------------------------------------------------------------|
| `default` | Use the value specified by the Docker daemon's `--exec-opt` or system default (see below).                        |
| `process` | Shared-kernel namespace isolation (not supported on Windows client operating systems older than Windows 10 1809). |
| `hyperv`  | Hyper-V hypervisor partition-based isolation.                                                                     |
> `default` : 使用Docker守护进程的`--exec opt`或系统默认值指定的值（见下文）。  
> `process` : 共享内核（Shared-kernel） namespace隔离（在早于Windows 10 1809的Windows客户端操作系统上不受支持）。  
> `hyperv` : 基于Hyper-V虚拟机监控程序分区的隔离。

The default isolation on Windows server operating systems is `process`. The default
isolation on Windows client operating systems is `hyperv`. An attempt to start a container on a client
operating system older than Windows 10 1809 with `--isolation process` will fail.
> Windows服务器操作系统上的默认隔离是`process`。Windows客户端操作系统上的默认隔离是`hyperv`。
> 尝试在早于Windows101809的客户端操作系统上用`--isolation process`启动容器将失败。

On Windows server, assuming the default configuration, these commands are equivalent
and result in `process` isolation:
> 在Windows服务器上，假设默认配置，这些命令导致的`process`隔离是等效的：

```powershell
PS C:\> docker run -d microsoft/nanoserver powershell echo process
PS C:\> docker run -d --isolation default microsoft/nanoserver powershell echo process
PS C:\> docker run -d --isolation process microsoft/nanoserver powershell echo process
```

If you have set the `--exec-opt isolation=hyperv` option on the Docker `daemon`, or
are running against a Windows client-based daemon, these commands are equivalent and
result in `hyperv` isolation:
> 如果您已经在Docker守护程序上设置了`--exec-opt isolation=hyperv`选项，
> 或者正在针对基于Windows客户端的守护程序运行，那么这些命令是等效的，并且会导致`hyperv`隔离：

```powershell
PS C:\> docker run -d microsoft/nanoserver powershell echo hyperv
PS C:\> docker run -d --isolation default microsoft/nanoserver powershell echo hyperv
PS C:\> docker run -d --isolation hyperv microsoft/nanoserver powershell echo hyperv
```

### Specify hard limits on memory available to containers (-m, --memory)
> 指定容器可用内存的硬限制

These parameters always set an upper limit on the memory available to the container. On Linux, this
is set on the cgroup and applications in a container can query it at `/sys/fs/cgroup/memory/memory.limit_in_bytes`.
> 这些参数总是设置容器可用内存的上限。
> 在Linux上，这是在cgroup上设置的，容器中的应用程序可以在`/sys/fs/cgroup/memory/memory.limit_in_bytes`查询它。

On Windows, this will affect containers differently depending on what type of isolation is used.
> 在Windows上，这将根据所使用的隔离类型对容器产生不同的影响

- With `process` isolation, Windows will report the full memory of the host system, not the limit to applications running inside the containe
  > 使用`process`隔离，Windows将报告主机系统的全部内存，而不是对容器内运行的应用程序的限制
    ```powershell
    PS C:\> docker run -it -m 2GB --isolation=process microsoft/nanoserver powershell Get-ComputerInfo *memory*

    CsTotalPhysicalMemory      : 17064509440
    CsPhyicallyInstalledMemory : 16777216
    OsTotalVisibleMemorySize   : 16664560
    OsFreePhysicalMemory       : 14646720
    OsTotalVirtualMemorySize   : 19154928
    OsFreeVirtualMemory        : 17197440
    OsInUseVirtualMemory       : 1957488
    OsMaxProcessMemorySize     : 137438953344
    ```

- With `hyperv` isolation, Windows will create a utility VM that is big enough to hold the memory limit, plus the minimal OS needed to host the container. That size is reported as "Total Physical Memory."
  > 使用`hyperv`隔离，Windows将创建一个足够大的实用虚拟机来容纳内存限制，再加上托管容器所需的最小操作系统。
  > 这个大小被报告为“Total Physical Memory”

    ```powershell
    PS C:\> docker run -it -m 2GB --isolation=hyperv microsoft/nanoserver powershell Get-ComputerInfo *memory*

    CsTotalPhysicalMemory      : 2683355136
    CsPhyicallyInstalledMemory :
    OsTotalVisibleMemorySize   : 2620464
    OsFreePhysicalMemory       : 2306552
    OsTotalVirtualMemorySize   : 2620464
    OsFreeVirtualMemory        : 2356692
    OsInUseVirtualMemory       : 263772
    OsMaxProcessMemorySize     : 137438953344
    ```


### Configure namespaced kernel parameters (sysctls) at runtime
> 在运行时配置命名空间内核参数（sysctls)

The `--sysctl` sets namespaced kernel parameters (sysctls) in the
container. For example, to turn on IP forwarding in the containers
network namespace, run this command:
> `--sysctl`设置容器中的命名空间内核参数（sysctl）。
> 例如，要在容器网络名称空间中启用IP转发，请运行以下命令：

```bash
$ docker run --sysctl net.ipv4.ip_forward=1 someimage
```

> **Note**
>
> Not all sysctls are namespaced. Docker does not support changing sysctls
> inside of a container that also modify the host system. As the kernel
> evolves we expect to see more sysctls become namespaced.
> 并非所有sysctl都有名称空间。Docker不支持在同时修改主机系统的容器内更改sysctl。
> 随着内核的发展，我们期望看到更多的sysctl被命名。

#### Currently supported sysctls
> 目前支持的sysctls

IPC Namespace:

- `kernel.msgmax`, `kernel.msgmnb`, `kernel.msgmni`, `kernel.sem`,
  `kernel.shmall`, `kernel.shmmax`, `kernel.shmmni`, `kernel.shm_rmid_forced`.
- Sysctls beginning with `fs.mqueue.*`
- If you use the `--ipc=host` option these sysctls are not allowed.

Network Namespace:

- Sysctls beginning with `net.*`
- If you use the `--network=host` option using these sysctls are not allowed.