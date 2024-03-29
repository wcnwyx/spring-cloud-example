本文翻译自docker官网：[https://docs.docker.com/engine/reference/builder/](https://docs.docker.com/engine/reference/builder/)

#Dockerfile reference

Docker can build images automatically by reading the instructions from a
`Dockerfile`. A `Dockerfile` is a text document that contains all the commands a
user could call on the command line to assemble an image. Using `docker build`
users can create an automated build that executes several command-line
instructions in succession.
> Docker可以通过读取`Dockerfile`文件中的指令来自动构建镜像。
> `Dockerfile`是一个文本文档，其中包含用户可以在命令行上调用以组装图像的所有命令。
> 使用`docker build`，用户可以创建一个连续执行多个命令行指令的自动构建。

This page describes the commands you can use in a `Dockerfile`. When you are
done reading this page, refer to the [`Dockerfile` Best
Practices](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/) for a tip-oriented guide.
> 本页介绍可在Dockerfile中使用的命令。阅读完本页后，
> 请参阅 [`Dockerfile` Best Practices](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/) 以获取面向提示的指南

## Usage

The [docker build](commandline/build.md) command builds an image from
a `Dockerfile` and a *context*. The build's context is the set of files at a
specified location `PATH` or `URL`. The `PATH` is a directory on your local
filesystem. The `URL` is a Git repository location.
> [docker build](https://github.com/docker/cli/blob/master/docs/reference/commandline/build.md) 指令从`Dockerfile`和上下文构建镜像。
> 构建的上下文是位于通过`PATH` or `URL`来指定位置的一组文件。
> `PATH`是本地文件系统上的一个目录。`URL`是Git存储库位置

A context is processed recursively. So, a `PATH` includes any subdirectories and
the `URL` includes the repository and its submodules. This example shows a
build command that uses the current directory as context:
> 上下文是递归处理的。所以路径包含任何子目录，URL包含存储库及其子模块。
> 此示例展示一个使用当前目录作为上下文的生成命令：

```bash
$ docker build .

Sending build context to Docker daemon  6.51 MB
...
```

The build is run by the Docker daemon, not by the CLI. The first thing a build
process does is send the entire context (recursively) to the daemon.  In most
cases, it's best to start with an empty directory as context and keep your
Dockerfile in that directory. Add only the files needed for building the
Dockerfile.
> 构建由Docker守护进程运行，而不是由CLI运行。
> 构建过程要做的第一件事就是将整个上下文（递归地）发送到守护进程。
> 在大多数情况下，最好从一个空目录开始作为上下文，并将Dockerfile保存在该目录中。
> 只添加构建Dockerfile所需的文件。

> **Warning**
>
> Do not use your root directory, `/`, as the `PATH` as it causes the build to
> transfer the entire contents of your hard drive to the Docker daemon.  
> 不要使用根目录`/`作为`PATH`，因为它会导致生成将硬盘驱动器的全部内容传输到Docker守护进程。

To use a file in the build context, the `Dockerfile` refers to the file specified
in an instruction, for example,  a `COPY` instruction. To increase the build's
performance, exclude files and directories by adding a `.dockerignore` file to
the context directory.  For information about how to [create a `.dockerignore`
file](#dockerignore-file) see the documentation on this page.
> 要在生成上下文中使用文件，Dockerfile引用指令中指定的文件，例如，`COPY`指令。
> 要提高生成的性能，请通过向上下文目录添加`.dockerginore`文件来排除文件和目录。
> 有关如何[创建 `.dockerignore`文件](#dockerignore-file) 的信息，请参阅本页上的文档。

Traditionally, the `Dockerfile` is called `Dockerfile` and located in the root
of the context. You use the `-f` flag with `docker build` to point to a Dockerfile
anywhere in your file system.
> 传统上，`Dockerfile`称为`Dockerfile`，位于上下文的根目录中。
> 您可以在`docker build`中使用`-f`标志来指向文件系统中任何位置的Dockerfile。

```bash
$ docker build -f /path/to/a/Dockerfile .
```

You can specify a repository and tag at which to save the new image if
the build succeeds:
> 如果构建成功，您可以指定保存新镜像的存储库和标记。

```bash
$ docker build -t shykes/myapp .
```

To tag the image into multiple repositories after the build,
add multiple `-t` parameters when you run the `build` command:
> 要在生成后将图像标记到多个存储库中，请在运行`build`命令时添加多个`-t`参数:

```bash
$ docker build -t shykes/myapp:1.0.2 -t shykes/myapp:latest .
```

Before the Docker daemon runs the instructions in the `Dockerfile`, it performs
a preliminary validation of the `Dockerfile` and returns an error if the syntax is incorrect:
> 在Docker守护进程运行`Dockerfile`中的指令之前，它将对`Dockerfile`执行初步验证，如果语法不正确，则返回错误

```bash
$ docker build -t test/myapp .

Sending build context to Docker daemon 2.048 kB
Error response from daemon: Unknown instruction: RUNCMD
```

The Docker daemon runs the instructions in the `Dockerfile` one-by-one,
committing the result of each instruction
to a new image if necessary, before finally outputting the ID of your
new image. The Docker daemon will automatically clean up the context you
sent.
> Docker守护进程逐个运行`Dockerfile`中的指令，如果需要，将每条指令的结果提交给新镜像，最后输出新镜像的ID。
> Docker守护进程将自动清理你发送的上下文。

Note that each instruction is run independently, and causes a new image
to be created - so `RUN cd /tmp` will not have any effect on the next
instructions.
> 请注意，每条指令都是独立运行的，并会创建一个新的镜像，所以`RUN cd/tmp`不会对下一个指令产生任何影响。

Whenever possible, Docker will re-use the intermediate images (cache),
to accelerate the `docker build` process significantly. This is indicated by
the `Using cache` message in the console output.
(For more information, see the [`Dockerfile` best practices guide](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/) :
> 只要有可能，Docker将重用中间镜像（cache），以显著加快`docker build`构建过程。
> 这由控制台输出中的“Using cache”消息表示。
> 更多的详情，请看  [`Dockerfile` best practices guide](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/)

```bash
$ docker build -t svendowideit/ambassador .

Sending build context to Docker daemon 15.36 kB
Step 1/4 : FROM alpine:3.2
 ---> 31f630c65071
Step 2/4 : MAINTAINER SvenDowideit@home.org.au
 ---> Using cache
 ---> 2a1c91448f5f
Step 3/4 : RUN apk update &&      apk add socat &&        rm -r /var/cache/
 ---> Using cache
 ---> 21ed6e7fbb73
Step 4/4 : CMD env | grep _TCP= | (sed 's/.*_PORT_\([0-9]*\)_TCP=tcp:\/\/\(.*\):\(.*\)/socat -t 100000000 TCP4-LISTEN:\1,fork,reuseaddr TCP4:\2:\3 \&/' && echo wait) | sh
 ---> Using cache
 ---> 7ea8aef582cc
Successfully built 7ea8aef582cc
```

Build cache is only used from images that have a local parent chain. This means
that these images were created by previous builds or the whole chain of images
was loaded with `docker load`. If you wish to use build cache of a specific
image you can specify it with `--cache-from` option. Images specified with
`--cache-from` do not need to have a parent chain and may be pulled from other
registries.
> 生成缓存仅用于具有本地父链的镜像。
> 这意味着这些镜像是由以前的构建创建的，或者整个镜像链是用“docker load”加载的。
> 如果要使用特定镜像的生成缓存，可以使用“-cache from”选项指定它。
> 用“-cache from”指定的镜像不需要父链，可以从其它registries中提取。

When you're done with your build, you're ready to look into [*Pushing a
repository to its registry*](https://docs.docker.com/engine/tutorials/dockerrepos/#/contributing-to-docker-hub).
> 当你完成了你的构建，你已经准备好研究[*Pushing a repository to its registry*](https://docs.docker.com/engine/tutorials/dockerrepos/#/contributing-to-docker-hub)


## BuildKit

Starting with version 18.09, Docker supports a new backend for executing your
builds that is provided by the [moby/buildkit](https://github.com/moby/buildkit)
project. The BuildKit backend provides many benefits compared to the old
implementation. For example, BuildKit can:
> 从 18.09 版本开始，Docker支持一个新的后端来执行你的构件，由 [moby/buildkit](https://github.com/moby/buildkit) 提供的项目。
> 与旧的实现相比，BuildKit后端提供了许多好处。 BuildKit 可以：

- Detect and skip executing unused build stages
  > 检测并跳过未使用的生成阶段
- Parallelize building independent build stages
  > 并行化独立构建阶段
- Incrementally transfer only the changed files in your build context between builds
  > 在构建之间增量地仅传输构建上下文中更改的文件
- Detect and skip transferring unused files in your build context
  > 检测并跳过在构建上下文中传输未使用的文件
- Use external Dockerfile implementations with many new features
  > 使用具有许多新功能的外部Dockerfile实现
- Avoid side-effects with rest of the API (intermediate images and containers)
  > 使用API的其余部分（中间图像和容器）避免副作用
- Prioritize your build cache for automatic pruning
  > 为自动修剪设置生成缓存的优先级

To use the BuildKit backend, you need to set an environment variable
`DOCKER_BUILDKIT=1` on the CLI before invoking `docker build`.
> 要使用BuildKit后端，需要在调用`docker build`之前, 在CLI上设置一个环境变量`DOCKER_BUILDKIT=1` 。

To learn about the experimental Dockerfile syntax available to BuildKit-based
builds [refer to the documentation in the BuildKit repository](https://github.com/moby/buildkit/blob/master/frontend/dockerfile/docs/experimental.md).

## Format

Here is the format of the `Dockerfile`:
> 这是 `Dockerfile` 的格式：

```dockerfile
# Comment
INSTRUCTION arguments
```

The instruction is not case-sensitive. However, convention is for them to
be UPPERCASE to distinguish them from arguments more easily.
> 指令不区分大小写。然而，惯例使用大写字母，以便更容易地将它们与参数区分开来。


Docker runs instructions in a `Dockerfile` in order. A `Dockerfile` **must
begin with a `FROM` instruction**. This may be after [parser
directives](#parser-directives), [comments](#format), and globally scoped
[ARGs](#arg). The `FROM` instruction specifies the [*Parent
Image*](https://docs.docker.com/glossary/#parent_image) from which you are
building. `FROM` may only be preceded by one or more `ARG` instructions, which
declare arguments that are used in `FROM` lines in the `Dockerfile`.
> Docker 顺序的执行`Dockerfile`中的指令。 一个`Dockerfile`必须以一个`FROM`指令开始。
> 这可能在解析器指令、注释和全局作用域参数之后。
> `FROM`指令指定要从哪个 [父镜像](https://docs.docker.com/glossary/#parent_image) 构建。
> `FROM'前面只能有一个或多个'ARG'指令，这些指令声明在'Dockerfile'的'FROM'行中使用的参数。

Docker treats lines that *begin* with `#` as a comment, unless the line is
a valid [parser directive](#parser-directives). A `#` marker anywhere
else in a line is treated as an argument. This allows statements like:
> Docker将以#开头的行视为注释，除非该行是有效的解析器指令。
> 行中其他任何位置的#标记都被视为参数。这允许这样的语句:

```dockerfile
# Comment
RUN echo 'we are running some # of cool things'
```

Comment lines are removed before the Dockerfile instructions are executed, which
means that the comment in the following example is not handled by the shell
executing the `echo` command, and both examples below are equivalent:
> 注释行在Dockerfile指令执行之前被删除，这意味着下面示例中的注释不是由执行echo命令的shell处理的，下面两个示例是等效的:

```dockerfile
RUN echo hello \
# comment
world
```

```dockerfile
RUN echo hello \
world
```

Line continuation characters are not supported in comments.
> 注释中不支持行连续字符。

> **Note on whitespace**
> **关于空格的注意**
>
> For backward compatibility, leading whitespace before comments (`#`) and
> instructions (such as `RUN`) are ignored, but discouraged. Leading whitespace
> is not preserved in these cases, and the following examples are therefore
> equivalent:
> 为了向后兼容，注释（`#`）和指令（如`RUN`）之前的空格将被忽略，但不鼓励这样做。
> 在这些情况下不保留最前面的空格，因此下面的示例是等效的：
>
> ```dockerfile
>         # this is a comment-line
>     RUN echo hello
> RUN echo world
> ```
>
> ```dockerfile
> # this is a comment-line
> RUN echo hello
> RUN echo world
> ```
>
> Note however, that whitespace in instruction _arguments_, such as the commands
> following `RUN`, are preserved, so the following example prints `    hello    world`
> with leading whitespace as specified:
> 但是请注意，指令 _参数_ 中的空格（例如`RUN`后面的命令）是保留的，因此下面的示例按指定的前导空格打印`    hello    world` :
>
> ```dockerfile
> RUN echo "\
>      hello\
>      world"
> ```

## Parser directives
> 分析器指令

Parser directives are optional, and affect the way in which subsequent lines
in a `Dockerfile` are handled. Parser directives do not add layers to the build,
and will not be shown as a build step. Parser directives are written as a
special type of comment in the form `# directive=value`. A single directive
may only be used once.
> 解析器指令是可选的，并影响Dockerfile中后续行的处理方式。
> 解析器指令不会将层添加到构建中，也不会显示为构建步骤。
> 解析器指令以`# directive=value`的形式作为特殊类型的注释编写。单个指令只能使用一次。

Once a comment, empty line or builder instruction has been processed, Docker
no longer looks for parser directives. Instead it treats anything formatted
as a parser directive as a comment and does not attempt to validate if it might
be a parser directive. Therefore, all parser directives must be at the very
top of a `Dockerfile`.
> 一旦处理完注释、空行或构建器指令后，Docker将不再查找解析器指令。
> 相反，它将任何格式化为解析器指令的内容视为注释，并且不尝试验证它是否可能是解析器指令。
>因此，所有解析器指令必须位于一个`Dockerfile`的最顶层。

Parser directives are not case-sensitive. However, convention is for them to
be lowercase. Convention is also to include a blank line following any
parser directives. Line continuation characters are not supported in parser
directives.
> 解析器指令不区分大小写。但是，约定是小写的。
> 约定还包括在任何解析器指令之后包含一个空行。解析器指令中不支持行连续字符。

Due to these rules, the following examples are all invalid:
> 根据这些规则，以下示例均无效：

Invalid due to line continuation:
> 由于行连续而无效：

```dockerfile
# direc \
tive=value
```

Invalid due to appearing twice:
> 因出现两次而无效：

```dockerfile
# directive=value1
# directive=value2

FROM ImageName
```

Treated as a comment due to appearing after a builder instruction:
> 由于出现在构建器指令之后而被视为注释：

```dockerfile
FROM ImageName
# directive=value
```

Treated as a comment due to appearing after a comment which is not a parser
directive:
> 由于出现在不是解析器指令的注释之后而被视为注释：

```dockerfile
# About my dockerfile
# directive=value
FROM ImageName
```

The unknown directive is treated as a comment due to not being recognized. In
addition, the known directive is treated as a comment due to appearing after
a comment which is not a parser directive.
> 由于未被识别，未知指令被视为注释。此外，由于出现在不是解析器指令的注释之后，因此已知指令被视为注释。

```dockerfile
# unknowndirective=value
# knowndirective=value
```

Non line-breaking whitespace is permitted in a parser directive. Hence, the
following lines are all treated identically:
> 解析器指令中允许使用非换行空格。因此，以下各行的处理方式相同：

```dockerfile
#directive=value
# directive =value
#	directive= value
# directive = value
#	  dIrEcTiVe=value
```

The following parser directives are supported:
> 支持以下解析器指令：

- `syntax`
- `escape`

## syntax

```dockerfile
# syntax=[remote image reference]
```

For example:

```dockerfile
# syntax=docker/dockerfile
# syntax=docker/dockerfile:1.0
# syntax=docker.io/docker/dockerfile:1
# syntax=docker/dockerfile:1.0.0-experimental
# syntax=example.com/user/repo:tag@sha256:abcdef...
```

This feature is only enabled if the [BuildKit](#buildkit) backend is used.
> 此功能仅在使用 [BuildKit](#buildkit) 后端时启用。

The syntax directive defines the location of the Dockerfile builder that is used for
building the current Dockerfile. The BuildKit backend allows to seamlessly use
external implementations of builders that are distributed as Docker images and
execute inside a container sandbox environment.
> syntax指令定义用于生成当前Dockerfile的Dockerfile生成器的位置。
> BuildKit后端允许无缝地使用作为Docker镜像分发并在容器沙盒环境中执行的构建器的外部实现。

Custom Dockerfile implementation allows you to:
> 自定义Dockerfile实现允许你：

- Automatically get bugfixes without updating the daemon
  > 在不更新守护进程的情况下自动获得错误修复
- Make sure all users are using the same implementation to build your Dockerfile
  > 确保所有用户都使用相同的实现来构建Dockerfile
- Use the latest features without updating the daemon
  > 使用最新功能而不更新守护程序
- Try out new experimental or third-party features
  > 尝试新的实验或第三方功能

### Official releases

Docker distributes official versions of the images that can be used for building
Dockerfiles under `docker/dockerfile` repository on Docker Hub. There are two
channels where new images are released: stable and experimental.
> Docker分发官方版本的镜像，这些镜像可用于在Docker Hub上的`docker/dockerfile`存储库下构建dockerfile。
> 发布新镜像有两个通道：稳定和实验。

Stable channel follows semantic versioning. For example:
> 稳定通道遵循语义版本控制。例如：

- `docker/dockerfile:1.0.0` - only allow immutable version `1.0.0`
- `docker/dockerfile:1.0` - allow versions `1.0.*`
- `docker/dockerfile:1` - allow versions `1.*.*`
- `docker/dockerfile:latest` - latest release on stable channel

The experimental channel uses incremental versioning with the major and minor
component from the stable channel on the time of the release. For example:
> 实验通道使用增量版本控制，主要和次要组件在发布时来自稳定通道。例如：

- `docker/dockerfile:1.0.1-experimental` - only allow immutable version `1.0.1-experimental`
- `docker/dockerfile:1.0-experimental` - latest experimental releases after `1.0`
- `docker/dockerfile:experimental` - latest release on experimental channel

You should choose a channel that best fits your needs. If you only want
bugfixes, you should use `docker/dockerfile:1.0`. If you want to benefit from
experimental features, you should use the experimental channel. If you are using
the experimental channel, newer releases may not be backwards compatible, so it
is recommended to use an immutable full version variant.
> 你应该选择一个最适合你需要的通道。如果您只想修复bug，应该使用`docker/dockerfile:1.0`。
> 如果您想从实验特性中获益，应该使用实验通道。
> 如果您使用的是实验频道，则较新的版本可能无法向后兼容，因此建议使用不可变的完整版本变体。

For master builds and nightly feature releases refer to the description in
[the source repository](https://github.com/moby/buildkit/blob/master/README.md).
> 有关主版本和夜间功能发布，请参阅[the source repository](https://github.com/moby/buildkit/blob/master/README.md) 。

## escape

```dockerfile
# escape=\ (backslash)
```

Or

```dockerfile
# escape=` (backtick)
```

The `escape` directive sets the character used to escape characters in a
`Dockerfile`. If not specified, the default escape character is `\`.
> `escape`指令用于设置Dockerfile中的转义字符。如果未指定，则默认转义字符为 `\`。

The escape character is used both to escape characters in a line, and to
escape a newline. This allows a `Dockerfile` instruction to
span multiple lines. Note that regardless of whether the `escape` parser
directive is included in a `Dockerfile`, *escaping is not performed in
a `RUN` command, except at the end of a line.*
> 转义字符既用于转义行中的字符，也用于转义换行。这允许一个`Dockerfile`指令跨越多行。
> 请注意，无论`escape`解析器指令是否包含在`Dockerfile`中，*转义都不会在`RUN`命令中执行，除非在行尾。*

Setting the escape character to `` ` `` is especially useful on
`Windows`, where `\` is the directory path separator. `` ` `` is consistent
with [Windows PowerShell](https://technet.microsoft.com/en-us/library/hh847755.aspx).
> 将转义字符设置为`` ` ``在Windows上特别有用，其中`\`是目录路径分隔符。

Consider the following example which would fail in a non-obvious way on
`Windows`. The second `\` at the end of the second line would be interpreted as an
escape for the newline, instead of a target of the escape from the first `\`.
Similarly, the `\` at the end of the third line would, assuming it was actually
handled as an instruction, cause it be treated as a line continuation. The result
of this dockerfile is that second and third lines are considered a single
instruction:
> 考虑下面的例子，它在`Windows`上以一种不明显的方式失败。
> 第二行末尾的第二个`\`将被解释为换行符的转义，而不是从第一个`\`转义的目标。
> 类似地，假设第三行末尾的`\`实际上是作为指令处理的，则会导致它被视为行的延续。
> 此dockerfile的结果是第二行和第三行被视为一条指令：

```dockerfile
FROM microsoft/nanoserver
COPY testfile.txt c:\\
RUN dir c:\
```

Results in:

```powershell
PS C:\John> docker build -t cmd .
Sending build context to Docker daemon 3.072 kB
Step 1/2 : FROM microsoft/nanoserver
 ---> 22738ff49c6d
Step 2/2 : COPY testfile.txt c:\RUN dir c:
GetFileAttributesEx c:RUN: The system cannot find the file specified.
PS C:\John>
```

One solution to the above would be to use `/` as the target of both the `COPY`
instruction, and `dir`. However, this syntax is, at best, confusing as it is not
natural for paths on `Windows`, and at worst, error prone as not all commands on
`Windows` support `/` as the path separator.
> 解决上述问题的一种方法是使用`/`作为`COPY`指令和`dir`的目标。
> 然而，这种语法充其量是令人困惑的，因为它对于 `Windows` 上的路径来说并不自然，
> 最坏的情况是，由于`Windows`上的命令并不都支持`/`作为路径分隔符，因此容易出错。

By adding the `escape` parser directive, the following `Dockerfile` succeeds as
expected with the use of natural platform semantics for file paths on `Windows`:
> 通过添加`escape` 解析器指令，以下`Dockerfile`通过对`Windows`上的文件路径使用自然平台语义，如期成功：

```dockerfile
# escape=`

FROM microsoft/nanoserver
COPY testfile.txt c:\
RUN dir c:\
```

Results in:

```powershell
PS C:\John> docker build -t succeeds --no-cache=true .
Sending build context to Docker daemon 3.072 kB
Step 1/3 : FROM microsoft/nanoserver
 ---> 22738ff49c6d
Step 2/3 : COPY testfile.txt c:\
 ---> 96655de338de
Removing intermediate container 4db9acbb1682
Step 3/3 : RUN dir c:\
 ---> Running in a2c157f842f5
 Volume in drive C has no label.
 Volume Serial Number is 7E6D-E0F7

 Directory of c:\

10/05/2016  05:04 PM             1,894 License.txt
10/05/2016  02:22 PM    <DIR>          Program Files
10/05/2016  02:14 PM    <DIR>          Program Files (x86)
10/28/2016  11:18 AM                62 testfile.txt
10/28/2016  11:20 AM    <DIR>          Users
10/28/2016  11:20 AM    <DIR>          Windows
           2 File(s)          1,956 bytes
           4 Dir(s)  21,259,096,064 bytes free
 ---> 01c7f3bef04f
Removing intermediate container a2c157f842f5
Successfully built 01c7f3bef04f
PS C:\John>
```

## Environment replacement
> 环境替换

Environment variables (declared with [the `ENV` statement](#env)) can also be
used in certain instructions as variables to be interpreted by the
`Dockerfile`. Escapes are also handled for including variable-like syntax
into a statement literally.
> 环境变量（用`ENV`语句声明）也可以在某些指令中用作`Dockerfile`要解释的变量。
> 转义也被处理为在语句中包含类似变量的语法。

Environment variables are notated in the `Dockerfile` either with
`$variable_name` or `${variable_name}`. They are treated equivalently and the
brace syntax is typically used to address issues with variable names with no
whitespace, like `${foo}_bar`.
> 环境变量在Dockerfile中用`$variable_name`或`${variable_name}`表示。
> 它们被等价地对待，大括号语法通常用于解决没有空格的变量名的问题，比如`${foo}_bar`。

The `${variable_name}` syntax also supports a few of the standard `bash`
modifiers as specified below:
> `${variable_name}`语法还支持下面指定的一些标准bash修饰符:

- `${variable:-word}` indicates that if `variable` is set then the result
  will be that value. If `variable` is not set then `word` will be the result.
  > `${variable:-word}` 表示如果设置了`variable`，则结果将是该值。如果未设置`variable`，则结果将为word。
- `${variable:+word}` indicates that if `variable` is set then `word` will be
  the result, otherwise the result is the empty string.
  > `${variable:+word}` 表示如果设置了`variable`，则结果是`word`，否则结果是空字符串。

In all cases, `word` can be any string, including additional environment
variables.
> 在所有情况下，`word`可以是任何字符串，包括附加的环境变量。

Escaping is possible by adding a `\` before the variable: `\$foo` or `\${foo}`,
for example, will translate to `$foo` and `${foo}` literals respectively.
> 可以通过在变量前面添加`\`来进行转义：例如，`\$foo`或`\${foo}`将分别转换为`$foo`和`${foo}`文本。

Example (parsed representation is displayed after the `#`):
> 示例（解析的表示形式显示在#之后）：

```dockerfile
FROM busybox
ENV FOO=/bar
WORKDIR ${FOO}   # WORKDIR /bar
ADD . $FOO       # ADD . /bar
COPY \$FOO /quux # COPY $FOO /quux
```

Environment variables are supported by the following list of instructions in
the `Dockerfile`:
> `Dockerfile`中的以下指令列表支持环境变量：

- `ADD`
- `COPY`
- `ENV`
- `EXPOSE`
- `FROM`
- `LABEL`
- `STOPSIGNAL`
- `USER`
- `VOLUME`
- `WORKDIR`
- `ONBUILD` (when combined with one of the supported instructions above)

Environment variable substitution will use the same value for each variable
throughout the entire instruction. In other words, in this example:
> 环境变量替换将在整个指令中对每个变量使用相同的值。换句话说，在这个例子中：

```dockerfile
ENV abc=hello
ENV abc=bye def=$abc
ENV ghi=$abc
```

will result in `def` having a value of `hello`, not `bye`. However,
`ghi` will have a value of `bye` because it is not part of the same instruction
that set `abc` to `bye`.
> 结果将是 `def`的值为`hello`，而不是`bye`。
> 然而， `ghi`的值为`bye`， 因为它不是 将`abc`设置为`bye` 的指令的一部分。

## .dockerignore file

Before the docker CLI sends the context to the docker daemon, it looks
for a file named `.dockerignore` in the root directory of the context.
If this file exists, the CLI modifies the context to exclude files and
directories that match patterns in it.  This helps to avoid
unnecessarily sending large or sensitive files and directories to the
daemon and potentially adding them to images using `ADD` or `COPY`.
> 在docker CLI将上下文发送到docker守护进程之前，它会在上下文的根目录中查找名为`.dockerignore`的文件。
> 果此文件存在，CLI将修改上下文以排除与其中模式匹配的文件和目录。
> 这有助于避免不必要地将大型或敏感文件和目录发送到守护进程，并可能使用`ADD`或`COPY`将它们添加到图像中。

The CLI interprets the `.dockerignore` file as a newline-separated
list of patterns similar to the file globs of Unix shells.  For the
purposes of matching, the root of the context is considered to be both
the working and the root directory.  For example, the patterns
`/foo/bar` and `foo/bar` both exclude a file or directory named `bar`
in the `foo` subdirectory of `PATH` or in the root of the git
repository located at `URL`.  Neither excludes anything else.
> CLI将`.dockerignore`文件解释为一个新行分隔的模式列表，类似于Unix shells的文件globs。
> 为了匹配，上下文的根被认为是工作目录和根目录。
> 例如，模式`/foo/bar`和`foo/bar`都排除`PATH`的`foo子`目录或位于`URL`的git存储库根目录中名为`bar`的文件或目录。
> 也不排除任何其他因素。

If a line in `.dockerignore` file starts with `#` in column 1, then this line is
considered as a comment and is ignored before interpreted by the CLI.
> 如果`.dockerignore`文件中的一行在第1列中以`#`开头，则该行将被视为注释，在CLI解释之前将被忽略。

Here is an example `.dockerignore` file:
> 这是`.dockerignore`文件的一个例子：

```gitignore
# comment
*/temp*
*/*/temp*
temp?
```

This file causes the following build behavior:
> 此文件导致以下构建行为：

| Rule        | Behavior                                                                                                                                                                                                       |
|:------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `# comment` | Ignored.                                                                                                                                                                                                       |
| `*/temp*`   | Exclude files and directories whose names start with `temp` in any immediate subdirectory of the root.  For example, the plain file `/somedir/temporary.txt` is excluded, as is the directory `/somedir/temp`. |
| `*/*/temp*` | Exclude files and directories starting with `temp` from any subdirectory that is two levels below the root. For example, `/somedir/subdir/temporary.txt` is excluded.                                          |
| `temp?`     | Exclude files and directories in the root directory whose names are a one-character extension of `temp`.  For example, `/tempa` and `/tempb` are excluded.                                                     |
> `# comment` : 忽略  
> `*/temp*`   ：排除根目录的任何直接子目录中名称以`temp`开头的文件和目录。例如，普通文件`/somedir/temporary.txt`与目录`/somedir/temp`一样被排除。  
> `*/*/temp*` : 从根目录下两级的任何子目录中排除以temp开头的文件和目录。例如，`/somedir/subdir/temporary.txt` 被排除。  
> `temp?`     ：排除根目录中名称为`temp`的单字符扩展名的文件和目录。例如，`/tempa`和`/tempb`被排除。

Matching is done using Go's
[filepath.Match](http://golang.org/pkg/path/filepath#Match) rules.  A
preprocessing step removes leading and trailing whitespace and
eliminates `.` and `..` elements using Go's
[filepath.Clean](http://golang.org/pkg/path/filepath/#Clean).  Lines
that are blank after preprocessing are ignored.
> 匹配是使用Go的[filepath.Match](http://golang.org/pkg/path/filepath#Match) 规则进行。
> 预处理步骤删除前导和尾随空格并使用Go的[filepath.Clean](http://golang.org/pkg/path/filepath/#Clean) 消除`.`还有`..`。 
> 预处理后为空的行将被忽略。

Beyond Go's filepath.Match rules, Docker also supports a special
wildcard string `**` that matches any number of directories (including
zero). For example, `**/*.go` will exclude all files that end with `.go`
that are found in all directories, including the root of the build context.
> 超越Go的 filepath.Match 规则，Docker还支持一个特殊的通配符字符串`**`，它匹配任意数量的目录（包括零）。
> 例如， `**/*.go`将排除所有以`.go`结尾的文件，这些文件位于所有目录中，包括构建上下文的根目录中。

Lines starting with `!` (exclamation mark) can be used to make exceptions
to exclusions.  The following is an example `.dockerignore` file that
uses this mechanism:
> 以`!`（感叹号）开头的行可用于排除例外。
> 以下是使用此机制的`.dockerignore`文件的示例：

```gitignore
*.md
!README.md
```

All markdown files *except* `README.md` are excluded from the context.
> 所有的 markdown文件 *除了* `README.md` 都将从上下文中排除。

The placement of `!` exception rules influences the behavior: the last
line of the `.dockerignore` that matches a particular file determines
whether it is included or excluded.  Consider the following example:
> `!`的位置影响行为的例外规则: 
> 与特定文件匹配的`.dockerignore`的最后一行决定是否包含或排除该文件。
> 考虑下面的例子：

```gitignore
*.md
!README*.md
README-secret.md
```

No markdown files are included in the context except README files other than
`README-secret.md`.
> 没有 markdown 文件会被包含到上下文中，除了README 文件，但是README-secret.md也会被排除掉。

Now consider this example:

```gitignore
*.md
README-secret.md
!README*.md
```

All of the README files are included.  The middle line has no effect because
`!README*.md` matches `README-secret.md` and comes last.
> 所有的 README 文件是包含的。 中间哪一行是不起作用的，因为`!README*.md`是最后一行并且匹配得上`README-secret.md`。

You can even use the `.dockerignore` file to exclude the `Dockerfile`
and `.dockerignore` files.  These files are still sent to the daemon
because it needs them to do its job.  But the `ADD` and `COPY` instructions
do not copy them to the image.
> 你甚至可以使用`.dockerignore`文件排除`Dockerfile`和`.dockerignore`文件。
> 这些文件仍然被发送到守护进程，因为它（守护进程）需要它们来完成它（守护进程）的工作。

Finally, you may want to specify which files to include in the
context, rather than which to exclude. To achieve this, specify `*` as
the first pattern, followed by one or more `!` exception patterns.
> 最后，您可能希望指定上下文中要包含哪些文件，而不是要排除哪些文件。
> 为此，请指定`*`作为第一个模式，后跟一个或多个！异常模式。

> **Note**
>
> For historical reasons, the pattern `.` is ignored.
> 由于历史原因，`.`模式是被忽略的。

## FROM

```dockerfile
FROM [--platform=<platform>] <image> [AS <name>]
```

Or

```dockerfile
FROM [--platform=<platform>] <image>[:<tag>] [AS <name>]
```

Or

```dockerfile
FROM [--platform=<platform>] <image>[@<digest>] [AS <name>]
```

The `FROM` instruction initializes a new build stage and sets the
[*Base Image*](https://docs.docker.com/glossary/#base_image) for subsequent instructions. As such, a
valid `Dockerfile` must start with a `FROM` instruction. The image can be
any valid image – it is especially easy to start by **pulling an image** from
the [*Public Repositories*](https://docs.docker.com/docker-hub/repos/).
> `FROM`指令初始化新的构建阶段，并为后续指令设置[*基础镜像*](https://docs.docker.com/glossary/#base_image) 。
> 因此，有效的`Dockerfile`必须以`FROM`指令开头。
> 镜像可以是任何有效的镜像–从[*公共存储库*](https://docs.docker.com/docker-hub/repos/) 中提取镜像尤其容易。

- `ARG` is the only instruction that may precede `FROM` in the `Dockerfile`.
  See [Understand how ARG and FROM interact](#understand-how-arg-and-from-interact).
  > `ARG`是`Dockerfile`中唯一可以在`FROM`前面的指令。
- `FROM` can appear multiple times within a single `Dockerfile` to
  create multiple images or use one build stage as a dependency for another.
  Simply make a note of the last image ID output by the commit before each new
  `FROM` instruction. Each `FROM` instruction clears any state created by previous
  instructions.
  > `FROM`可以在一个`Dockerfile`中多次出现，以创建多个镜像或将一个构建阶段用作另一个构建阶段的依赖项。
  > 只需在每个新的`FROM`指令之前记下提交输出的最后一个图像ID。
  > 每条FROM指令都会清除以前的指令所创建的任何状态。
- Optionally a name can be given to a new build stage by adding `AS name` to the
  `FROM` instruction. The name can be used in subsequent `FROM` and
  `COPY --from=<name>` instructions to refer to the image built in this stage.
  > 也可以通过将`AS name`添加到`FROM`指令中，为新的构建阶段指定一个名称。
  > 该名称可以在后续的`FROM`和`COPY --from=<name>`指令中使用，以引用在此阶段中构建的镜像。
- The `tag` or `digest` values are optional. If you omit either of them, the
  builder assumes a `latest` tag by default. The builder returns an error if it
  cannot find the `tag` value.
  > `tag`或`digest`是可选的。如果忽略其中一个，则生成器默认采用`latest`标记。如果生成器找不到`tag`值，则返回一个错误。

The optional `--platform` flag can be used to specify the platform of the image
in case `FROM` references a multi-platform image. For example, `linux/amd64`,
`linux/arm64`, or `windows/amd64`. By default, the target platform of the build
request is used. Global build arguments can be used in the value of this flag,
for example [automatic platform ARGs](#automatic-platform-args-in-the-global-scope)
allow you to force a stage to native build platform (`--platform=$BUILDPLATFORM`),
and use it to cross-compile to the target platform inside the stage.
> 可选的`--platform`标志可用于在 `FROM` 引用多平台镜像时指定镜像的平台。
> 例如：`linux/amd64`, `linux/arm64`, or `windows/amd64`。默认情况下，使用构建请求的目标平台。
> 全局构建参数可用于此标志的值中，例如，自动平台参数（automatic platform ARGs）允许您强制一个阶段到本机生成平台（`--platform=$BUILDPLATFORM`），
> 并使用它交叉编译到阶段内的目标平台。

### Understand how ARG and FROM interact
> 了解ARG和FROM是如何相互作用的

`FROM` instructions support variables that are declared by any `ARG`
instructions that occur before the first `FROM`.
> `FROM`指令支持由在第一个`FROM`之前出现的任何`ARG`指令声明的变量。

```dockerfile
ARG  CODE_VERSION=latest
FROM base:${CODE_VERSION}
CMD  /code/run-app

FROM extras:${CODE_VERSION}
CMD  /code/run-extras
```

An `ARG` declared before a `FROM` is outside of a build stage, so it
can't be used in any instruction after a `FROM`. To use the default value of
an `ARG` declared before the first `FROM` use an `ARG` instruction without
a value inside of a build stage:
> 在`FROM`之前声明的`ARG`在生成阶段之外，因此不能在`FROM`之后的任何指令中使用。
> 要使用在第一个`FROM`之前声明的`ARG`的默认值，请在生成阶段中使用没有值的ARG指令：

```dockerfile
ARG VERSION=latest
FROM busybox:$VERSION
ARG VERSION
RUN echo $VERSION > image_version
```

## RUN

RUN has 2 forms:
> RUN 有两种形式：

- `RUN <command>` (*shell* form, the command is run in a shell, which by
  default is `/bin/sh -c` on Linux or `cmd /S /C` on Windows)
  > `RUN <command>` (*shell* 形式， 命令在shell中运行，在Linux上默认为`/bin/sh -c`，在Windows上默认为`cmd /S /C`)
- `RUN ["executable", "param1", "param2"]` (*exec* form)

The `RUN` instruction will execute any commands in a new layer on top of the
current image and commit the results. The resulting committed image will be
used for the next step in the `Dockerfile`.
> `RUN`指令将在当前镜像上的新层中执行任何命令，并提交结果。提交的镜像将用于Dockerfile中的下一步。

Layering `RUN` instructions and generating commits conforms to the core
concepts of Docker where commits are cheap and containers can be created from
any point in an image's history, much like source control.
> 分层`RUN`指令和生成提交符合Docker的核心概念，在Docker中提交很廉价，可以从镜像历史中的任何点创建容器，很像源代码管理。

The *exec* form makes it possible to avoid shell string munging, and to `RUN`
commands using a base image that does not contain the specified shell executable.
> *exec*形式可以避免shell字符串munging ？？？，并使用不包含指定shell可执行文件的基础镜像`RUN`指令。

The default shell for the *shell* form can be changed using the `SHELL`
command.
> 可以使用`SHELL`指令更改*shell*形式的默认shell。

In the *shell* form you can use a `\` (backslash) to continue a single
RUN instruction onto the next line. For example, consider these two lines:
> 在*shell*形式中，可以使用\（反斜杠）将单个运行指令继续执行到下一行。例如，考虑以下两行：

```dockerfile
RUN /bin/bash -c 'source $HOME/.bashrc; \
echo $HOME'
```
Together they are equivalent to this single line:
> 它们一起相当于这一行：

```dockerfile
RUN /bin/bash -c 'source $HOME/.bashrc; echo $HOME'
```

To use a different shell, other than '/bin/sh', use the *exec* form passing in
the desired shell. For example:
> 要使用不同的shell，而不是'/bin/sh'，使用*exec*形式并传入所需的shell。例如：

```dockerfile
RUN ["/bin/bash", "-c", "echo hello"]
```

> **Note**
>
> The *exec* form is parsed as a JSON array, which means that
> you must use double-quotes (") around words not single-quotes (').
> *exec*形式被解析为JSON数组，这意味着您必须在单词周围使用双引号（"），而不是单引号（'）。

Unlike the *shell* form, the *exec* form does not invoke a command shell.
This means that normal shell processing does not happen. For example,
`RUN [ "echo", "$HOME" ]` will not do variable substitution on `$HOME`.
If you want shell processing then either use the *shell* form or execute
a shell directly, for example: `RUN [ "sh", "-c", "echo $HOME" ]`.
When using the exec form and executing a shell directly, as in the case for
the shell form, it is the shell that is doing the environment variable
expansion, not docker.
> 与*shell* 形式不同，*exec*形式不调用shell指令。这意味着正常的shell处理不会发生。
> 例如，`RUN [ "echo", "$HOME" ]`不会对`$HOME`执行变量替换。
> 如果需要shell处理，那么可以使用shell形式，也可以直接执行shell，例如：`RUN [ "sh", "-c", "echo $HOME" ]`。
> 当使用exec形式并直接执行shell时，就像shell形式的情况一样，执行环境变量扩展的是shell，而不是docker。

> **Note**
>
> In the *JSON* form, it is necessary to escape backslashes. This is
> particularly relevant on Windows where the backslash is the path separator.
> The following line would otherwise be treated as *shell* form due to not
> being valid JSON, and fail in an unexpected way:
> 在JSON格式中，有必要转义反斜杠。
> 这在反斜杠作为路径分隔符的Windows中尤其重要。否则，由于不是有效的JSON，以下行将被视为*shell*形式，并以意外的方式失败：
>
> ```dockerfile
> RUN ["c:\windows\system32\tasklist.exe"]
> ```
>
> The correct syntax for this example is:
> 此示例的正确语法为：
>
> ```dockerfile
> RUN ["c:\\windows\\system32\\tasklist.exe"]
> ```

The cache for `RUN` instructions isn't invalidated automatically during
the next build. The cache for an instruction like
`RUN apt-get dist-upgrade -y` will be reused during the next build. The
cache for `RUN` instructions can be invalidated by using the `--no-cache`
flag, for example `docker build --no-cache`.
> `RUN`指令的缓存在下一次生成期间不会自动失效。
> `RUN apt-get dist-upgrade -y`等指令的缓存将在下一次生成过程中重用。
> `RUN`指令的缓存可以通过使用`--no-cache`标志来失效，例如`docker build --no-cache`。

See the [`Dockerfile` Best Practices
guide](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/) for more information.
> 查看[`Dockerfile` Best Practices guide](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/) 获取更多的信息。

The cache for `RUN` instructions can be invalidated by [`ADD`](#add) and [`COPY`](#copy) instructions.
> `RUN`指令的缓存可以由[`ADD`](#add) 和[`COPY`](#copy) 指令失效。

### Known issues (RUN)

- [Issue 783](https://github.com/docker/docker/issues/783) is about file
  permissions problems that can occur when using the AUFS file system. You
  might notice it during an attempt to `rm` a file, for example.
  > 问题783是关于使用AUFS文件系统时可能出现的文件权限问题。例如，在尝试`rm`文件时，您可能会注意到它。

  For systems that have recent aufs version (i.e., `dirperm1` mount option can
  be set), docker will attempt to fix the issue automatically by mounting
  the layers with `dirperm1` option. More details on `dirperm1` option can be
  found at [`aufs` man page](https://github.com/sfjro/aufs3-linux/tree/aufs3.18/Documentation/filesystems/aufs)
  > 对于具有最新aufs版本的系统（即可以设置`dirperm1`挂载选项），docker将尝试通过使用`dirperm1`选项挂载层来自动修复该问题。
  > 有关`dirperm1`选项的更多详细信息，请参见[`aufs` man page](https://github.com/sfjro/aufs3-linux/tree/aufs3.18/Documentation/filesystems/aufs) 

  If your system doesn't have support for `dirperm1`, the issue describes a workaround.
  > 如果您的系统不支持`dirperm1`，则问题描述了一种解决方法。

## CMD

The `CMD` instruction has three forms:
> `CMD`指令有三种形式：

- `CMD ["executable","param1","param2"]` (*exec* form, this is the preferred form) 首选项
- `CMD ["param1","param2"]` (as *default parameters to ENTRYPOINT*) 
- `CMD command param1 param2` (*shell* form)

There can only be one `CMD` instruction in a `Dockerfile`. If you list more than one `CMD`
then only the last `CMD` will take effect.
> 一个`Dockerfile`中只能有一条`CMD`指令。如果列出多个`CMD`，则只有最后一个`CMD`生效。

**The main purpose of a `CMD` is to provide defaults for an executing
container.** These defaults can include an executable, or they can omit
the executable, in which case you must specify an `ENTRYPOINT`
instruction as well.
> **`CMD`的主要目的是为正在执行的容器提供默认值。**
> 这些默认值可以包括可执行文件，也可以忽略可执行文件，在这种情况下，还必须指定`ENTRYPOINT`(入口点)指令。

If `CMD` is used to provide default arguments for the `ENTRYPOINT` instruction,
both the `CMD` and `ENTRYPOINT` instructions should be specified with the JSON
array format.
> 如果使用`CMD`为`ENTRYPOINT`指令提供默认参数，则`CMD`为`ENTRYPOINT`指令都应使用JSON数组格式指定。

> **Note**
>
> The *exec* form is parsed as a JSON array, which means that you must use
> double-quotes (") around words not single-quotes (').
> *exec*形式被解析为JSON数组，这意味着您必须在单词周围使用双引号（“），而不是单引号（“）。

Unlike the *shell* form, the *exec* form does not invoke a command shell.
This means that normal shell processing does not happen. For example,
`CMD [ "echo", "$HOME" ]` will not do variable substitution on `$HOME`.
If you want shell processing then either use the *shell* form or execute
a shell directly, for example: `CMD [ "sh", "-c", "echo $HOME" ]`.
When using the exec form and executing a shell directly, as in the case for
the shell form, it is the shell that is doing the environment variable
expansion, not docker.
> 与*shell* 形式不同，*exec*形式不调用shell指令。这意味着正常的shell处理不会发生。
> 例如，`CMD [ "echo", "$HOME" ]`不会对`$HOME`执行变量替换。
> 如果需要shell处理，那么可以使用shell形式，也可以直接执行shell，例如：`CMD [ "sh", "-c", "echo $HOME" ]`。
> 当使用exec形式并直接执行shell时，就像shell形式的情况一样，执行环境变量扩展的是shell，而不是docker。

When used in the shell or exec formats, the `CMD` instruction sets the command
to be executed when running the image.
> 在shell或exec格式中使用时，`CMD`指令设置运行镜像时要执行的命令。

If you use the *shell* form of the `CMD`, then the `<command>` will execute in
`/bin/sh -c`:
> 如果使用`CMD`的*shell*形式，则`<command>`将在`/bin/sh -c`中执行：

```dockerfile
FROM ubuntu
CMD echo "This is a test." | wc -
```

If you want to **run your** `<command>` **without a shell** then you must
express the command as a JSON array and give the full path to the executable.
**This array form is the preferred format of `CMD`.** Any additional parameters
must be individually expressed as strings in the array:
> 如果要在**不使用shell**的情况下**运行**`<command>`，则必须将指令表示为JSON数组，并给出可执行文件的完整路径。
> **此数组形式是CMD的首选格式。** **任何附加参数都必须单独表示为数组中的字符串。**

```dockerfile
FROM ubuntu
CMD ["/usr/bin/wc","--help"]
```

If you would like your container to run the same executable every time, then
you should consider using `ENTRYPOINT` in combination with `CMD`. See
[*ENTRYPOINT*](#entrypoint).
> 如果希望容器每次都运行相同的可执行文件，那么应该考虑将`ENTRYPOINT`与`CMD`结合使用。

If the user specifies arguments to `docker run` then they will override the
default specified in `CMD`.
> 如果用户指定`docker run`的参数，那么它们将覆盖`CMD`中指定的默认值。

> **Note**
>
> Do not confuse `RUN` with `CMD`. `RUN` actually runs a command and commits
> the result; `CMD` does not execute anything at build time, but specifies
> the intended command for the image.  
> 不要混淆`RUN`和`CMD`。`RUN`实际运行一个命令并提交结果；`CMD`在构建时不执行任何操作，而是为镜像指定预期的命令。

## LABEL

```dockerfile
LABEL <key>=<value> <key>=<value> <key>=<value> ...
```

The `LABEL` instruction adds metadata to an image. A `LABEL` is a
key-value pair. To include spaces within a `LABEL` value, use quotes and
backslashes as you would in command-line parsing. A few usage examples:
> `LABEL`指令将元数据添加到镜像中。`LABEL`是键值对。
> 要在`LABEL`值中包含空格，请像在命令行分析中一样使用引号和反斜杠。一些用法示例：

```dockerfile
LABEL "com.example.vendor"="ACME Incorporated"
LABEL com.example.label-with-value="foo"
LABEL version="1.0"
LABEL description="This text illustrates \
that label-values can span multiple lines."
```

An image can have more than one label. You can specify multiple labels on a
single line. Prior to Docker 1.10, this decreased the size of the final image,
but this is no longer the case. You may still choose to specify multiple labels
in a single instruction, in one of the following two ways:
> 一个镜像可以有多个标签。可以在一行上指定多个标签。
> 在Docker1.10之前，这减小了最终镜像的大小，但现在不再是这样了。
> 你仍然可以选择通过以下两种方式之一在一条指令中指定多个标签：

```dockerfile
LABEL multi.label1="value1" multi.label2="value2" other="value3"
```

```dockerfile
LABEL multi.label1="value1" \
      multi.label2="value2" \
      other="value3"
```

Labels included in base or parent images (images in the `FROM` line) are
inherited by your image. If a label already exists but with a different value,
the most-recently-applied value overrides any previously-set value.
> 基本镜像或父镜像（`FROM`行中的镜像）中包含的标签由你的镜像继承。
> 如果标签已存在但具有不同的值，则最近应用的值将覆盖任何先前设置的值。

To view an image's labels, use the `docker image inspect` command. You can use
the `--format` option to show just the labels;
> 要查看镜像的标签，请使用`docker image inspect`指令。你可以使用`--format`选项只显示标签；

```bash
docker image inspect --format='{{json .Config.Labels}}' myimage
```
```json
{
  "com.example.vendor": "ACME Incorporated",
  "com.example.label-with-value": "foo",
  "version": "1.0",
  "description": "This text illustrates that label-values can span multiple lines.",
  "multi.label1": "value1",
  "multi.label2": "value2",
  "other": "value3"
}
```

## MAINTAINER (deprecated)
> 维护者（已弃用）

```dockerfile
MAINTAINER <name>
```

The `MAINTAINER` instruction sets the *Author* field of the generated images.
The `LABEL` instruction is a much more flexible version of this and you should use
it instead, as it enables setting any metadata you require, and can be viewed
easily, for example with `docker inspect`. To set a label corresponding to the
`MAINTAINER` field you could use:
> `MAINTAINER`指令设置生成镜像的*Author*字段。
> `LABEL`指令是一个更灵活的版本，您应该改用它，因为它允许设置您需要的任何元数据，并且可以很容易地查看，
> 例如使用`docker inspect`。要设置与`MAINTAINER`字段相对应的标签，可以使用：

```dockerfile
LABEL maintainer="SvenDowideit@home.org.au"
```

This will then be visible from `docker inspect` with the other labels.
> 这将从`docker inspect`中与其他标签一样可见。

## EXPOSE

```dockerfile
EXPOSE <port> [<port>/<protocol>...]
```

The `EXPOSE` instruction informs Docker that the container listens on the
specified network ports at runtime. You can specify whether the port listens on
TCP or UDP, and the default is TCP if the protocol is not specified.
> `EXPOSE`指令通知Docker容器在运行时侦听指定的网络端口。
> 您可以指定端口是侦听TCP还是UDP，如果未指定协议，则默认为TCP。

The `EXPOSE` instruction does not actually publish the port. It functions as a
type of documentation between the person who builds the image and the person who
runs the container, about which ports are intended to be published. To actually
publish the port when running the container, use the `-p` flag on `docker run`
to publish and map one or more ports, or the `-P` flag to publish all exposed
ports and map them to high-order ports.
> EXPOSE指令实际上并不发布端口。它是构建镜像的人员和运行容器的人员之间的一种文档类型，计划要发布的端口。
> 要在运行容器时实际发布端口，请在`docker run`上使用`-p`标志发布和映射一个或多个端口，
> 或者使用`-P`标志发布所有公开的端口并将它们映射到高阶端口。

By default, `EXPOSE` assumes TCP. You can also specify UDP:
> 默认情况下，`EXPOSE`采用TCP。也可以指定UDP：

```dockerfile
EXPOSE 80/udp
```

To expose on both TCP and UDP, include two lines:
> 要在TCP和UDP上都公开，请包括两行：

```dockerfile
EXPOSE 80/tcp
EXPOSE 80/udp
```

In this case, if you use `-P` with `docker run`, the port will be exposed once
for TCP and once for UDP. Remember that `-P` uses an ephemeral high-ordered host
port on the host, so the port will not be the same for TCP and UDP.
> 在这种情况下，如果将`-P`与`docker run`一起使用，则端口将为TCP和UDP分别公开一次。
> 请记住，`-P`在主机上使用临时的高阶主机端口，因此TCP和UDP的端口将不同。

Regardless of the `EXPOSE` settings, you can override them at runtime by using
the `-p` flag. For example
> 不管`EXPOSE`设置如何，都可以在运行时使用-p标志覆盖它们。例如：

```bash
docker run -p 80:80/tcp -p 80:80/udp ...
```

To set up port redirection on the host system, see [using the -P flag](run.md#expose-incoming-ports).
The `docker network` command supports creating networks for communication among
containers without the need to expose or publish specific ports, because the
containers connected to the network can communicate with each other over any
port. For detailed information, see the
[overview of this feature](https://docs.docker.com/engine/userguide/networking/).
> 要在主机系统上设置端口重定向，请参阅[using the -P flag](https://docs.docker.com/engine/reference/run/#expose-incoming-ports) 。
> docker network命令支持创建用于容器间通信的网络，而无需公开或发布特定端口，因为连接到网络的容器可以通过任何端口相互通信。
> 有关详细信息，请参阅[overview of this feature](https://docs.docker.com/engine/userguide/networking/) 。

## ENV

```dockerfile
ENV <key>=<value> ...
```

The `ENV` instruction sets the environment variable `<key>` to the value
`<value>`. This value will be in the environment for all subsequent instructions
in the build stage and can be [replaced inline](#environment-replacement) in
many as well. The value will be interpreted for other environment variables, so
quote characters will be removed if they are not escaped. Like command line parsing,
quotes and backslashes can be used to include spaces within values.
> `ENV`指令将环境变量`<key>`设置为值`<value>`。
> 该值将在构建阶段的所有后续指令的环境中，并且可以在许多环境中内联替换。
> 该值将被解释为其他环境变量，因此引号字符将被删除，如果他们没有转义。
> 与命令行分析一样，引号和反斜杠可以用于在值中包含空格。

Example:

```dockerfile
ENV MY_NAME="John Doe"
ENV MY_DOG=Rex\ The\ Dog
ENV MY_CAT=fluffy
```

The `ENV` instruction allows for multiple `<key>=<value> ...` variables to be set
at one time, and the example below will yield the same net results in the final
image:
> `ENV`指令允许多个`<key>=<value> ...`一次设置变量，下面的示例将在最终镜像中产生相同的净结果：

```dockerfile
ENV MY_NAME="John Doe" MY_DOG=Rex\ The\ Dog \
    MY_CAT=fluffy
```

The environment variables set using `ENV` will persist when a container is run
from the resulting image. You can view the values using `docker inspect`, and
change them using `docker run --env <key>=<value>`.
> 当容器从结果镜像运行时，使用`ENV`设置的环境变量将保持不变。
> 您可以使用`docker inspect`查看值，并使用`docker run --env <key>=<value>`更改它们。

Environment variable persistence can cause unexpected side effects. For example,
setting `ENV DEBIAN_FRONTEND=noninteractive` changes the behavior of `apt-get`,
and may confuse users of your image.
> 环境变量持久性可能会导致意外的副作用。
> 例如，设置`ENV DEBIAN_FRONTEND=noninteractive`会更改apt-get的行为，并可能会混淆图像的用户。

If an environment variable is only needed during build, and not in the final
image, consider setting a value for a single command instead:
> 如果仅在生成过程中需要环境变量，而不是在最终镜像中，请考虑为单个命令设置值：

```dockerfile
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y ...
```

Or using [`ARG`](#arg), which is not persisted in the final image:
> 或者使用[`ARG`](#arg)，它不会保留在最终镜像中：

```dockerfile
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y ...
```

> **Alternative syntax** 替代语法
>
> The `ENV` instruction also allows an alternative syntax `ENV <key> <value>`,
> omitting the `=`. For example:   
> `ENV`指令还允许使用另一种语法`ENV <key> <value>`，省略`=`。例如：  
>
> ```dockerfile
> ENV MY_VAR my-value
> ```
>
> This syntax does not allow for multiple environment-variables to be set in a
> single `ENV` instruction, and can be confusing. For example, the following
> sets a single environment variable (`ONE`) with value `"TWO= THREE=world"`:   
> 这种语法不允许在一条`ENV`指令中设置多个环境变量，这会造成混淆。
> 例如，下面设置一个值为`"TWO= THREE=world"`的环境变量(`ONE`)：  
>
> ```dockerfile
> ENV ONE TWO= THREE=world
> ```
>
> The alternative syntax is supported for backward compatibility, but discouraged
> for the reasons outlined above, and may be removed in a future release.  
> 支持替代语法是为了向后兼容，但由于上述原因不鼓励使用，并且可能在将来的版本中删除。

## ADD

ADD has two forms:
> ADD 有两种形式：

```dockerfile
ADD [--chown=<user>:<group>] <src>... <dest>
ADD [--chown=<user>:<group>] ["<src>",... "<dest>"]
```

The latter form is required for paths containing whitespace.
> 包含空格的路径需要后一种形式。

> **Note**
>
> The `--chown` feature is only supported on Dockerfiles used to build Linux containers,
> and will not work on Windows containers. Since user and group ownership concepts do
> not translate between Linux and Windows, the use of `/etc/passwd` and `/etc/group` for
> translating user and group names to IDs restricts this feature to only be viable
> for Linux OS-based containers.  
> `--chown`特性仅在用于构建Linux容器的Dockerfiles上受支持，在Windows容器上不起作用。
> 由于用户和组所有权概念不会在Linux和Windows之间转换，
> 因此使用/etc/passwd和/etc/group将用户名和组名转换为id将限制此功能仅适用于基于Linux操作系统的容器。

The `ADD` instruction copies new files, directories or remote file URLs from `<src>`
and adds them to the filesystem of the image at the path `<dest>`.
> `ADD`指令从`<src>`复制新文件、目录或远程文件URLs，并将它们添加到路径`<dest>`处的镜像文件系统中。

Multiple `<src>` resources may be specified but if they are files or
directories, their paths are interpreted as relative to the source of
the context of the build.
> 可以指定多个`<src>`资源，但如果它们是文件或目录，则它们的路径将被解释为相对于构建上下文的源。

Each `<src>` may contain wildcards and matching will be done using Go's
[filepath.Match](http://golang.org/pkg/path/filepath#Match) rules. For example:
> 每个`<src>`可能包含通配符，匹配将使用Go的[filepath.Match](http://golang.org/pkg/path/filepath#Match) 规则。例如：

To add all files starting with "hom":
> 添加所有以"hom"开头的文件：

```dockerfile
ADD hom* /mydir/
```

In the example below, `?` is replaced with any single character, e.g., "home.txt".
> 下面的列子中，`?`替换为任意单字符， 比如 "home.txt"。

```dockerfile
ADD hom?.txt /mydir/
```

The `<dest>` is an absolute path, or a path relative to `WORKDIR`, into which
the source will be copied inside the destination container.
> `<dest>`是一个绝对路径，或者是相对于WORKDIR的路径，源将被复制到目标容器中。

The example below uses a relative path, and adds "test.txt" to `<WORKDIR>/relativeDir/`:
> 下面的例子使用的是相对路径，添加 "test.txt" 到 `<WORKDIR>/relativeDir/`:

```dockerfile
ADD test.txt relativeDir/
```

Whereas this example uses an absolute path, and adds "test.txt" to `/absoluteDir/`
> 然后本例子使用的是绝对路径，添加"test.txt" 到 `/absoluteDir/` ：

```dockerfile
ADD test.txt /absoluteDir/
```

When adding files or directories that contain special characters (such as `[`
and `]`), you need to escape those paths following the Golang rules to prevent
them from being treated as a matching pattern. For example, to add a file
named `arr[0].txt`, use the following;
> 添加包含特殊字符（如 `[` 和 `]` ）的文件或目录时，需要按照Golang规则转义这些路径，以防止它们被视为匹配模式。
> 例如，要添加名为`arr[0].txt`的文件，请使用以下命令：

```dockerfile
ADD arr[[]0].txt /mydir/
```


All new files and directories are created with a UID and GID of 0, unless the
optional `--chown` flag specifies a given username, groupname, or UID/GID
combination to request specific ownership of the content added. The
format of the `--chown` flag allows for either username and groupname strings
or direct integer UID and GID in any combination. Providing a username without
groupname or a UID without GID will use the same numeric UID as the GID. If a
username or groupname is provided, the container's root filesystem
`/etc/passwd` and `/etc/group` files will be used to perform the translation
from name to integer UID or GID respectively. The following examples show
valid definitions for the `--chown` flag:
> 所有新文件和目录都是使用UID和GID为0创建的，除非可选的`--chown`标志指定一个给定的用户名、组名或UID/GID组合以请求所添加内容的特定所有权。
> `--chown`标志的格式允许username和groupname字符串或任意组合的直接整数UID和GID。
> 提供不带groupname的用户名或不带GID的UID将使用与GID相同的数字UID。
> 如果提供了用户名或组名，则将使用容器的根文件系统`/etc/passwd`和`/etc/group`文件分别执行从名称到整数UID或GID的转换。
> 以下示例显示--chown标志的有效定义：

```dockerfile
ADD --chown=55:mygroup files* /somedir/
ADD --chown=bin files* /somedir/
ADD --chown=1 files* /somedir/
ADD --chown=10:11 files* /somedir/
```

If the container root filesystem does not contain either `/etc/passwd` or
`/etc/group` files and either user or group names are used in the `--chown`
flag, the build will fail on the `ADD` operation. Using numeric IDs requires
no lookup and will not depend on container root filesystem content.
> 如果容器根文件系统不包含`/etc/passwd`或`/etc/group`文件，并且在`--chown`标志中使用了用户名或组名，
> 则构建在ADD操作中将失败。使用数字ID不需要查找，也不依赖于容器根文件系统内容。

In the case where `<src>` is a remote file URL, the destination will
have permissions of 600. If the remote file being retrieved has an HTTP
`Last-Modified` header, the timestamp from that header will be used
to set the `mtime` on the destination file. However, like any other file
processed during an `ADD`, `mtime` will not be included in the determination
of whether or not the file has changed and the cache should be updated.
> 如果`<src>`是远程文件URL，则目标将拥有600的权限。
> 如果要检索的远程文件具有`HTTP Last Modified`头，则该头中的时间戳将用于设置目标文件的`mtime`。

> **Note**
>
> If you build by passing a `Dockerfile` through STDIN (`docker
> build - < somefile`), there is no build context, so the `Dockerfile`
> can only contain a URL based `ADD` instruction. You can also pass a
> compressed archive through STDIN: (`docker build - < archive.tar.gz`),
> the `Dockerfile` at the root of the archive and the rest of the
> archive will be used as the context of the build.   
> 如果通过STDIN（`docker > build - < somefile`）传递`Dockerfile`进行构建，
> 则没有构建上下文，因此`Dockerfile`只能包含基于`ADD`指令的一个`URL`。
> 你还可以通过STDIN:（`docker build - < archive.tar.gz`)，
> 归档文件根目录下的`Dockerfile`和归档文件的其余部分将用作构建的上下文。

If your URL files are protected using authentication, you need to use `RUN wget`,
`RUN curl` or use another tool from within the container as the `ADD` instruction
does not support authentication.
> 如果URL文件使用身份验证进行保护，则需要使用`RUN wget`、`RUN curl`或使用容器中的其他工具，因为ADD指令不支持身份验证。

> **Note**
>
> The first encountered `ADD` instruction will invalidate the cache for all
> following instructions from the Dockerfile if the contents of `<src>` have
> changed. This includes invalidating the cache for `RUN` instructions.
> See the [`Dockerfile` Best Practices
guide – Leverage build cache](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#leverage-build-cache)
> for more information.  
> 如果`<src>`的内容已更改，则遇到的第一条`ADD`指令将使Dockerfile中所有后续指令的缓存失效。
> 这包括使`RUN`指令的缓存失效。
> 参考[`Dockerfile` Best Practices
    guide – Leverage build cache](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#leverage-build-cache) 获得更多信息。


`ADD` obeys the following rules:
> `ADD`遵循以下规则：

- The `<src>` path must be inside the *context* of the build;
  you cannot `ADD ../something /something`, because the first step of a
  `docker build` is to send the context directory (and subdirectories) to the
  docker daemon.
  > `<src>`路径必须在生成的上下文中；不能`ADD ../something /something`，
  > 因为`docker build`的第一步是将上下文目录（和子目录）发送到docker守护进程。

- If `<src>` is a URL and `<dest>` does not end with a trailing slash, then a
  file is downloaded from the URL and copied to `<dest>`.
  > 如果`<src>`是一个URL，而`<dest>`没有以斜杠结尾，则从URL下载一个文件并复制到`<dest>`。

- If `<src>` is a URL and `<dest>` does end with a trailing slash, then the
  filename is inferred from the URL and the file is downloaded to
  `<dest>/<filename>`. For instance, `ADD http://example.com/foobar /` would
  create the file `/foobar`. The URL must have a nontrivial path so that an
  appropriate filename can be discovered in this case (`http://example.com`
  will not work).
  > 如果`<src>`是一个URL，并且`<dest>`以斜杠结尾，则从URL推断文件名，并将文件下载到`<dest>/<filename>`。
  > 例如，`ADD http://example.com/foobar /`将创建文件`/foobar`。
  > URL必须有一个非平凡的路径，以便在这种情况下可以找到适当的文件名(http://example.com将不工作）。

- If `<src>` is a directory, the entire contents of the directory are copied,
  including filesystem metadata.
  > 如果`<src>`是一个目录，则复制目录的全部内容，包括文件系统元数据。

> **Note**
>
> The directory itself is not copied, just its contents.  
> 目录本身不复制的，只是复制它的内容。

- If `<src>` is a *local* tar archive in a recognized compression format
  (identity, gzip, bzip2 or xz) then it is unpacked as a directory. Resources
  from *remote* URLs are **not** decompressed. When a directory is copied or
  unpacked, it has the same behavior as `tar -x`, the result is the union of:
  > 如果`<src>`是一个*本地*tar归档文件，采用可识别的压缩格式（identity、gzip、bzip2或xz），则将其解包为目录。
  > *不会*解压缩来自*远程*URL的资源。复制或解压缩目录时，其行为与tar-x相同，其结果是:

    1. Whatever existed at the destination path and
        > 无论目标路径上存在什么
    2. The contents of the source tree, with conflicts resolved in favor
       of "2." on a file-by-file basis.
       > 源目录树的内容，以支持"2."的方式逐文件解决冲突。

  > **Note**
  >
  > Whether a file is identified as a recognized compression format or not
  > is done solely based on the contents of the file, not the name of the file.
  > For example, if an empty file happens to end with `.tar.gz` this will not
  > be recognized as a compressed file and **will not** generate any kind of
  > decompression error message, rather the file will simply be copied to the
  > destination.    
  > 文件是否被识别为可识别的压缩格式完全取决于文件的内容，而不是文件名。
  > 例如，如果一个空文件恰好以`.tar.gz`结尾 这不会被识别为压缩文件，也不会生成任何类型的解压缩错误消息，而只是将文件复制到目标。

- If `<src>` is any other kind of file, it is copied individually along with
  its metadata. In this case, if `<dest>` ends with a trailing slash `/`, it
  will be considered a directory and the contents of `<src>` will be written
  at `<dest>/base(<src>)`.
  > 如果`<src>`是任何其他类型的文件，则会将其与其元数据单独复制。
  > 在这种情况下，如果`<dest>`以斜杠`/`结尾，它将被视为一个目录，`<src>`的内容将写入`<dest>/base(<src>)`。

- If multiple `<src>` resources are specified, either directly or due to the
  use of a wildcard, then `<dest>` must be a directory, and it must end with
  a slash `/`.
  > 如果直接或由于使用通配符而指定了多个`<src>`资源，则`<dest>`必须是一个目录，并且必须以斜杠`/`结尾。

- If `<dest>` does not end with a trailing slash, it will be considered a
  regular file and the contents of `<src>` will be written at `<dest>`.
  > 如果`<dest>`没有以斜杠结尾，它将被视为常规文件，`<src>`的内容将写入`<dest>`。

- If `<dest>` doesn't exist, it is created along with all missing directories
  in its path.
  > 如果`<dest>`不存在，它将与其路径中所有丢失的目录一起创建。

## COPY

COPY has two forms:
> COPY 有两种形式：

```dockerfile
COPY [--chown=<user>:<group>] <src>... <dest>
COPY [--chown=<user>:<group>] ["<src>",... "<dest>"]
```

This latter form is required for paths containing whitespace
> 包含空格的路径需要后一种形式。

> **Note**
>
> The `--chown` feature is only supported on Dockerfiles used to build Linux containers,
> and will not work on Windows containers. Since user and group ownership concepts do
> not translate between Linux and Windows, the use of `/etc/passwd` and `/etc/group` for
> translating user and group names to IDs restricts this feature to only be viable for
> Linux OS-based containers.   
> `--chown`特性仅在用于构建Linux容器的Dockerfiles上受支持，在Windows容器上不起作用。
> 由于用户和组所有权概念不会在Linux和Windows之间转换，
> 因此使用/etc/passwd和/etc/group将用户名和组名转换为id将限制此功能仅适用于基于Linux操作系统的容器。

The `COPY` instruction copies new files or directories from `<src>`
and adds them to the filesystem of the container at the path `<dest>`.
> `COPY`指令从`<src>`复制新文件或目录，并将它们添加到路径`<dest>`处的镜像文件系统中。

Multiple `<src>` resources may be specified but the paths of files and
directories will be interpreted as relative to the source of the context
of the build.
> 可以指定多个`<src>`资源，但如果它们是文件或目录，则它们的路径将被解释为相对于构建上下文的源。

Each `<src>` may contain wildcards and matching will be done using Go's
[filepath.Match](http://golang.org/pkg/path/filepath#Match) rules. For example:
> 每个`<src>`可能包含通配符，匹配将使用Go的[filepath.Match](http://golang.org/pkg/path/filepath#Match) 规则。例如：

To add all files starting with "hom":
> 添加所有以"hom"开头的文件：

```dockerfile
COPY hom* /mydir/
```

In the example below, `?` is replaced with any single character, e.g., "home.txt".
> 下面的列子中，`?`替换为任意单字符， 比如 "home.txt"。

```dockerfile
COPY hom?.txt /mydir/
```

The `<dest>` is an absolute path, or a path relative to `WORKDIR`, into which
the source will be copied inside the destination container.
> `<dest>`是一个绝对路径，或者是相对于WORKDIR的路径，源将被复制到目标容器中。

The example below uses a relative path, and adds "test.txt" to `<WORKDIR>/relativeDir/`:
> 下面的例子使用的是相对路径，添加 "test.txt" 到 `<WORKDIR>/relativeDir/`:

```dockerfile
COPY test.txt relativeDir/
```

Whereas this example uses an absolute path, and adds "test.txt" to `/absoluteDir/`
> 然后本例子使用的是绝对路径，添加"test.txt" 到 `/absoluteDir/` ：

```dockerfile
COPY test.txt /absoluteDir/
```

When copying files or directories that contain special characters (such as `[`
and `]`), you need to escape those paths following the Golang rules to prevent
them from being treated as a matching pattern. For example, to copy a file
named `arr[0].txt`, use the following;
> 拷贝包含特殊字符（如 `[` 和 `]` ）的文件或目录时，需要按照Golang规则转义这些路径，以防止它们被视为匹配模式。
> 例如，要添加名为`arr[0].txt`的文件，请使用以下命令：

```dockerfile
COPY arr[[]0].txt /mydir/
```

All new files and directories are created with a UID and GID of 0, unless the
optional `--chown` flag specifies a given username, groupname, or UID/GID
combination to request specific ownership of the copied content. The
format of the `--chown` flag allows for either username and groupname strings
or direct integer UID and GID in any combination. Providing a username without
groupname or a UID without GID will use the same numeric UID as the GID. If a
username or groupname is provided, the container's root filesystem
`/etc/passwd` and `/etc/group` files will be used to perform the translation
from name to integer UID or GID respectively. The following examples show
valid definitions for the `--chown` flag:
> 所有新文件和目录都是使用UID和GID为0创建的，除非可选的`--chown`标志指定一个给定的用户名、组名或UID/GID组合以请求所拷贝内容的特定所有权。
> `--chown`标志的格式允许username和groupname字符串或任意组合的直接整数UID和GID。
> 提供不带groupname的用户名或不带GID的UID将使用与GID相同的数字UID。
> 如果提供了用户名或组名，则将使用容器的根文件系统`/etc/passwd`和`/etc/group`文件分别执行从名称到整数UID或GID的转换。
> 以下示例显示--chown标志的有效定义：


```dockerfile
COPY --chown=55:mygroup files* /somedir/
COPY --chown=bin files* /somedir/
COPY --chown=1 files* /somedir/
COPY --chown=10:11 files* /somedir/
```

If the container root filesystem does not contain either `/etc/passwd` or
`/etc/group` files and either user or group names are used in the `--chown`
flag, the build will fail on the `COPY` operation. Using numeric IDs requires
no lookup and does not depend on container root filesystem content.
> 如果容器根文件系统不包含`/etc/passwd`或`/etc/group`文件，并且在`--chown`标志中使用了用户名或组名，
> 则构建在`COPY`操作中将失败。使用数字ID不需要查找，也不依赖于容器根文件系统内容。

> **Note**
>
> If you build using STDIN (`docker build - < somefile`), there is no
> build context, so `COPY` can't be used.  
> 如果你使用STDIN (`docker build - < somefile`)来构建，因为没有构建上下文，所以`COPY`不能使用。

Optionally `COPY` accepts a flag `--from=<name>` that can be used to set
the source location to a previous build stage (created with `FROM .. AS <name>`)
that will be used instead of a build context sent by the user. In case a build
stage with a specified name can't be found an image with the same name is
attempted to be used instead.
> 可选地，`COPY` 接受一个标志`--from=<name>`，该标志可用于将源位置设置为上一个构建阶段（使用`FROM .. AS <name>`创建），
> 将代替用户发送的生成上下文。如果找不到具有指定名称的生成阶段，将尝试改用具有相同名称的镜像。

`COPY` obeys the following rules:
> `COPY`遵循以下规则：

- The `<src>` path must be inside the *context* of the build;
  you cannot `COPY ../something /something`, because the first step of a
  `docker build` is to send the context directory (and subdirectories) to the
  docker daemon.
  > `<src>`路径必须在生成的上下文中；不能`COPY ../something /something`，
  > 因为`docker build`的第一步是将上下文目录（和子目录）发送到docker守护进程。

- If `<src>` is a directory, the entire contents of the directory are copied,
  including filesystem metadata.
  > 如果`<src>`是一个目录，则复制目录的全部内容，包括文件系统元数据。

> **Note**
>
> The directory itself is not copied, just its contents.  
> 目录本身不复制的，只是复制它的内容。

- If `<src>` is any other kind of file, it is copied individually along with
  its metadata. In this case, if `<dest>` ends with a trailing slash `/`, it
  will be considered a directory and the contents of `<src>` will be written
  at `<dest>/base(<src>)`.
  > 如果`<src>`是任何其他类型的文件，则会将其与其元数据单独复制。
  > 在这种情况下，如果`<dest>`以斜杠`/`结尾，它将被视为一个目录，`<src>`的内容将写入`<dest>/base(<src>)`。

- If multiple `<src>` resources are specified, either directly or due to the
  use of a wildcard, then `<dest>` must be a directory, and it must end with
  a slash `/`.
  > 如果直接或由于使用通配符而指定了多个`<src>`资源，则`<dest>`必须是一个目录，并且必须以斜杠`/`结尾。

- If `<dest>` does not end with a trailing slash, it will be considered a
  regular file and the contents of `<src>` will be written at `<dest>`.
  > 如果`<dest>`没有以斜杠结尾，它将被视为常规文件，`<src>`的内容将写入`<dest>`。

- If `<dest>` doesn't exist, it is created along with all missing directories
  in its path.
  > 如果`<dest>`不存在，它将与其路径中所有丢失的目录一起创建。

> **Note**
>
> The first encountered `COPY` instruction will invalidate the cache for all
> following instructions from the Dockerfile if the contents of `<src>` have
> changed. This includes invalidating the cache for `RUN` instructions.
> See the [`Dockerfile` Best Practices
guide – Leverage build cache](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#leverage-build-cache)
> for more information.   
> for more information.  
> 如果`<src>`的内容已更改，则遇到的第一条`COPY`指令将使Dockerfile中所有后续指令的缓存失效。
> 这包括使`RUN`指令的缓存失效。
> 参考[`Dockerfile` Best Practices
guide – Leverage build cache](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#leverage-build-cache) 获得更多信息。

## ENTRYPOINT

ENTRYPOINT has two forms:
> ENTRYPOINT 有两种形式：

The *exec* form, which is the preferred form:
> *exec* 格式是首选格式：

```dockerfile
ENTRYPOINT ["executable", "param1", "param2"]
```

The *shell* form:
> *shell* 形式：

```dockerfile
ENTRYPOINT command param1 param2
```

An `ENTRYPOINT` allows you to configure a container that will run as an executable.
> `ENTRYPOINT` 允许你配置一个将作为可执行文件的容器。

For example, the following starts nginx with its default content, listening
on port 80:
> 例如，以下命令使用默认内容启动nginx，监听端口80：

```bash
$ docker run -i -t --rm -p 80:80 nginx
```

Command line arguments to `docker run <image>` will be appended after all
elements in an *exec* form `ENTRYPOINT`, and will override all elements specified
using `CMD`.
This allows arguments to be passed to the entry point, i.e., `docker run <image> -d`
will pass the `-d` argument to the entry point.
You can override the `ENTRYPOINT` instruction using the `docker run --entrypoint`
flag.
> `docker run <image>`的命令行参数将附加在*exec*形式 `ENTRYPOINT`中的所有元素之后，并将覆盖使用CMD指定的所有元素。
> 这允许将参数传递给入口点，即d`docker run <image> -d`将把`-d`参数传递给入口点。
> 您可以使用`docker run --entrypoint`标志重写`ENTRYPOINT`指令。

The *shell* form prevents any `CMD` or `run` command line arguments from being
used, but has the disadvantage that your `ENTRYPOINT` will be started as a
subcommand of `/bin/sh -c`, which does not pass signals.
This means that the executable will not be the container's `PID 1` - and
will _not_ receive Unix signals - so your executable will not receive a
`SIGTERM` from `docker stop <container>`.
> *shell*形式防止使用任何`CMD`或`run`命令行参数，但缺点是`ENTRYPOINT`将作为`/bin/sh -c`的子命令启动，它不会传递信号。
> 这意味着可执行文件将不会是容器的`PID 1`，也不会接收Unix信号，因此您的可执行文件将不会从`docker stop <container>`接收`SIGTERM`。

Only the last `ENTRYPOINT` instruction in the `Dockerfile` will have an effect.
> 只有`Dockerfile`中的最后一个`ENTRYPOINT`指令才有效果。

### Exec form ENTRYPOINT example
> exec 形式的 ENTRYPOINT 例子

You can use the *exec* form of `ENTRYPOINT` to set fairly stable default commands
and arguments and then use either form of `CMD` to set additional defaults that
are more likely to be changed.
> 您可以使用`ENTRYPOINT`的*exec*形式来设置相当稳定的默认命令和参数，
> 然后使用`CMD`的任一形式来设置更容易更改的其他默认值。

```dockerfile
FROM ubuntu
ENTRYPOINT ["top", "-b"]
CMD ["-c"]
```

When you run the container, you can see that `top` is the only process:
> 当你运行容器时，可以看到top是唯一的进程：

```bash
$ docker run -it --rm --name test  top -H

top - 08:25:00 up  7:27,  0 users,  load average: 0.00, 0.01, 0.05
Threads:   1 total,   1 running,   0 sleeping,   0 stopped,   0 zombie
%Cpu(s):  0.1 us,  0.1 sy,  0.0 ni, 99.7 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem:   2056668 total,  1616832 used,   439836 free,    99352 buffers
KiB Swap:  1441840 total,        0 used,  1441840 free.  1324440 cached Mem

  PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
    1 root      20   0   19744   2336   2080 R  0.0  0.1   0:00.04 top
```

To examine the result further, you can use `docker exec`:
> 要进一步检查结果，可以使用`docker exec`

```bash
$ docker exec -it test ps aux

USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root         1  2.6  0.1  19752  2352 ?        Ss+  08:24   0:00 top -b -H
root         7  0.0  0.1  15572  2164 ?        R+   08:25   0:00 ps aux
```

And you can gracefully request `top` to shut down using `docker stop test`.
> 您可以使用`docker stop test`优雅地请求`top`关闭。

The following `Dockerfile` shows using the `ENTRYPOINT` to run Apache in the
foreground (i.e., as `PID 1`):
> 以下`Dockerfile`显示了如何使用`ENTRYPOINT`在前台运行Apache（即作为`PID 1`）：

```dockerfile
FROM debian:stable
RUN apt-get update && apt-get install -y --force-yes apache2
EXPOSE 80 443
VOLUME ["/var/www", "/var/log/apache2", "/etc/apache2"]
ENTRYPOINT ["/usr/sbin/apache2ctl", "-D", "FOREGROUND"]
```

If you need to write a starter script for a single executable, you can ensure that
the final executable receives the Unix signals by using `exec` and `gosu`
commands:
> 如果需要为单个可执行文件编写启动程序脚本，可以使用`exec`和`gosu`命令确保最终可执行文件接收Unix信号：

```bash
#!/usr/bin/env bash
set -e

if [ "$1" = 'postgres' ]; then
    chown -R postgres "$PGDATA"

    if [ -z "$(ls -A "$PGDATA")" ]; then
        gosu postgres initdb
    fi

    exec gosu postgres "$@"
fi

exec "$@"
```

Lastly, if you need to do some extra cleanup (or communicate with other containers)
on shutdown, or are co-ordinating more than one executable, you may need to ensure
that the `ENTRYPOINT` script receives the Unix signals, passes them on, and then
does some more work:
> 最后，如果您需要在关闭时执行一些额外的清理（或与其他容器通信），或者协调多个可执行文件，
> 则可能需要确保`ENTRYPOINT`脚本接收Unix信号，传递这些信号，然后执行更多的工作：

```bash
#!/bin/sh
# Note: I've written this using sh so it works in the busybox container too

# USE the trap if you need to also do manual cleanup after the service is stopped,
#     or need to start multiple services in the one container
trap "echo TRAPed signal" HUP INT QUIT TERM

# start service in background here
/usr/sbin/apachectl start

echo "[hit enter key to exit] or run 'docker stop <container>'"
read

# stop service and clean up here
echo "stopping apache"
/usr/sbin/apachectl stop

echo "exited $0"
```

If you run this image with `docker run -it --rm -p 80:80 --name test apache`,
you can then examine the container's processes with `docker exec`, or `docker top`,
and then ask the script to stop Apache:
> 如果使用`docker run -it --rm -p 80:80 --name test apache`运行此镜像，
> 则可以使用使用`docker exec`或`docker top`检查容器的进程，然后要求脚本停止Apache：

```bash
$ docker exec -it test ps aux

USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root         1  0.1  0.0   4448   692 ?        Ss+  00:42   0:00 /bin/sh /run.sh 123 cmd cmd2
root        19  0.0  0.2  71304  4440 ?        Ss   00:42   0:00 /usr/sbin/apache2 -k start
www-data    20  0.2  0.2 360468  6004 ?        Sl   00:42   0:00 /usr/sbin/apache2 -k start
www-data    21  0.2  0.2 360468  6000 ?        Sl   00:42   0:00 /usr/sbin/apache2 -k start
root        81  0.0  0.1  15572  2140 ?        R+   00:44   0:00 ps aux

$ docker top test

PID                 USER                COMMAND
10035               root                {run.sh} /bin/sh /run.sh 123 cmd cmd2
10054               root                /usr/sbin/apache2 -k start
10055               33                  /usr/sbin/apache2 -k start
10056               33                  /usr/sbin/apache2 -k start

$ /usr/bin/time docker stop test

test
real	0m 0.27s
user	0m 0.03s
sys	0m 0.03s
```

> **Note**
>
> You can override the `ENTRYPOINT` setting using `--entrypoint`,
> but this can only set the binary to *exec* (no `sh -c` will be used).  
> 您可以使用`--entrypoint`重写`ENTRYPOINT`设置，但这只能将二进制文件设置为*exec*（不使用`sh -c`）。

> **Note**
>
> The *exec* form is parsed as a JSON array, which means that
> you must use double-quotes (") around words not single-quotes (').  
> exec表单被解析为JSON数组，这意味着您必须在单词周围使用双引号(")，而不是单引号(')。

Unlike the *shell* form, the *exec* form does not invoke a command shell.
This means that normal shell processing does not happen. For example,
`ENTRYPOINT [ "echo", "$HOME" ]` will not do variable substitution on `$HOME`.
If you want shell processing then either use the *shell* form or execute
a shell directly, for example: `ENTRYPOINT [ "sh", "-c", "echo $HOME" ]`.
When using the exec form and executing a shell directly, as in the case for
the shell form, it is the shell that is doing the environment variable
expansion, not docker.
> 与*shell*形式不同，*exec*形式不调用命令shell。这意味着正常的shell处理不会发生。
> 例如，`ENTRYPOINT [ "echo", "$HOME" ]`不会对`$HOME`进行变量替换。
> 如果需要shell处理，那么可以使用*shell*形式，也可以直接执行shell，例如：`ENTRYPOINT [ "sh", "-c", "echo $HOME" ]`。
> 当使用exec形式并直接执行shell时，就像shell形式的情况一样，执行环境变量扩展的是shell，而不是docker。

### Shell form ENTRYPOINT example
> Shell 形式 ENTRYPOINT 例子

You can specify a plain string for the `ENTRYPOINT` and it will execute in `/bin/sh -c`.
This form will use shell processing to substitute shell environment variables,
and will ignore any `CMD` or `docker run` command line arguments.
To ensure that `docker stop` will signal any long running `ENTRYPOINT` executable
correctly, you need to remember to start it with `exec`:
> 您可以为`ENTRYPOINT`指定一个普通字符串，它将在/bin/sh-c中执行。
> 此形式将使用shell处理来替换shell环境变量，并将忽略任何`CMD`或`docker run`命令行参数。
> 为了确保`docker stop`正确地向任何长时间运行的`ENTRYPOINT`可执行文件发出信号，您需要记住使用`exec`启动它：

```dockerfile
FROM ubuntu
ENTRYPOINT exec top -b
```

When you run this image, you'll see the single `PID 1` process:
> 当你运行此镜像时，将看到单个`PID 1`进程：

```bash
$ docker run -it --rm --name test top

Mem: 1704520K used, 352148K free, 0K shrd, 0K buff, 140368121167873K cached
CPU:   5% usr   0% sys   0% nic  94% idle   0% io   0% irq   0% sirq
Load average: 0.08 0.03 0.05 2/98 6
  PID  PPID USER     STAT   VSZ %VSZ %CPU COMMAND
    1     0 root     R     3164   0%   0% top -b
```

Which exits cleanly on `docker stop`:
> 使用`docker stop` 干净的退出：

```bash
$ /usr/bin/time docker stop test

test
real	0m 0.20s
user	0m 0.02s
sys	0m 0.04s
```

If you forget to add `exec` to the beginning of your `ENTRYPOINT`:
> 如果忘记将`exec`添加到`ENTRYPOINT`的开头：

```dockerfile
FROM ubuntu
ENTRYPOINT top -b
CMD --ignored-param1
```

You can then run it (giving it a name for the next step):
> 然后可以运行它（为下一步命名）：

```bash
$ docker run -it --name test top --ignored-param2

Mem: 1704184K used, 352484K free, 0K shrd, 0K buff, 140621524238337K cached
CPU:   9% usr   2% sys   0% nic  88% idle   0% io   0% irq   0% sirq
Load average: 0.01 0.02 0.05 2/101 7
  PID  PPID USER     STAT   VSZ %VSZ %CPU COMMAND
    1     0 root     S     3168   0%   0% /bin/sh -c top -b cmd cmd2
    7     1 root     R     3164   0%   0% top -b
```

You can see from the output of `top` that the specified `ENTRYPOINT` is not `PID 1`.
> 从`top`的输出可以看出指定的`ENTRYPOINT`不是`PID 1`。

If you then run `docker stop test`, the container will not exit cleanly - the
`stop` command will be forced to send a `SIGKILL` after the timeout:
> 如果运行`docker stop test`，容器将不会干净地退出， `stop`命令将被迫在超时后发送`SIGKILL`：

```bash
$ docker exec -it test ps aux

PID   USER     COMMAND
    1 root     /bin/sh -c top -b cmd cmd2
    7 root     top -b
    8 root     ps aux

$ /usr/bin/time docker stop test

test
real	0m 10.19s
user	0m 0.04s
sys	0m 0.03s
```

### Understand how CMD and ENTRYPOINT interact
> 了解CMD和ENTRYPOINT如何交互

Both `CMD` and `ENTRYPOINT` instructions define what command gets executed when running a container.
There are few rules that describe their co-operation.
> `CMD`和`ENTRYPOINT`指令都定义了运行容器时执行的命令。这里有少许规则描述他们的合作。

1. Dockerfile should specify at least one of `CMD` or `ENTRYPOINT` commands.
     >Dockerfile应至少指定一个`CMD`或`ENTRYPOINT`命令。

2. `ENTRYPOINT` should be defined when using the container as an executable.
    > 在将容器用作可执行文件时，应定义`ENTRYPOINT`。

3. `CMD` should be used as a way of defining default arguments for an `ENTRYPOINT` command
   or for executing an ad-hoc command in a container.
   > `CMD`应该用作定义`ENTRYPOINT`命令或在容器中执行特殊命令的默认参数的方法。

4. `CMD` will be overridden when running the container with alternative arguments.
    > 当使用其他参数运行容器时，`CMD`将被重写。

The table below shows what command is executed for different `ENTRYPOINT` / `CMD` combinations:
> 下表显示了针对不同`ENTRYPOINT` / `CMD`组合执行的命令：

|                                | No ENTRYPOINT              | ENTRYPOINT exec_entry p1_entry | ENTRYPOINT ["exec_entry", "p1_entry"]          |
|:-------------------------------|:---------------------------|:-------------------------------|:-----------------------------------------------|
| **No CMD**                     | *error, not allowed*       | /bin/sh -c exec_entry p1_entry | exec_entry p1_entry                            |
| **CMD ["exec_cmd", "p1_cmd"]** | exec_cmd p1_cmd            | /bin/sh -c exec_entry p1_entry | exec_entry p1_entry exec_cmd p1_cmd            |
| **CMD ["p1_cmd", "p2_cmd"]**   | p1_cmd p2_cmd              | /bin/sh -c exec_entry p1_entry | exec_entry p1_entry p1_cmd p2_cmd              |
| **CMD exec_cmd p1_cmd**        | /bin/sh -c exec_cmd p1_cmd | /bin/sh -c exec_entry p1_entry | exec_entry p1_entry /bin/sh -c exec_cmd p1_cmd |

> **Note**
>
> If `CMD` is defined from the base image, setting `ENTRYPOINT` will
> reset `CMD` to an empty value. In this scenario, `CMD` must be defined in the
> current image to have a value.  
> 如果`CMD`是从基本镜像定义的，设置`ENTRYPOINT`会将`CMD`重置为空值。
> 在这种情况下，必须在当前镜像中定义`CMD`才能有值。

## VOLUME

```dockerfile
VOLUME ["/data"]
```

The `VOLUME` instruction creates a mount point with the specified name
and marks it as holding externally mounted volumes from native host or other
containers. The value can be a JSON array, `VOLUME ["/var/log/"]`, or a plain
string with multiple arguments, such as `VOLUME /var/log` or `VOLUME /var/log
/var/db`. For more information/examples and mounting instructions via the
Docker client, refer to
[*Share Directories via Volumes*](https://docs.docker.com/storage/volumes/)
documentation.
> `VOLUME`指令创建具有指定名称的装入点，并将其标记为保存来自本机主机或其他容器的外部装入的卷。
> 该值可以是JSON数组, `VOLUME ["/var/log/"]`，或具有多个参数的普通字符串，如`VOLUME /var/log`或`VOLUME /var/log /var/db`。
> 有关通过Docker客户端的更多信息/示例和安装说明，请参阅通[*Share Directories via Volumes*](https://docs.docker.com/storage/volumes/) 。

The `docker run` command initializes the newly created volume with any data
that exists at the specified location within the base image. For example,
consider the following Dockerfile snippet:
> `docker run`命令使用存在于基本镜像中指定位置的任何数据初始化新创建的卷。
> 例如，考虑以下Dockerfile片段：

```dockerfile
FROM ubuntu
RUN mkdir /myvol
RUN echo "hello world" > /myvol/greeting
VOLUME /myvol
```

This Dockerfile results in an image that causes `docker run` to
create a new mount point at `/myvol` and copy the  `greeting` file
into the newly created volume.
> 此Dockerfile生成一个镜像，使`docker run`在`/myvol`处创建一个新的装入点，并将`greeting`文件复制到新创建的卷中。

### Notes about specifying volumes

Keep the following things in mind about volumes in the `Dockerfile`.
> 关于`Dockerfile`中的卷，请记住以下几点。

- **Volumes on Windows-based containers**: When using Windows-based containers,
  the destination of a volume inside the container must be one of:
  > 使用基于Windows的容器时，容器内卷的目标必须是以下之一：

    - a non-existing or empty directory(一个不存在或为空的目录)
    - a drive other than `C:`（除了`C:`以外的驱动）

- **Changing the volume from within the Dockerfile**: If any build steps change the
  data within the volume after it has been declared, those changes will be discarded.
  > 如果任何生成步骤在声明卷后更改了卷内的数据，则这些更改将被丢弃。

- **JSON formatting**: The list is parsed as a JSON array.
  You must enclose words with double quotes (`"`) rather than single quotes (`'`).
  > 列表被解析为JSON数组。必须用双引号(`"`)而不是单引号 (`'`)将单词括起来。

- **The host directory is declared at container run-time**: The host directory
  (the mountpoint) is, by its nature, host-dependent. This is to preserve image
  portability, since a given host directory can't be guaranteed to be available
  on all hosts. For this reason, you can't mount a host directory from
  within the Dockerfile. The `VOLUME` instruction does not support specifying a `host-dir`
  parameter.  You must specify the mountpoint when you create or run the container.
  > 主机目录（挂载点）本质上依赖于主机。这是为了保持镜像的可移植性，因为不能保证给定的主机目录在所有主机上都可用。
  > 因此，无法从Dockerfile中装载主机目录。`VOLUME`指令不支持指定`host dir`参数。必须在创建或运行容器时指定装入点。

## USER

```dockerfile
USER <user>[:<group>]
```

or

```dockerfile
USER <UID>[:<GID>]
```

The `USER` instruction sets the user name (or UID) and optionally the user
group (or GID) to use when running the image and for any `RUN`, `CMD` and
`ENTRYPOINT` instructions that follow it in the `Dockerfile`.
> `USER`指令设置运行镜像时要使用的用户名（或UID）和可选的用户组（或GID），
> 以及`Dockerfile`中紧随其后的任何`RUN`、`CMD`和`ENTRYPOINT`指令。

> Note that when specifying a group for the user, the user will have _only_ the
> specified group membership. Any other configured group memberships will be ignored.  
> 请注意，为用户指定组时，用户将只有指定的组成员资格。任何其他配置的组成员身份都将被忽略。

> **Warning**
>
> When the user doesn't have a primary group then the image (or the next
> instructions) will be run with the `root` group.   
> 当用户没有主组时，镜像（或下一个指令）将与`root`组一起运行。
>
> On Windows, the user must be created first if it's not a built-in account.
> This can be done with the `net user` command called as part of a Dockerfile.   
> 在Windows上，如果用户不是内置帐户，则必须首先创建该用户。这可以通过在Dockerfile中调用`net user`命令来完成。

```dockerfile
FROM microsoft/windowsservercore
# Create Windows user in the container
RUN net user /add patrick
# Set it for subsequent commands
USER patrick
```


## WORKDIR

```dockerfile
WORKDIR /path/to/workdir
```

The `WORKDIR` instruction sets the working directory for any `RUN`, `CMD`,
`ENTRYPOINT`, `COPY` and `ADD` instructions that follow it in the `Dockerfile`.
If the `WORKDIR` doesn't exist, it will be created even if it's not used in any
subsequent `Dockerfile` instruction.
> `WORKDIR`指令为`Dockerfile`中的`RUN`、`CMD`、`ENTRYPOINT`、`COPY`和`ADD`指令设置工作目录。
> 如果`WORKDIR`不存在，即使在任何后续`Dockerfile`指令中不使用也会被创建。

The `WORKDIR` instruction can be used multiple times in a `Dockerfile`. If a
relative path is provided, it will be relative to the path of the previous
`WORKDIR` instruction. For example:
> `WORKDIR`指令可以在一个`Dockerfile`中多次使用。
> 如果提供了相对路径，它将相对于上一条WORKDIR指令的路径。例如：

```dockerfile
WORKDIR /a
WORKDIR b
WORKDIR c
RUN pwd
```

The output of the final `pwd` command in this `Dockerfile` would be `/a/b/c`.
> 该`Dockerfile`的`pwd`指令最终输出将是`/a/b/c`。

The `WORKDIR` instruction can resolve environment variables previously set using
`ENV`. You can only use environment variables explicitly set in the `Dockerfile`.
For example:
> `WORKDIR`指令可以解析之前使用`ENV`设置的环境变量。只能使用`Dockerfile`中显式设置的环境变量。例如：

```dockerfile
ENV DIRPATH=/path
WORKDIR $DIRPATH/$DIRNAME
RUN pwd
```

The output of the final `pwd` command in this `Dockerfile` would be
`/path/$DIRNAME`
> 此`Dockerfile`中的最终`pwd`命令的输出将是`/path/$DIRNAME`

## ARG

```dockerfile
ARG <name>[=<default value>]
```

The `ARG` instruction defines a variable that users can pass at build-time to
the builder with the `docker build` command using the `--build-arg <varname>=<value>`
flag. If a user specifies a build argument that was not
defined in the Dockerfile, the build outputs a warning.
> `ARG`指令定义了一个变量，用户可以在构建时使用`--build ARG<varname>=<value>`标志通过`docker build`指令将该变量传递给构建器。
> 如果用户指定了Dockerfile中未定义的构建参数，则构建将输出警告。

```console
[Warning] One or more build-args [foo] were not consumed.
```

A Dockerfile may include one or more `ARG` instructions. For example,
the following is a valid Dockerfile:
> Dockerfile可以包含一个或多个`ARG`指令。例如，以下是有效的Dockerfile：

```dockerfile
FROM busybox
ARG user1
ARG buildno
# ...
```

> **Warning:**
>
> It is not recommended to use build-time variables for passing secrets like
> github keys, user credentials etc. Build-time variable values are visible to
> any user of the image with the `docker history` command.   
> 不建议使用构建时（build-time）变量传递机密，如github密钥、用户凭据等。
> 使用`docker history`指令，镜像的任何用户都可以看到构建时变量值。
>
> Refer to the ["build images with BuildKit"](https://docs.docker.com/develop/develop-images/build_enhancements/#new-docker-build-secret-information)
> section to learn about secure ways to use secrets when building images.
{:.warning}   
> 请参阅["build images with BuildKit"](https://docs.docker.com/develop/develop-images/build_enhancements/#new-docker-build-secret-information) 部分，了解在构建镜像时使用机密的安全方法。

### Default values

An `ARG` instruction can optionally include a default value:
> `ARG`指令可以选择包含默认值：

```dockerfile
FROM busybox
ARG user1=someuser
ARG buildno=1
# ...
```

If an `ARG` instruction has a default value and if there is no value passed
at build-time, the builder uses the default.
> 如果一个`ARG`指令有默认值并且构建时并未传入值，构建器将采用默认值。

### Scope

An `ARG` variable definition comes into effect from the line on which it is
defined in the `Dockerfile` not from the argument's use on the command-line or
elsewhere.  For example, consider this Dockerfile:
> `ARG`变量定义是从`Dockerfile`中定义它的行开始生效，而不是从命令行或其他地方使用参数开始生效。
> 例如，考虑以下Dockerfile：

```dockerfile
FROM busybox
USER ${user:-some_user}
ARG user
USER $user
# ...
```
A user builds this file by calling:
> 用户通过调用一下命令构建该文件：

```bash
$ docker build --build-arg user=what_user .
```

The `USER` at line 2 evaluates to `some_user` as the `user` variable is defined on the
subsequent line 3. The `USER` at line 4 evaluates to `what_user` as `user` is
defined and the `what_user` value was passed on the command line. Prior to its definition by an
`ARG` instruction, any use of a variable results in an empty string.
> 第2行的`USER`计算结果为`some_user`，因为`user`变量在随后的第3行中定义。
> 第4行的`USER`计算结果为`what_user`，因为`user`已定义并在命令行上传递了`what_user`值。
> 在`ARG`指令定义变量之前，任何变量的使用都会导致空字符串。

An `ARG` instruction goes out of scope at the end of the build
stage where it was defined. To use an arg in multiple stages, each stage must
include the `ARG` instruction.
> `ARG`指令在定义它的构建阶段结束时超出范围。要在多个阶段中使用arg，每个阶段必须包含`ARG`指令。

```dockerfile
FROM busybox
ARG SETTINGS
RUN ./run/setup $SETTINGS

FROM busybox
ARG SETTINGS
RUN ./run/other $SETTINGS
```

### Using ARG variables

You can use an `ARG` or an `ENV` instruction to specify variables that are
available to the `RUN` instruction. Environment variables defined using the
`ENV` instruction always override an `ARG` instruction of the same name. Consider
this Dockerfile with an `ENV` and `ARG` instruction.
> 可以使用`ARG`或`ENV`指令指定`RUN`指令可用的变量。
> 使用`ENV`指令定义的环境变量总是重写同名的`ARG`指令。
> 考虑一下这个带有`ENV`和`ARG`指令的Dockerfile。

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
ENV CONT_IMG_VER=v1.0.0
RUN echo $CONT_IMG_VER
```

Then, assume this image is built with this command:
> 然后，假设此镜像是使用以下命令生成：

```bash
$ docker build --build-arg CONT_IMG_VER=v2.0.1 .
```

In this case, the `RUN` instruction uses `v1.0.0` instead of the `ARG` setting
passed by the user:`v2.0.1` This behavior is similar to a shell
script where a locally scoped variable overrides the variables passed as
arguments or inherited from environment, from its point of definition.
> 在这种情况下，`RUN`指令使用`v1.0.0`而不是用户传入的：`v2.0.1` 。
> 此行为类似于shell脚本，在该脚本中，局部作用域变量将重写作为参数传递的变量或从其它环境继承的变量。

Using the example above but a different `ENV` specification you can create more
useful interactions between `ARG` and `ENV` instructions:
> 使用上面的示例，但使用不同的`ENV`规范，可以在`ARG`和`ENV`指令之间创建更有用的交互：

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
ENV CONT_IMG_VER=${CONT_IMG_VER:-v1.0.0}
RUN echo $CONT_IMG_VER
```

Unlike an `ARG` instruction, `ENV` values are always persisted in the built
image. Consider a docker build without the `--build-arg` flag:
> 与`ARG`指令不同，`ENV`值始终保留在生成的镜像中。考虑一个没有`--build arg`标志的docker构建：

```bash
$ docker build .
```

Using this Dockerfile example, `CONT_IMG_VER` is still persisted in the image but
its value would be `v1.0.0` as it is the default set in line 3 by the `ENV` instruction.
> 使用这个Dockerfile示例，`CONT_IMG_VER`仍然保留在镜像中，但是它的值将是v1.0.0，因为它是ENV指令在第3行中设置的默认值。

The variable expansion technique in this example allows you to pass arguments
from the command line and persist them in the final image by leveraging the
`ENV` instruction. Variable expansion is only supported for [a limited set of
Dockerfile instructions.](#environment-replacement)
> 本例中的变量扩展技术允许您从命令行传递参数，并通过利用ENV指令将它们持久化到最终镜像中。
> 变量扩展仅支持有限的Dockerfile指令集。

### Predefined ARGs
> 预定义参数

Docker has a set of predefined `ARG` variables that you can use without a
corresponding `ARG` instruction in the Dockerfile.
> Docker有一组预定义的`ARG`变量，您可以在Dockerfile中不使用相应的`ARG`指令的情况下使用这些变量。

- `HTTP_PROXY`
- `http_proxy`
- `HTTPS_PROXY`
- `https_proxy`
- `FTP_PROXY`
- `ftp_proxy`
- `NO_PROXY`
- `no_proxy`

To use these, simply pass them on the command line using the flag:
> 要使用它们，只需在命令行上使用以下标志传递它们：

```bash
--build-arg <varname>=<value>
```

By default, these pre-defined variables are excluded from the output of
`docker history`. Excluding them reduces the risk of accidentally leaking
sensitive authentication information in an `HTTP_PROXY` variable.
> 默认情况下，这些预定义变量从`docker history`的输出中排除。
> 排除它们可以降低在`HTTP_PROXY`变量中意外泄漏敏感身份验证信息的风险。

For example, consider building the following Dockerfile using
`--build-arg HTTP_PROXY=http://user:pass@proxy.lon.example.com`
> 例如，考虑使用`--build-arg HTTP_PROXY=http://user:pass@proxy.lon.example.com`构建下面的Dockerfile

```dockerfile
FROM ubuntu
RUN echo "Hello World"
```

In this case, the value of the `HTTP_PROXY` variable is not available in the
`docker history` and is not cached. If you were to change location, and your
proxy server changed to `http://user:pass@proxy.sfo.example.com`, a subsequent
build does not result in a cache miss.
> 在这种情况下，`HTTP_PROXY`变量的值在`docker history`记录中不可用，并且不会被缓存。
> 如果您要更改位置，并且您的代理服务器更改为`http://user:pass@proxy.sfo.example.com`，后续生成不会导致缓存未命中。

If you need to override this behaviour then you may do so by adding an `ARG`
statement in the Dockerfile as follows:
> 如果需要重写此行为，则可以通过在Dockerfile中添加`ARG`语句来执行以下操作：

```dockerfile
FROM ubuntu
ARG HTTP_PROXY
RUN echo "Hello World"
```

When building this Dockerfile, the `HTTP_PROXY` is preserved in the
`docker history`, and changing its value invalidates the build cache.
> 在构建这个Dockerfile时，`HTTP_PROXY`将保留在`docker history`记录中，更改其值将使生成缓存无效。

### Automatic platform ARGs in the global scope
> 全局范围内的自动平台参数

This feature is only available when using the [BuildKit](#buildkit) backend.
> 此功能仅在使用BuildKit后端时可用。

Docker predefines a set of `ARG` variables with information on the platform of
the node performing the build (build platform) and on the platform of the
resulting image (target platform). The target platform can be specified with
the `--platform` flag on `docker build`.
> Docker预定义了一组`ARG`变量，其中包含执行构建的节点的平台（构建平台）和生成镜像的平台（目标平台）上的信息。
> 目标平台可以在`docker build`上用`--platform`标志指定。

The following `ARG` variables are set automatically:
> 以下ARG变量是自动设置的:

- `TARGETPLATFORM` - platform of the build result. Eg `linux/amd64`, `linux/arm/v7`, `windows/amd64`.
- `TARGETOS` - OS component of TARGETPLATFORM
- `TARGETARCH` - architecture component of TARGETPLATFORM
- `TARGETVARIANT` - variant component of TARGETPLATFORM
- `BUILDPLATFORM` - platform of the node performing the build.
- `BUILDOS` - OS component of BUILDPLATFORM
- `BUILDARCH` - architecture component of BUILDPLATFORM
- `BUILDVARIANT` - variant component of BUILDPLATFORM

These arguments are defined in the global scope so are not automatically
available inside build stages or for your `RUN` commands. To expose one of
these arguments inside the build stage redefine it without value.
> 这些参数是在全局范围内定义的，因此在生成阶段或`RUN`指令中不会自动可用。
> 要在构建阶段中公开这些参数之一，请在没有值的情况下重新定义它。

For example:

```dockerfile
FROM alpine
ARG TARGETPLATFORM
RUN echo "I'm building for $TARGETPLATFORM"
```

### Impact on build caching
> 对生成缓存的影响

`ARG` variables are not persisted into the built image as `ENV` variables are.
However, `ARG` variables do impact the build cache in similar ways. If a
Dockerfile defines an `ARG` variable whose value is different from a previous
build, then a "cache miss" occurs upon its first usage, not its definition. In
particular, all `RUN` instructions following an `ARG` instruction use the `ARG`
variable implicitly (as an environment variable), thus can cause a cache miss.
All predefined `ARG` variables are exempt from caching unless there is a
matching `ARG` statement in the `Dockerfile`.
> `ARG`变量不会像`ENV`变量一样持久化到内置镜像中。但是，ARG变量确实以类似的方式影响构建缓存。
> 如果Dockerfile定义了一个`ARG`变量，该变量的值不同于以前的构建，那么在第一次使用时会发生`缓存未命中`，而不是它的定义。
> 特别是，`ARG`指令后面的所有`RUN`指令都隐式地使用`ARG`变量（作为环境变量），因此可能会导致缓存未命中。
> 除非`Dockerfile`中有匹配的`ARG`声明，否则所有预定义的`ARG`变量都不会缓存。

For example, consider these two Dockerfile:
> 例如，考虑这两个 Dockerfile：

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
RUN echo $CONT_IMG_VER
```

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
RUN echo hello
```

If you specify `--build-arg CONT_IMG_VER=<value>` on the command line, in both
cases, the specification on line 2 does not cause a cache miss; line 3 does
cause a cache miss.`ARG CONT_IMG_VER` causes the RUN line to be identified
as the same as running `CONT_IMG_VER=<value> echo hello`, so if the `<value>`
changes, we get a cache miss.
> 如果在命令行上指定`--build-arg CONT_IMG_VER=<value>`，在这两种情况下，
> 第2行上的规范都不会导致缓存未命中；第3行会导致缓存未命中。
> `ARG CONT_IMG_VER`导致RUN line被标识为与`CONT_IMG_VER=<value> echo hello`相同，
> 因此如果`<value>`发生更改，我们将获得缓存未命中。

Consider another example under the same command line:
> 考虑同一命令行下的另一个示例:

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
ENV CONT_IMG_VER=$CONT_IMG_VER
RUN echo $CONT_IMG_VER
```

In this example, the cache miss occurs on line 3. The miss happens because
the variable's value in the `ENV` references the `ARG` variable and that
variable is changed through the command line. In this example, the `ENV`
command causes the image to include the value.
> 在本例中，缓存未命中发生在第3行。未命中的原因是`ENV`中的变量值引用了`ARG`变量，并且该变量通过命令行更改。
> 在本例中，ENV命令使镜像包含该值。

If an `ENV` instruction overrides an `ARG` instruction of the same name, like
this Dockerfile:
> 如果`ENV`指令重写同名的`ARG`指令，如以下Dockerfile：

```dockerfile
FROM ubuntu
ARG CONT_IMG_VER
ENV CONT_IMG_VER=hello
RUN echo $CONT_IMG_VER
```

Line 3 does not cause a cache miss because the value of `CONT_IMG_VER` is a
constant (`hello`). As a result, the environment variables and values used on
the `RUN` (line 4) doesn't change between builds.
> 第3行不会导致缓存未命中，因为`CONT_IMG_VER`的值是常量（`hello`）。
> 因此，`RUN`使用的环境变量和值（第4行）在构建之间不会更改。

## ONBUILD

```dockerfile
ONBUILD <INSTRUCTION>
```

The `ONBUILD` instruction adds to the image a *trigger* instruction to
be executed at a later time, when the image is used as the base for
another build. The trigger will be executed in the context of the
downstream build, as if it had been inserted immediately after the
`FROM` instruction in the downstream `Dockerfile`.
> `ONBUILD`指令向镜像添加一条*触发器*指令，当镜像用作另一个生成的基础时，该指令将在稍后执行。
> 触发器将在下游构建的上下文中执行，就好像它是在下游`Dockerfile`中的`FROM`指令之后立即插入的一样。

Any build instruction can be registered as a trigger.
> 任何构建指令都可以注册为触发器。

This is useful if you are building an image which will be used as a base
to build other images, for example an application build environment or a
daemon which may be customized with user-specific configuration.
> 如果您正在构建一个镜像，该镜像将用作构建其他镜像的基础，
> 例如应用程序构建环境或可以使用特定于用户的配置自定义的守护进程，那么这将非常有用。

For example, if your image is a reusable Python application builder, it
will require application source code to be added in a particular
directory, and it might require a build script to be called *after*
that. You can't just call `ADD` and `RUN` now, because you don't yet
have access to the application source code, and it will be different for
each application build. You could simply provide application developers
with a boilerplate `Dockerfile` to copy-paste into their application, but
that is inefficient, error-prone and difficult to update because it
mixes with application-specific code.
> 例如，如果您的镜像是一个可重用的Python应用程序生成器，
> 则需要将应用程序源代码添加到特定目录中，并且可能需要在此之后调用构建脚本。
> 您不能现在就调用`ADD`和`RUN`，因为您还没有访问应用程序源代码的权限，而且每个应用程序构建的源代码都是不同的。
> 您可以简单地为应用程序开发人员提供一个`Dockerfile`样板文件，以便将粘贴复制到他们的应用程序中，
> 但这样做效率低，容易出错，而且很难更新，因为它与特定于应用程序的代码混合在一起。

The solution is to use `ONBUILD` to register advance instructions to
run later, during the next build stage.
> 解决方案是使用`ONBUILD`注册高级指令，以便稍后在下一个构建阶段运行。

Here's how it works:

1. When it encounters an `ONBUILD` instruction, the builder adds a
   trigger to the metadata of the image being built. The instruction
   does not otherwise affect the current build.
   > 当遇到`ONBUILD`指令时，生成器会向正在生成的镜像的元数据添加触发器。该指令不会影响当前生成。
2. At the end of the build, a list of all triggers is stored in the
   image manifest, under the key `OnBuild`. They can be inspected with
   the `docker inspect` command.
   > 在构建结束时，所有触发器的列表存储在镜像清单的`OnBuild`键下。它们可以通过`docker inspect`命令进行检查。
3. Later the image may be used as a base for a new build, using the
   `FROM` instruction. As part of processing the `FROM` instruction,
   the downstream builder looks for `ONBUILD` triggers, and executes
   them in the same order they were registered. If any of the triggers
   fail, the `FROM` instruction is aborted which in turn causes the
   build to fail. If all triggers succeed, the `FROM` instruction
   completes and the build continues as usual.
   > 稍后，可以使用`FROM`指令将该镜像用作新构建的基础。
   > 作为处理`FROM`指令的一部分，下游构建器查找`ONBUILD`触发器，并按照注册的顺序执行它们。
   > 如果任何触发器失败，`FROM`指令将被中止，从而导致生成失败。如果所有触发器都成功，则`FROM`指令完成，构建照常继续。
4. Triggers are cleared from the final image after being executed. In
   other words they are not inherited by "grand-children" builds.
   > 触发器在执行后从最终镜像中清除。换句话说，它们不是由“孙子”继承的。

For example you might add something like this:
> 例如，您可以添加如下内容

```dockerfile
ONBUILD ADD . /app/src
ONBUILD RUN /usr/local/bin/python-build --dir /app/src
```

> **Warning**
>
> Chaining `ONBUILD` instructions using `ONBUILD ONBUILD` isn't allowed.

> **Warning**
>
> The `ONBUILD` instruction may not trigger `FROM` or `MAINTAINER` instructions.

## STOPSIGNAL

```dockerfile
STOPSIGNAL signal
```

The `STOPSIGNAL` instruction sets the system call signal that will be sent to the container to exit.
This signal can be a valid unsigned number that matches a position in the kernel's syscall table, for instance 9,
or a signal name in the format SIGNAME, for instance SIGKILL.
> `STOPSIGNAL`指令设置将发送到容器以退出的系统调用信号。
> 此信号可以是与内核syscall表中的位置匹配的有效无符号数字，例如9，或者SIGNAME中的信号名，例如SIGKILL。

## HEALTHCHECK

The `HEALTHCHECK` instruction has two forms:
> `HEALTHCHECK`指令有两种形式：

- `HEALTHCHECK [OPTIONS] CMD command` (check container health by running a command inside the container)
- `HEALTHCHECK NONE` (disable any healthcheck inherited from the base image)

The `HEALTHCHECK` instruction tells Docker how to test a container to check that
it is still working. This can detect cases such as a web server that is stuck in
an infinite loop and unable to handle new connections, even though the server
process is still running.
> `HEALTHCHECK`指令告诉Docker如何测试容器以检查它是否仍在工作。
> 这可以检测到一些情况，例如web服务器卡在无限循环中，无法处理新连接，即使服务器进程仍在运行。

When a container has a healthcheck specified, it has a _health status_ in
addition to its normal status. This status is initially `starting`. Whenever a
health check passes, it becomes `healthy` (whatever state it was previously in).
After a certain number of consecutive failures, it becomes `unhealthy`.
> 当容器指定了healthcheck时，除了正常状态外，它还具有健康状态。此状态最初处于`starting`。
> 每当健康检查通过时，它就会变为`healthy`（不管它以前处于什么状态）。经过一定数量的连续失败，它变为`unhealthy`。

The options that can appear before `CMD` are:
> 可以在CMD之前出现的选项有：

- `--interval=DURATION` (default: `30s`)
- `--timeout=DURATION` (default: `30s`)
- `--start-period=DURATION` (default: `0s`)
- `--retries=N` (default: `3`)

The health check will first run **interval** seconds after the container is
started, and then again **interval** seconds after each previous check completes.
> 运行状况检查将首先在容器启动后的**interval**秒内运行，然后在前一次检查完成后的**interval**秒内再次运行。

If a single run of the check takes longer than **timeout** seconds then the check
is considered to have failed.
> 如果单次运行检查所需时间超过**timeout**秒，则认为检查失败。

It takes **retries** consecutive failures of the health check for the container
to be considered `unhealthy`.
> 如果运行状况检查连续失败，则需要重试一次才能将容器视为`unhealthy`。

**start period** provides initialization time for containers that need time to bootstrap.
Probe failure during that period will not be counted towards the maximum number of retries.
However, if a health check succeeds during the start period, the container is considered
started and all consecutive failures will be counted towards the maximum number of retries.
> **start period**为需要时间引导的容器提供初始化时间。在此期间的探测失败将不计入最大重试次数。
> 但是，如果运行状况检查在启动期间成功，则认为容器已启动，所有连续失败都将计入最大重试次数。

There can only be one `HEALTHCHECK` instruction in a Dockerfile. If you list
more than one then only the last `HEALTHCHECK` will take effect.
> Dockerfile中只能有一条`HEALTHCHECK`指令。如果您列出多个，那么只有最后一个`HEALTHCHECK`才会生效。

The command after the `CMD` keyword can be either a shell command (e.g. `HEALTHCHECK
CMD /bin/check-running`) or an _exec_ array (as with other Dockerfile commands;
see e.g. `ENTRYPOINT` for details).
> CMD关键字后面的命令可以是shell命令（例如`HEALTHCHECK CMD /bin/check-running`）
> 或exec数组（与其他Dockerfile命令一样；有关详细信息，请参见`ENTRYPOINT`）。

The command's exit status indicates the health status of the container.
The possible values are:
> 命令的退出状态指示容器的运行状况。可能的值为：

- 0: success - the container is healthy and ready for use
- 1: unhealthy - the container is not working correctly
- 2: reserved - do not use this exit code

For example, to check every five minutes or so that a web-server is able to
serve the site's main page within three seconds:
> 例如，每五分钟左右检查一次，以便web服务器能够在三秒内为网站主页提供服务：

```dockerfile
HEALTHCHECK --interval=5m --timeout=3s \
  CMD curl -f http://localhost/ || exit 1
```

To help debug failing probes, any output text (UTF-8 encoded) that the command writes
on stdout or stderr will be stored in the health status and can be queried with
`docker inspect`. Such output should be kept short (only the first 4096 bytes
are stored currently).
> 为了帮助调试失败的探测，命令在stdout或stderr上写入的任何输出文本（UTF-8编码）都将存储在运行状况中，
> 并且可以使用`docker inspect`进行查询。这样的输出应该保持短（当前只存储前4096个字节）。

When the health status of a container changes, a `health_status` event is
generated with the new status.
> 当容器的运行状况发生更改时，一个带有新状态的`health_status`事件将生成。


## SHELL

```dockerfile
SHELL ["executable", "parameters"]
```

The `SHELL` instruction allows the default shell used for the *shell* form of
commands to be overridden. The default shell on Linux is `["/bin/sh", "-c"]`, and on
Windows is `["cmd", "/S", "/C"]`. The `SHELL` instruction *must* be written in JSON
form in a Dockerfile.
> `SHELL`指令允许重写命令的shell形式所使用的默认shell。
> Linux上的默认shell是`["/bin/sh", "-c"]`，Windows上的默认shell是`["cmd", "/S", "/C"]`。
> SHELL指令必须以JSON格式写入Dockerfile

The `SHELL` instruction is particularly useful on Windows where there are
two commonly used and quite different native shells: `cmd` and `powershell`, as
well as alternate shells available including `sh`.
> `SHELL`指令在Windows上特别有用，在Windows上有两种常用的、完全不同的本机shells:`cmd`和`powershell`，以及可选shell，包括`sh`。

The `SHELL` instruction can appear multiple times. Each `SHELL` instruction overrides
all previous `SHELL` instructions, and affects all subsequent instructions. For example:
> `SHELL`指令可以出现多次。每一条`SHELL`指令都会覆盖所有以前的`SHELL`指令，并影响所有后续指令。例如：

```dockerfile
FROM microsoft/windowsservercore

# Executed as cmd /S /C echo default
RUN echo default

# Executed as cmd /S /C powershell -command Write-Host default
RUN powershell -command Write-Host default

# Executed as powershell -command Write-Host hello
SHELL ["powershell", "-command"]
RUN Write-Host hello

# Executed as cmd /S /C echo hello
SHELL ["cmd", "/S", "/C"]
RUN echo hello
```

The following instructions can be affected by the `SHELL` instruction when the
*shell* form of them is used in a Dockerfile: `RUN`, `CMD` and `ENTRYPOINT`.
> 当在Dockerfile中使用*shell*形式时，以下指令可能会受到`SHELL`指令的影响：`RUN`、`CMD`和`ENTRYPOINT`。

The following example is a common pattern found on Windows which can be
streamlined by using the `SHELL` instruction:
> 下面的示例是Windows上的常见模式，可以使用`SHELL`指令对其进行优化：

```dockerfile
RUN powershell -command Execute-MyCmdlet -param1 "c:\foo.txt"
```

The command invoked by docker will be:
> docker调用的命令是：

```powershell
cmd /S /C powershell -command Execute-MyCmdlet -param1 "c:\foo.txt"
```

This is inefficient for two reasons. First, there is an un-necessary cmd.exe command
processor (aka shell) being invoked. Second, each `RUN` instruction in the *shell*
form requires an extra `powershell -command` prefixing the command.
> 这是效率低下的两个原因。首先，有一个不必要的cmd.exe正在调用的命令处理器（又名shell）。
> 其次，shell形式的每个`RUN`指令都需要一个额外的`powershell -command`作为该命令的前缀。

To make this more efficient, one of two mechanisms can be employed. One is to
use the JSON form of the RUN command such as:
> 为了提高效率，可以采用两种机制之一。一种是使用RUN命令的JSON格式，例如：

```dockerfile
RUN ["powershell", "-command", "Execute-MyCmdlet", "-param1 \"c:\\foo.txt\""]
```

While the JSON form is unambiguous and does not use the un-necessary cmd.exe,
it does require more verbosity through double-quoting and escaping. The alternate
mechanism is to use the `SHELL` instruction and the *shell* form,
making a more natural syntax for Windows users, especially when combined with
the `escape` parser directive:
> 而JSON形式是明确的，没有使用非必要的cmd.exe, 它确实更加冗长因为需要通过双重引用和转义。
> 另一种机制是使用`SHELL`指令和*shell*形式，为Windows用户提供更自然的语法，特别是与escape parser指令结合使用时：

```dockerfile
# escape=`

FROM microsoft/nanoserver
SHELL ["powershell","-command"]
RUN New-Item -ItemType Directory C:\Example
ADD Execute-MyCmdlet.ps1 c:\example\
RUN c:\example\Execute-MyCmdlet -sample 'hello world'
```

Resulting in:

```powershell
PS E:\docker\build\shell> docker build -t shell .
Sending build context to Docker daemon 4.096 kB
Step 1/5 : FROM microsoft/nanoserver
 ---> 22738ff49c6d
Step 2/5 : SHELL powershell -command
 ---> Running in 6fcdb6855ae2
 ---> 6331462d4300
Removing intermediate container 6fcdb6855ae2
Step 3/5 : RUN New-Item -ItemType Directory C:\Example
 ---> Running in d0eef8386e97


    Directory: C:\


Mode                LastWriteTime         Length Name
----                -------------         ------ ----
d-----       10/28/2016  11:26 AM                Example


 ---> 3f2fbf1395d9
Removing intermediate container d0eef8386e97
Step 4/5 : ADD Execute-MyCmdlet.ps1 c:\example\
 ---> a955b2621c31
Removing intermediate container b825593d39fc
Step 5/5 : RUN c:\example\Execute-MyCmdlet 'hello world'
 ---> Running in be6d8e63fe75
hello world
 ---> 8e559e9bf424
Removing intermediate container be6d8e63fe75
Successfully built 8e559e9bf424
PS E:\docker\build\shell>
```

The `SHELL` instruction could also be used to modify the way in which
a shell operates. For example, using `SHELL cmd /S /C /V:ON|OFF` on Windows, delayed
environment variable expansion semantics could be modified.
> `SHELL`指令还可以用来修改shell的操作方式。
> 例如，在Windows上使用`SHELL cmd /S /C /V:ON|OFF`，可以修改延迟的环境变量扩展语义。

The `SHELL` instruction can also be used on Linux should an alternate shell be
required such as `zsh`, `csh`, `tcsh` and others.
> 如果需要另一个SHELL，如`zsh`、`csh`、`tcsh`和其他，也可以在Linux上使用`SHELL`指令。

## External implementation features

This feature is only available when using the  [BuildKit](#buildkit) backend.
> 此功能仅在使用BuildKit后端时可用。

Docker build supports experimental features like cache mounts, build secrets and
ssh forwarding that are enabled by using an external implementation of the
builder with a syntax directive. To learn about these features,
[refer to the documentation in BuildKit repository](https://github.com/moby/buildkit/blob/master/frontend/dockerfile/docs/experimental.md).
> Docker build支持实验性功能，如缓存装载、构建机密和ssh转发，这些功能是通过使用带有语法指令的生成器的外部实现启用的。

## Dockerfile examples

Below you can see some examples of Dockerfile syntax.

```dockerfile
# Nginx
#
# VERSION               0.0.1

FROM      ubuntu
LABEL Description="This image is used to start the foobar executable" Vendor="ACME Products" Version="1.0"
RUN apt-get update && apt-get install -y inotify-tools nginx apache2 openssh-server
```

```dockerfile
# Firefox over VNC
#
# VERSION               0.3

FROM ubuntu

# Install vnc, xvfb in order to create a 'fake' display and firefox
RUN apt-get update && apt-get install -y x11vnc xvfb firefox
RUN mkdir ~/.vnc
# Setup a password
RUN x11vnc -storepasswd 1234 ~/.vnc/passwd
# Autostart firefox (might not be the best way, but it does the trick)
RUN bash -c 'echo "firefox" >> /.bashrc'

EXPOSE 5900
CMD    ["x11vnc", "-forever", "-usepw", "-create"]
```

```dockerfile
# Multiple images example
#
# VERSION               0.1

FROM ubuntu
RUN echo foo > bar
# Will output something like ===> 907ad6c2736f

FROM ubuntu
RUN echo moo > oink
# Will output something like ===> 695d7793cbe4

# You'll now have two images, 907ad6c2736f with /bar, and 695d7793cbe4 with
# /oink.
```