官网上region的概念是AWS的东西。
region和zone在源码中也无处不在，先简单有个概念，后续再源码中再领会吧。  
zone： 可以理解为一个小的区域范围，比如说一个机房，一个zone里我们可以部署多台eureka server。  
region： 可以理解为比zone个大的一个区域范围，比如说北京、上海。  

如果说我们北京有两个机房，上海有两个机房，可以使用两个region：region-beijing、region-shanghai  
每个region里可以配置两个zone：beijing-zone1、beijing-zone2; shanghai-zone1、shanghai-zone2  
每个zone又可以部署多台eureka server服务。  

简单看下shanghai-zone1的配置：  
```
eureka:
  instance:
    instance-id: ${spring.cloud.client.ip-address}:${server.port} #euraka实例列表中显示的id
    hostname: eureka-server-shanghai-zone1-7001.com #eureka服务端的实例名称，DS Replicas栏显示的名称
  client:
    #该应用为注册中心，设置为false表示不向注册中心注册自己，集群配置是必须设置为true
    register-with-eureka: true
    #该应用为注册中心，设置为false表示不需要要去检索服务，集群配置是必须设置为true
    fetch-registry: true
    region: shanghai #该服务所在的region
    prefer-same-zone-eureka: true #是否优先和相同zone的server进行交互
    availability-zones:
      shanghai: shanghai-zone-1,shanghai-zone-2 #第一个就代表自己所在的zone
    service-url:
      shanghai-zone-1: http://eureka-server-shanghai-zone1-7001.com:7001/eureka/ #同一个zone里也可以配置多个，逗号分隔即可
      shanghai-zone-2: http://eureka-server-shanghai-zone2-7002.com:7002/eureka/
  server:
    remoteRegionUrlsWithName:
      beijing: http://eureka-server-beijing-zone1-7003.com:7003/eureka/ #其它region的名称和地址映射
```

1. 服务通过eureka-client向eureka-server发起租约管理的时候，eureka-server收到请求后会同步的将信息复制给和自己同一个region的同等节点。  
2. 多个region之间也会同步数据，通过RemoteRegionRegistry进行同步保存。  
3. 客户端向自己想通region的server发送获取Applications请求时，可以通过参数控制同时获取哪些remoteRegions的Applications数据。  