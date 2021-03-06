##自我保护的概念
先看一下eureka官方对这个概念的描述，再根据代码来看下。  
[官方定义地址](https://github.com/Netflix/eureka/wiki/Server-Self-Preservation-Mode)

> Eureka servers will enter self preservation mode if they detect that a larger than expected number of registered clients have terminated their connections in an ungraceful way, and are pending eviction at the same time. This is done to ensure catastrophic network events do not wipe out eureka registry data, and having this be propagated downstream to all clients.

如果Eureka服务器检测到超过预期数量的已注册客户端以不公平的方式终止了它们的连接，并且正在等待收回，那么它们将进入自我保护模式。这样做是为了确保灾难性的网络事件不会清除eureka注册表数据，并将其传播到下游的所有客户端

> To better understand self preservation, it help to first understand how does eureka clients 'end' their registration lifecycle. The eureka protocol requires clients to execute an explicit unregister action when they are permanently going away. For example, in the provided java client, this is done in the shutdown() method. any clients that fails 3 consecutive heartbeat renewals is considered to have an unclean termination, and will be evicted by the background eviction process. It is when > 15% of the current registry is in this later state, that self preservation will be enabled.

为了更好地理解自我保护，首先了解eureka客户机如何“结束”其注册生命周期很有帮助。eureka协议要求客户端在永久离开时执行显式的注销操作。例如，在提供的java客户机中，这是在shutdown（）方法中完成的。任何连续3次心跳更新失败的客户端都将被视为具有不干净的终止，并将被后台逐出进程逐出。当当前注册表的>15%处于稍后的状态时，将启用自我保护。

> When in self preservation mode, eureka servers will stop eviction of all instances until either:
  
  1. the number of heartbeat renewals it sees is back above the expected threshold, or
  2. self preservation is disabled (see below)
  
当处于自我保护模式时，eureka服务器将停止逐出所有实例，直到已下两种情况发生：
1. 心跳更新次数重新高于预期阈值
2. 自我保护被禁用

>  Self preservation is enabled by default, and the default threshold for enabling self preservation is > 15% of the current registry size.

默认情况下启用自我保护，并且启用自我保护的默认阈值大于当前注册表大小的15%。


##源码分析
源码只展示一些相关的参数和代码， 如下所示：  
```java
public abstract class AbstractInstanceRegistry implements InstanceRegistry {
    //记录上一分钟续约请求的个数
    private final MeasuredRate renewsLastMin;
    //每分钟续约请求的最小阀值
    protected volatile int numberOfRenewsPerMinThreshold;
    //期待有多少个客户端来发送续约请求
    protected volatile int expectedNumberOfClientsSendingRenews;

    //注册实例
    public void register(InstanceInfo registrant, int leaseDuration, boolean isReplication) {
        //省略部分代码。。。

        //新的实例来进行注册时，将expectedNumberOfClientsSendingRenews加1，默认值是1
        this.expectedNumberOfClientsSendingRenews = this.expectedNumberOfClientsSendingRenews + 1;
        
        //更新每分钟续约请求的最小阀值
        updateRenewsPerMinThreshold();

        //省略部分代码。。。
    }

    //驱逐过期实例
    public void evict(long additionalLeaseMs) {
        logger.debug("Running the evict task");

        //该方法再PeerAwareInstanceRegistryImpl中定义了，后面有介绍
        //如果说开启了自我保护模式，并且上一分钟心跳续约个数小于阀值，那么就是要自我保护了，就不允许方法发驱逐实例。
        if (!isLeaseExpirationEnabled()) {
            logger.debug("DS: lease expiration is currently disabled.");
            return;
        }

        //省略部分代码...
    }
    
    //更新每分钟续约请求的最小阀值(register、cancle和定时器都会调用该方法)
    protected void updateRenewsPerMinThreshold() {
        //serverConfig.getExpectedClientRenewalIntervalSeconds() 这个参数是客户端每隔几秒进行一次续约，默认是30s
        //serverConfig.getRenewalPercentThreshold() 这个参数是自我保护模式启动的阀值，默认是0.85
        
        //情况1： 刚启动一个eureka服务，并且没有注册任何实例进来（自己也不注册），
        //expectedNumberOfClientsSendingRenews是1，那么numberOfRenewsPerMinThreshold计算出来的结果就是1
        //但是因为没有注册任何实例，也就不会收到续约请求，过一段时间eureka管理页面上就可以看到进入了自我保护模式。
        
        //情况2：比如所现在就1个实例注册了进来，那么expectedNumberOfClientsSendingRenews=2，numberOfRenewsPerMinThreshold计算出来的结果就是3，
        //因为只有一个客户端进行了注册，30秒一个心跳续约请求，一分钟智能收到2此心跳续约请求，所以也会进入自我保护模式。
        this.numberOfRenewsPerMinThreshold = (int) (this.expectedNumberOfClientsSendingRenews
                * (60.0 / serverConfig.getExpectedClientRenewalIntervalSeconds())
                * serverConfig.getRenewalPercentThreshold());
    }


}

public class PeerAwareInstanceRegistryImpl extends AbstractInstanceRegistry implements PeerAwareInstanceRegistry {

    @Override
    public void init(PeerEurekaNodes peerEurekaNodes) throws Exception {
        //初始化时启动更新每分钟续约请求的最小阀值的定时任务
        scheduleRenewalThresholdUpdateTask();
    }
    
    /**
     * Schedule the task that updates <em>renewal threshold</em> periodically.
     * The renewal threshold would be used to determine if the renewals drop
     * dramatically because of network partition and to protect expiring too
     * many instances at a time.
     * 启动定时任务来更新续约的阀值
     */
    private void scheduleRenewalThresholdUpdateTask() {
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               updateRenewalThreshold();
                           }
                       }, serverConfig.getRenewalThresholdUpdateIntervalMs(),
                serverConfig.getRenewalThresholdUpdateIntervalMs());
    }

    /**
     * Updates the <em>renewal threshold</em> based on the current number of
     * renewals. The threshold is a percentage as specified in
     * {@link EurekaServerConfig#getRenewalPercentThreshold()} of renewals
     * received per minute {@link #getNumOfRenewsInLastMin()}.
     * 重新计算当前期望发送续约请求数量参数expectedNumberOfClientsSendingRenews，然后更新每分钟续约请求的阀值
     */
    private void updateRenewalThreshold() {
        try {
            Applications apps = eurekaClient.getApplications();
            int count = 0;
            for (Application app : apps.getRegisteredApplications()) {
                for (InstanceInfo instance : app.getInstances()) {
                    if (this.isRegisterable(instance)) {
                        ++count;
                    }
                }
            }
            synchronized (lock) {
                // Update threshold only if the threshold is greater than the
                // current expected threshold or if self preservation is disabled.
                if ((count) > (serverConfig.getRenewalPercentThreshold() * expectedNumberOfClientsSendingRenews)
                        || (!this.isSelfPreservationModeEnabled())) {
                    this.expectedNumberOfClientsSendingRenews = count;
                    updateRenewsPerMinThreshold();
                }
            }
            logger.info("Current renewal threshold is : {}", numberOfRenewsPerMinThreshold);
        } catch (Throwable e) {
            logger.error("Cannot update renewal threshold", e);
        }
    }


    //配置文件是否开启了自我保护模式
    @Override
    public boolean isSelfPreservationModeEnabled() {
        return serverConfig.shouldEnableSelfPreservation();
    }
    
    //是否允许租约过期失效，该方法在客户端获取InstanceInfo时和调用evict驱逐方法时使用。
    @Override
    public boolean isLeaseExpirationEnabled() {
        if (!isSelfPreservationModeEnabled()) {
            // The self preservation mode is disabled, hence allowing the instances to expire.
            //配置文件里设置的自我保护模式不可用，所以允许实例过期
            return true;
        }
        //上一分钟续约的个数大于阀值，说明没有进入自我保护模式，可以进行过期、驱逐
        return numberOfRenewsPerMinThreshold > 0 && getNumOfRenewsInLastMin() > numberOfRenewsPerMinThreshold;
    }

    @com.netflix.servo.annotations.Monitor(name = "isBelowRenewThreshold", description = "0 = false, 1 = true",
            type = com.netflix.servo.annotations.DataSourceType.GAUGE)
    @Override
    public int isBelowRenewThresold() {
        //判断续约请求是否低于阀值，也是eureka管理页面上使用的参数
        //这里的逻辑判断了启动时间和当前时间的时差，使用了这个配置参数serverConfig.getWaitTimeInMsWhenSyncEmpty()，默认是15分钟
        //主要用于eureka服务刚启动，还未从对等节点同步过来数据，设置的一个延迟时间。
        if ((getNumOfRenewsInLastMin() <= numberOfRenewsPerMinThreshold)
                &&
                ((this.startupTime > 0) && (System.currentTimeMillis() > this.startupTime + (serverConfig.getWaitTimeInMsWhenSyncEmpty())))) {
            return 1;
        } else {
            return 0;
        }
    }
}
```


下面是eureka管理页面的显示自我保护模式的一点逻辑，还是很简单的。   
```
<#if isBelowRenewThresold>
    <#if !registry.selfPreservationModeEnabled>
        <h4 id="uptime"><font size="+1" color="red"><b>RENEWALS ARE LESSER THAN THE THRESHOLD. THE SELF PRESERVATION MODE IS TURNED OFF. THIS MAY NOT PROTECT INSTANCE EXPIRY IN CASE OF NETWORK/OTHER PROBLEMS.</b></font></h4>
    <#else>
        <h4 id="uptime"><font size="+1" color="red"><b>EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT. RENEWALS ARE LESSER THAN THRESHOLD AND HENCE THE INSTANCES ARE NOT BEING EXPIRED JUST TO BE SAFE.</b></font></h4>
    </#if>
<#elseif !registry.selfPreservationModeEnabled>
    <h4 id="uptime"><font size="+1" color="red"><b>THE SELF PRESERVATION MODE IS TURNED OFF. THIS MAY NOT PROTECT INSTANCE EXPIRY IN CASE OF NETWORK/OTHER PROBLEMS.</b></font></h4>
</#if>
```