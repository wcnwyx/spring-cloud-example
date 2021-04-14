##image相关指令

- ###docker image ls  
    描述：列出所有镜像信息。  
    用法：`$ docker image ls [OPTIONS] [REPOSITORY[:TAG]]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--all, -a| 显示所有镜像|
    |--digests| 显示摘要|
    |--filter , -f|根据提供的条件过滤输出|
    |--format|使用go模板打印信息|
    |--no-trunc|不要截断输出（主要是IMAGE_ID列一般会截断输出）|
    |--quiet , -q|只显示镜像的ID| 

- ###docker image inspect
    描述：显示一个或多个镜像的详细信息  
    用法：`$ docker image inspect [OPTIONS] IMAGE [IMAGE...]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--format , -f|使用go模板格式化输出|

- ###docker image pull
    描述：从registry中拉去镜像  
    用法：`$ docker image pull [OPTIONS] NAME[:TAG|@DIGEST]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--all-tags , -a|下载仓库中所有标记的镜像|
    |--disable-content-trust|跳过镜像验证（默认true）|
    |--platform|如果服务支持多平台则设置平台（API 1.32+）|
    |--quiet , -q|抑制详细输出| 
	
- ###docker image push
    描述：将镜像推送到registry  
    用法：`$ docker image push [OPTIONS] NAME[:TAG]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--all-tags , -a|推送仓库中所有标记的镜像|
    |--disable-content-trust|跳过镜像签名（默认true）|
    |--quiet , -q|抑制详细输出| 

- ###docker image rm / docker rmi
  描述：移除一个或多个镜像  
  用法：`$ docker image rm [OPTIONS] IMAGE [IMAGE...]`  
  `docker rmi [OPTIONS] IMAGE [IMAGE...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--force , -f|强制删除镜像|
  |--no-prune|Do not delete untagged parents|

- ###docker image history
  描述：显示一个镜像的历史数据  
  用法：`$ docker image history [OPTIONS] IMAGE`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--format , -f|使用go模板格式化输出|
  |--human , -H|以可读方式显示尺寸和日期（默认true）|
  |--no-trunc|不截断输出|
  |--quiet , -q|只显示镜像ID|

- ###docker image tag
  描述：创建一个引用SOURCE_IMAGE的标记TARGET_IMAGE  
  用法：`$ docker image tag SOURCE_IMAGE[:TAG] TARGET_IMAGE[:TAG]`  
  OPTIONS：无
  
- ###docker image save
  描述：将一个或多个镜像保存到tar存档  
  用法：`$ docker image save [OPTIONS] IMAGE [IMAGE...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--output , -o|写入文件，而不是STDOUT|
  
- ###docker image load
  描述：从tar存档或者STDIN加载镜像  
  用法：`$ docker image load [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--input , -i|从tar存档文件读取，而不是STDIN|
  |--quiet , -q|不现实加载过程的输出|
  

##容器相关指令

- ###docker ps
  描述：列出所有容器（默认只显示运行中的）  
  用法：`$ docker ps [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--all , -a|显示所有容器（默认只显示运行中的）|
  |--filter , -f|根据提供的条件过滤输出|
  |--format|使用go模板打印信息|
  |--last , -n|显示n个容器（包括所有状态的）|
  |--latest , -l|显示最后创建的容器（包含所有状态的）|
  |--no-trunc|不截断输出|
  |--quiet , -q|只显示容器ID|
  |--size , -s|显示总文件大小|
  
- ###docker create
  描述：创建一个新的容器（并不启动容器）  
  用法：`$ docker create [OPTIONS] IMAGE [COMMAND] [ARG...]`  
  OPTIONS：该指令可选项多且重要，后续单独一篇写，带上例子。

- ###docker start
  描述：启动一个或多个已停止的容器  
  用法：`$ docker stop [OPTIONS] CONTAINER [CONTAINER...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--attach , -a|连接STDOUT/STDERR和forward信号|
  |--detach-keys|重写用于离开容器的键序列（默认是Ctrl+P+Q）|
  |--interactive , -i|连接容器的STDIN|
  
- ###docker pause
  描述：暂停一个或多个容器中的所有进程  
  用法：`$ docker pause CONTAINER [CONTAINER...]`  
  OPTIONS：无
 
- ###docker unpause
  描述：取消暂停一个或多个容器中的所有进程  
  用法：`$ docker unpause CONTAINER [CONTAINER...]`  
  OPTIONS：无
  
- ###docker stop
  描述：停止一个或多个运行中的容器  
  用法：`$ docker start [OPTIONS] CONTAINER [CONTAINER...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--time , -t|在杀死前等待停止的秒数（默认10秒）|
  
- ###docker run
  描述：在新容器中运行指令（相当于create+start）  
  用法：`$ docker create [OPTIONS] IMAGE [COMMAND] [ARG...]`  
  OPTIONS：该指令可选项多且重要，后续单独一篇写，带上例子。
  
- ###docker rm
  描述：移除一个或多个容器  
  用法：`$ docker rm [OPTIONS] CONTAINER [CONTAINER...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--force , -f|强制移除正在运行的容器|
  |--link , -l|删除制定的链接|
  |--volumes , -v|删除与容器关联的匿名卷|
  
- ###docker kill
  描述：杀死一个或多个运行中的容器  
  用法：`$ docker kill [OPTIONS] CONTAINER [CONTAINER...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--signal , -s|向容器发送信号（默认是KILL）|
  

##卷相关

- ###docker volume create
  描述：创建卷  
  用法：`$ docker volume create [OPTIONS] [VOLUME]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--driver , -d|指定卷驱动程序名称|
  |--label|设置卷的元数据|
  |--name|制定卷名|
  |--opt , -o|设置特定于驱动程序的选项|
  
- ###docker volume inspect
  描述：显示一个或多个卷的详细信息  
  用法：`$ docker volume inspect [OPTIONS] VOLUME [VOLUME...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--format , -f|使用指定的GO模板格式化输出|
  
- ###docker volume ls
  描述：列出卷  
  用法：`$ docker volume ls [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--filter , -f|提供筛选器值（例如 ‘dangling=true‘）|
  |--format|使用Go模板打印卷|
  |--quiet , -q|仅显示卷名|
  
- ###docker volume prune
  描述：删除所有未使用的本地卷  
  用法：`$ docker volume prune [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--filter|提供筛选器值（例如 'label=<label>'）|
  |--force , -f|不提示确认|
  
- ###docker volume rm
  描述：移除一个或多个卷  
  用法：`$ docker volume rm [OPTIONS] VOLUME [VOLUME...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--force , -f|强制移除（API 1.25+）|