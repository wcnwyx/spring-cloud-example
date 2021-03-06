PeerAwareInstanceRegistryImpl是AbstractInstanceRegistry的子类，也就具有了LeaseManager的功能。  
但是根据类名和它自身实现的接口PeerAwareInstanceRegistry可以看出来是具有同等节点意识的实例注册实现类，就是说需要处理eureka-server集群节点的逻辑。  

##先看下类的注释：
```java
/**
 * Handles replication of all operations to {@link AbstractInstanceRegistry} to peer
 * <em>Eureka</em> nodes to keep them all in sync.
 * 
 * 复制其父类（AbstractInstanceRegistry）的所有操作到Eureka的对等节点（就是集群节点）并且保持他们同步。
 *
 * <p>
 * Primary operations that are replicated are the
 * <em>Registers,Renewals,Cancels,Expirations and Status Changes</em>
 * </p>
 * 主要的复制操作有 Registers,Renewals,Cancels,Expirations and Status Changes
 *
 * <p>
 * When the eureka server starts up it tries to fetch all the registry
 * information from the peer eureka nodes.If for some reason this operation
 * fails, the server does not allow the user to get the registry information for
 * a period specified in
 * {@link com.netflix.eureka.EurekaServerConfig#getWaitTimeInMsWhenSyncEmpty()}.
 * </p>
 * 当eureka server启动时将尝试从其它对等节点获取所有的注册信息。
 * 如果因为某些原因导致这个操作失败了，该服务将在一定时间内不允许用户来获取注册信息，
 * 这个时间的长短定义在 com.netflix.eureka.EurekaServerConfig#getWaitTimeInMsWhenSyncEmpty()
 *
 * <p>
 * One important thing to note about <em>renewals</em>.If the renewal drops more
 * than the specified threshold as specified in
 * {@link com.netflix.eureka.EurekaServerConfig#getRenewalPercentThreshold()} within a period of
 * {@link com.netflix.eureka.EurekaServerConfig#getRenewalThresholdUpdateIntervalMs()}, eureka
 * perceives this as a danger and stops expiring instances.
 * </p>
 * 关于续约操作需要注意一个重要的事情。如果续约下降到特殊的阀值，eureka将意识到危险并停止实例的过期。就是自我保护模式的概念。
 *
 */
@Singleton
public class PeerAwareInstanceRegistryImpl extends AbstractInstanceRegistry implements PeerAwareInstanceRegistry {
    
}
```
根据类注释我们可以看到该类主要有三个方面的功能点：
1. 一些操作在同等节点之间的复制同步。
2. 启动时从同等节点获取注册信息。
3. 自我保护机制的一些操作。

##一些操作在同等节点之间的复制同步。
```java
public class PeerAwareInstanceRegistryImpl extends AbstractInstanceRegistry implements PeerAwareInstanceRegistry {
    //定义的动作
    public enum Action {
        Heartbeat, Register, Cancel, StatusUpdate, DeleteStatusOverride;

        private com.netflix.servo.monitor.Timer timer = Monitors.newTimer(this.name());

        public com.netflix.servo.monitor.Timer getTimer() {
            return this.timer;
        }
    }

    //计数器，记录上一分钟接收到的复制动作的次数
    private final MeasuredRate numberOfReplicationsLastMin;

    //所有Eureka对等节点的信息持有者，里面封装了多个PeerEurekaNode
    protected volatile PeerEurekaNodes peerEurekaNodes;
    
    @Override
    public boolean cancel(final String appName, final String id,
                          final boolean isReplication) {
        //调用父类AbstractInstanceRegistry的cancel操作做自己节点的实际cancel处理。
        if (super.cancel(appName, id, isReplication)) {
            //将cancel操作复制给其他对等节点
            replicateToPeers(Action.Cancel, appName, id, null, null, isReplication);
            synchronized (lock) {
                if (this.expectedNumberOfClientsSendingRenews > 0) {
                    //expectedNumberOfClientsSendingRenews 这个参数是记录有多少个客户端注册了上来需要发送续约请求的
                    //用于计算上一分钟续约请求数量是否低于阀值，用于判断是否进入自我保护模式用的
                    //客户端发来了cancel操作，这个参数就减1
                    this.expectedNumberOfClientsSendingRenews = this.expectedNumberOfClientsSendingRenews - 1;
                    //更新续约请求每分钟的阀值，因为expectedNumberOfClientsSendingRenews参数变了，需要重新计算
                    //这些在看自我保护模式的时候再详细看。
                    updateRenewsPerMinThreshold();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void register(final InstanceInfo info, final boolean isReplication) {
        int leaseDuration = Lease.DEFAULT_DURATION_IN_SECS;
        if (info.getLeaseInfo() != null && info.getLeaseInfo().getDurationInSecs() > 0) {
            leaseDuration = info.getLeaseInfo().getDurationInSecs();
        }
        //自身节点注册
        super.register(info, leaseDuration, isReplication);
        //复制给其他对等节点
        replicateToPeers(Action.Register, info.getAppName(), info.getId(), info, null, isReplication);
    }


    public boolean renew(final String appName, final String id, final boolean isReplication) {
        if (super.renew(appName, id, isReplication)) {
            replicateToPeers(Action.Heartbeat, appName, id, null, null, isReplication);
            return true;
        }
        return false;
    }

    @Override
    public boolean statusUpdate(final String appName, final String id,
                                final InstanceStatus newStatus, String lastDirtyTimestamp,
                                final boolean isReplication) {
        if (super.statusUpdate(appName, id, newStatus, lastDirtyTimestamp, isReplication)) {
            replicateToPeers(Action.StatusUpdate, appName, id, null, newStatus, isReplication);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteStatusOverride(String appName, String id,
                                        InstanceStatus newStatus,
                                        String lastDirtyTimestamp,
                                        boolean isReplication) {
        if (super.deleteStatusOverride(appName, id, newStatus, lastDirtyTimestamp, isReplication)) {
            replicateToPeers(Action.DeleteStatusOverride, appName, id, null, null, isReplication);
            return true;
        }
        return false;
    }

    /**
     * Replicates all eureka actions to peer eureka nodes except for replication
     * traffic to this node.
     * 复制所有eureka的动作到对等节点，除了这个操作本身就是到该节点的复制操作。
     */
    private void replicateToPeers(Action action, String appName, String id,
                                  InstanceInfo info /* optional */,
                                  InstanceStatus newStatus /* optional */, boolean isReplication) {
        Stopwatch tracer = action.getTimer().start();
        try {
            if (isReplication) {
                //如果是复制请求，记录下
                numberOfReplicationsLastMin.increment();
            }
            // If it is a replication already, do not replicate again as this will create a poison replication
            // 如果对等节点的信息为空，或者该次操作本身就是其他节点发过来的复制操作。
            if (peerEurekaNodes == Collections.EMPTY_LIST || isReplication) {
                return;
            }

            //循环复制到所有同等节点。
            for (final PeerEurekaNode node : peerEurekaNodes.getPeerEurekaNodes()) {
                // If the url represents this host, do not replicate to yourself.
                // 如果同等节点力存的host是自己的，跳过，不自己复制给自己。
                if (peerEurekaNodes.isThisMyUrl(node.getServiceUrl())) {
                    continue;
                }
                replicateInstanceActionsToPeers(action, appName, id, info, newStatus, node);
            }
        } finally {
            tracer.stop();
        }
    }

    /**
     * Replicates all instance changes to peer eureka nodes except for
     * replication traffic to this node.
     * 通过调用PeerEurekaNode类的方法将数据复制给peerNode
     */
    private void replicateInstanceActionsToPeers(Action action, String appName,
                                                 String id, InstanceInfo info, InstanceStatus newStatus,
                                                 PeerEurekaNode node) {
        try {
            InstanceInfo infoFromRegistry = null;
            CurrentRequestVersion.set(Version.V2);
            switch (action) {
                case Cancel:
                    node.cancel(appName, id);
                    break;
                case Heartbeat:
                    InstanceStatus overriddenStatus = overriddenInstanceStatusMap.get(id);
                    infoFromRegistry = getInstanceByAppAndId(appName, id, false);
                    node.heartbeat(appName, id, infoFromRegistry, overriddenStatus, false);
                    break;
                case Register:
                    node.register(info);
                    break;
                case StatusUpdate:
                    infoFromRegistry = getInstanceByAppAndId(appName, id, false);
                    node.statusUpdate(appName, id, newStatus, infoFromRegistry);
                    break;
                case DeleteStatusOverride:
                    infoFromRegistry = getInstanceByAppAndId(appName, id, false);
                    node.deleteStatusOverride(appName, id, infoFromRegistry);
                    break;
            }
        } catch (Throwable t) {
            logger.error("Cannot replicate information to {} for action {}", node.getServiceUrl(), action.name(), t);
        }
    }

}
```

##启动时从同等节点获取注册信息
```java
public class PeerAwareInstanceRegistryImpl extends AbstractInstanceRegistry implements PeerAwareInstanceRegistry {
    /**
     * Populates the registry information from a peer eureka node. This
     * operation fails over to other nodes until the list is exhausted if the
     * communication fails.
     * 从一个对等的eureka节点获取注册表信息，并填充到本节点。
     * 如果获取失败，根据配置的重试次数和重试间隔时间多次获取。
     */
    @Override
    public int syncUp() {
        // Copy entire entry from neighboring DS node
        int count = 0;

        for (int i = 0; ((i < serverConfig.getRegistrySyncRetries()) && (count == 0)); i++) {
            if (i > 0) {
                try {
                    Thread.sleep(serverConfig.getRegistrySyncRetryWaitMs());
                } catch (InterruptedException e) {
                    logger.warn("Interrupted during registry transfer..");
                    break;
                }
            }
            Applications apps = eurekaClient.getApplications();
            for (Application app : apps.getRegisteredApplications()) {
                for (InstanceInfo instance : app.getInstances()) {
                    try {
                        if (isRegisterable(instance)) {
                            register(instance, instance.getLeaseInfo().getDurationInSecs(), true);
                            count++;
                        }
                    } catch (Throwable t) {
                        logger.error("During DS init copy", t);
                    }
                }
            }
        }
        return count;
    }


    //syncUp从其它节点获取注册表信息成功后，会调用该方法进行一些列的初始化。
    // count参数为syncUp获取到的实例个数
    @Override
    public void openForTraffic(ApplicationInfoManager applicationInfoManager, int count) {
        //实例个数就等于期待发送续约请求客户端的个数
        this.expectedNumberOfClientsSendingRenews = count;
        //更新续约请求每分钟个数的阀值
        updateRenewsPerMinThreshold();
        logger.info("Got {} instances from neighboring DS node", count);
        logger.info("Renew threshold is: {}", numberOfRenewsPerMinThreshold);
        this.startupTime = System.currentTimeMillis();
        if (count > 0) {
            this.peerInstancesTransferEmptyOnStartup = false;
        }

        //AWS相关的逻辑
        DataCenterInfo.Name selfName = applicationInfoManager.getInfo().getDataCenterInfo().getName();
        boolean isAws = Name.Amazon == selfName;
        if (isAws && serverConfig.shouldPrimeAwsReplicaConnections()) {
            logger.info("Priming AWS connections for all replicas..");
            primeAwsReplicas(applicationInfoManager);
        }
        logger.info("Changing status to UP");
        applicationInfoManager.setInstanceStatus(InstanceStatus.UP);
        //调用父类postInit方法，启动evict定时任务
        super.postInit();
    }
}
```

##总结：
1：PeerAwareInstanceRegistryImpl为AbstractInstanceRegistry的子类，所以具有LeaseManager接口的功能，自身特性就是需要向其他同等的eureka节点复制信息。
2：启动时需要从其它对等（peer node）节点获取注册表信息，如果失败，根据配置间隔一定时间多次进行获取。
3：具有自我保护模式的一些操作（具体的在自我保护模式那一篇详细看吧）