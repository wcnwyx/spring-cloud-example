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