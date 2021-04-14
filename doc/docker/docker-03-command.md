##image相关指令

- ###docker image ls  
    描述：列出所有镜像信息。  
    用法：`docker image ls [OPTIONS] [REPOSITORY[:TAG]]`  
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
    用法：`docker image inspect [OPTIONS] IMAGE [IMAGE...]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--format , -f|使用go模板格式化输出|

- ###docker image pull
    描述：从registry中拉去镜像  
    用法：`docker image pull [OPTIONS] NAME[:TAG|@DIGEST]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--all-tags , -a|下载仓库中所有标记的镜像|
    |--disable-content-trust|跳过镜像验证（默认true）|
    |--platform|如果服务支持多平台则设置平台（API 1.32+）|
    |--quiet , -q|抑制详细输出| 
	
- ###docker image push
    描述：将镜像推送到registry  
    用法：`docker image push [OPTIONS] NAME[:TAG]`  
    OPTIONS：  
    
    |名称，速记|描述|
    |----|----|
    |--all-tags , -a|推送仓库中所有标记的镜像|
    |--disable-content-trust|跳过镜像签名（默认true）|
    |--quiet , -q|抑制详细输出| 

- ###docker image rm
  描述：移除一个或多个镜像  
  用法：`docker image rm [OPTIONS] IMAGE [IMAGE...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--force , -f|强制删除镜像|
  |--no-prune|Do not delete untagged parents|

- ###docker image history
  描述：显示一个镜像的历史数据  
  用法：`docker image history [OPTIONS] IMAGE`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--format , -f|使用go模板格式化输出|
  |--human , -H|以可读方式显示尺寸和日期（默认true）|
  |--no-trunc|不截断输出|
  |--quiet , -q|只显示镜像ID|

- ###docker image tag
  描述：创建一个引用SOURCE_IMAGE的标记TARGET_IMAGE  
  用法：`docker image tag SOURCE_IMAGE[:TAG] TARGET_IMAGE[:TAG]`  
  OPTIONS：无
  
- ###docker image save
  描述：将一个或多个镜像保存到tar存档  
  用法：`docker image save [OPTIONS] IMAGE [IMAGE...]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--output , -o|写入文件，而不是STDOUT|
  
- ###docker image load
  描述：从tar存档或者STDIN加载镜像  
  用法：`docker image load [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--input , -i|从tar存档文件读取，而不是STDIN|
  |--quiet , -q|不现实加载过程的输出|
  

##容器相关指令

- ###docker ps
  描述：列出所有容器（默认只显示运行中的）  
  用法：`docker ps [OPTIONS]`  
  OPTIONS：

  |名称，速记|描述|
  |----|----|
  |--all , -a|显示所有容器（默认只显示运行中的）|
  |--filter , -f|根据提供的条件过滤输出|
  |--format|使用go模板打印信息|
  |--last , -n|显示n个容器（包括所有状态的）|
  |--latest , -l|显示最后创建的容器（包含所有状态的）|
  |||
  |||
  |||