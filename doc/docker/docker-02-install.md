本文翻译自docker官网：[https://docs.docker.com/engine/install/centos/](https://docs.docker.com/engine/install/centos/)

#Install Docker Engine on CentOS
To get started with Docker Engine on CentOS, make sure you
[meet the prerequisites](#prerequisites), then
[install Docker](#installation-methods).
>要开始在CentOS上使用Docker引擎，请确保满足先决条件，然后安装Docker。

## Prerequisites

### OS requirements

To install Docker Engine, you need a maintained version of CentOS 7 or 8.
Archived versions aren't supported or tested.
>若要安装Docker引擎， 你需要CentOS 7或8的维护版本。 不支持或测试存档版本。

The `centos-extras` repository must be enabled. This repository is enabled by
default, but if you have disabled it, you need to
[re-enable it](https://wiki.centos.org/AdditionalResources/Repositories)
>必须启用`centos-extras`存储库。默认情情况下，此存储库处于开启状态，但如果你已禁用它，你需要重新启用它。


The `overlay2` storage driver is recommended.
>建议使用`overlay2`存储驱动程序。

### Uninstall old versions

Older versions of Docker were called `docker` or `docker-engine`. If these are
installed, uninstall them, along with associated dependencies.
>旧版本的docker称为`docker`或`docker-engine`。如果安装了这些，请卸载它们以及相关依赖项。

```console
$ sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
```

It's OK if `yum` reports that none of these packages are installed.
>如果`yum`报告没有安装这些包就OK了。

The contents of `/var/lib/docker/`, including images, containers, volumes, and
networks, are preserved. The Docker Engine package is now called `docker-ce`.
>`/var/lib/docker/`的内容（包括镜像、容器、卷、网络）将被保留。Docker引擎包现在成为`docker-ce`

## Installation methods

You can install Docker Engine in different ways, depending on your needs:
>你可以根据需要以不同的方式安装Docker Engine：
- Most users
  [set up Docker’s repositories](https://docs.docker.com/engine/install/centos/#install-using-the-repository) and install
  from them, for ease of installation and upgrade tasks. This is the
  recommended approach.
  > 大多数用户设置Docker的repositories并从中安装，以便安装和升级任务。这是推荐的方法。
  
- Some users download the RPM package and
  [install it manually](https://docs.docker.com/engine/install/centos/#install-from-a-package) and manage
  upgrades completely manually. This is useful in situations such as installing
  Docker on air-gapped systems with no access to the internet.
  > 有些用户下载RPM包并手动安装，完全手动管理升级。这在一些情况下很有用，比如在没有互联网接入的air-gapped（气隙）系统上安装Docker。

- In testing and development environments, some users choose to use automated
  [convenience scripts](https://docs.docker.com/engine/install/centos/#install-using-the-convenience-script) to install Docker.
  > 在测试和开发环境中，一些用户选择使用自动化的便利脚本来安装Docker。


### Install using the repository

Before you install Docker Engine for the first time on a new host machine, you need
to set up the Docker repository. Afterward, you can install and update Docker
from the repository.
> 在一台新的主机上首次安装Docker Engine之前，你需要设置Docker的repository。然后，你可以从该repository安装和更新Docker。

#### Set up the repository

Install the `yum-utils` package (which provides the `yum-config-manager`
utility) and set up the **stable** repository.
> 安装`yum-utils`包（它提供了yum config manager实用程序）并且设置稳定的repository。

```console
$ sudo yum install -y yum-utils

$ sudo yum-config-manager \
    --add-repo \
    {{ download-url-base }}/docker-ce.repo
```

#### Install Docker Engine

1.  Install the _latest version_ of Docker Engine and containerd, or go to the next step to install a specific version:
    >安装最新版本的Docker Engine和contianerd，或者转至下一步安装特定版本的：
    
    ```console
    $ sudo yum install docker-ce docker-ce-cli containerd.io
    ```

    If prompted to accept the GPG key, verify that the fingerprint matches
    `060A 61C5 1B55 8A7F 742B 77AA C52F EB6B 621E 9F35`, and if so, accept it.
    > 如果提示接受GPG秘钥，验证指纹是否与`060A 61C5 1B55 8A7F 742B 77AA C52F EB6B 621E 9F35`相匹配，如果匹配，接受它。

    Docker is installed but not started. The `docker` group is created, but no users are added to the group.
    >Docker已经安装但是并没有启动。`docker`组已经创建，但是没有用户加到该组。

2.  To install a _specific version_ of Docker Engine, list the available versions
    in the repo, then select and install:
    >要安装特定版本的Docker Engine，在repo中列出所有可用版本，然后选择并安装：

    a. List and sort the versions available in your repo. This example sorts
    results by version number, highest to lowest, and is truncated:
    >列出并排序repo中可用的版本。此例子通过版本号排序结果，从高到底，并被截断：

    ```console
    $ yum list docker-ce --showduplicates | sort -r

    docker-ce.x86_64  3:18.09.1-3.el7                     docker-ce-stable
    docker-ce.x86_64  3:18.09.0-3.el7                     docker-ce-stable
    docker-ce.x86_64  18.06.1.ce-3.el7                    docker-ce-stable
    docker-ce.x86_64  18.06.0.ce-3.el7                    docker-ce-stable
    ```

    The list returned depends on which repositories are enabled, and is specific
    to your version of CentOS (indicated by the `.el7` suffix in this example).
    >返回列表取决于你启用了哪些repositories，并且特定于你的CentOS版本（在本例中以.el7后缀表示）

    b. Install a specific version by its fully qualified package name, which is
    the package name (`docker-ce`) plus the version string (2nd column)
    starting at the first colon (`:`), up to the first hyphen, separated by
    a hyphen (`-`). For example, `docker-ce-18.09.1`.
    >按其完全限定的软件包名称（即软件包名称（docker ce）加上版本字符串（第2列），从第一个冒号（：）开始，一直到第一个连字符，用连字符（-）分隔）安装特定版本。例如，docker-ce-18.09.1

    ```console
    $ sudo yum install docker-ce-<VERSION_STRING> docker-ce-cli-<VERSION_STRING> containerd.io
    ```

    Docker is installed but not started. The `docker` group is created, but no users are added to the group.
    >Docker已经安装但是并没有启动。`docker`组已经创建，但是没有用户加到该组。

3.  Start Docker.

    ```console
    $ sudo systemctl start docker
    ```

4.  Verify that Docker Engine is installed correctly by running the `hello-world`
    image.
    >通过运行`hello-world`镜像来验证Docker Engine是否安装正确。

    ```console
    $ sudo docker run hello-world
    ```

    This command downloads a test image and runs it in a container. When the
    container runs, it prints an informational message and exits.
    > 该指令下载一个测试镜像并在一个容器内运行它。当容器运行时，它会打印一个信息性的消息并退出。

Docker Engine is installed and running. You need to use `sudo` to run Docker
commands. Continue to [Linux post install](https://docs.docker.com/engine/install/linux-postinstall/) to allow
non-privileged users to run Docker commands and for other optional configuration
steps.
>Docker Engine已经安装完成并运行。 你需要使用`sudo`来运行Docker指令。
> 继续[Linux post install](https://docs.docker.com/engine/install/linux-postinstall/) 以允许非特权用户运行Docker指令和其它可选配置步骤。

#### Upgrade Docker Engine

To upgrade Docker Engine, follow the [installation instructions](https://docs.docker.com/engine/install/centos/#install-using-the-repository),
choosing the new version you want to install.
>跟随[installation instructions](https://docs.docker.com/engine/install/centos/#install-using-the-repository) 选择你想要安装的新版本来更新Docker Engine。