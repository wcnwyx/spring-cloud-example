AbstractInstanceRegistry进行租约管理的多个地方都用到了overriddenStatus这个字段信息，  
##先看一下InstanceInfo关于该字段的注释：  
```java
public class InstanceInfo {
    /**
     * Sets the overridden status for this instance.Normally set by an external
     * process to disable instance from taking traffic.
     * 设置该实例的overriddenStatus。通常是被外部的进程设置用来禁止实例获取流量。
     */
    public synchronized void setOverriddenStatus(InstanceStatus status) {
        if (this.overriddenStatus != status) {
            this.overriddenStatus = status;
        }
    }
}
```

##如何设置该字段：
```java
public abstract class AbstractInstanceRegistry implements InstanceRegistry {
    //Map保存overriddenStatus
    protected final ConcurrentMap<String, InstanceStatus> overriddenInstanceStatusMap = CacheBuilder
            .newBuilder().initialCapacity(500)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .<String, InstanceStatus>build().asMap();
    
    /**
     * Updates the status of an instance. Normally happens to put an instance
     * between {@link InstanceStatus#OUT_OF_SERVICE} and
     * {@link InstanceStatus#UP} to put the instance in and out of traffic.
     * 更新一个实例的状态，通常发生在OUT_OF_SERVICE和UP这两个状态的转换，用于设置实例接收和不接受流量。
     */
    @Override
    public boolean statusUpdate(String appName, String id,
                                InstanceStatus newStatus, String lastDirtyTimestamp,
                                boolean isReplication) {
        try {
            read.lock();
            STATUS_UPDATE.increment(isReplication);
            Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
            Lease<InstanceInfo> lease = null;
            if (gMap != null) {
                lease = gMap.get(id);
            }
            if (lease == null) {
                return false;
            } else {
                lease.renew();
                InstanceInfo info = lease.getHolder();
                // Lease is always created with its instance info object.
                // This log statement is provided as a safeguard, in case this invariant is violated.
                if (info == null) {
                    logger.error("Found Lease without a holder for instance id {}", id);
                }
                if ((info != null) && !(info.getStatus().equals(newStatus))) {
                    //如果设置的是UP状态，租约进行服务上线操作（记录serviceUpTimestamp时间）
                    if (InstanceStatus.UP.equals(newStatus)) {
                        lease.serviceUp();
                    }
                    //map中保存状态
                    overriddenInstanceStatusMap.put(id, newStatus);
                    //设置实例对象的overriddenStatus字段
                    info.setOverriddenStatus(newStatus);
                    long replicaDirtyTimestamp = 0;
                    //设置实例对象的status字段信息，不更新lastDirtyTimestamp信息
                    info.setStatusWithoutDirty(newStatus);
                    if (lastDirtyTimestamp != null) {
                        replicaDirtyTimestamp = Long.valueOf(lastDirtyTimestamp);
                    }
                    // If the replication's dirty timestamp is more than the existing one, just update
                    // it to the replica's.
                    if (replicaDirtyTimestamp > info.getLastDirtyTimestamp()) {
                        info.setLastDirtyTimestamp(replicaDirtyTimestamp);
                    }
                    info.setActionType(ActionType.MODIFIED);
                    recentlyChangedQueue.add(new RecentlyChangedItem(lease));
                    info.setLastUpdatedTimestamp();
                    //将缓存失效
                    invalidateCache(appName, info.getVIPAddress(), info.getSecureVipAddress());
                }
                return true;
            }
        } finally {
            read.unlock();
        }
    }

    /**
     * Removes status override for a give instance.
     * 移除一个实例的 overriddenStatus状态，和statusUpdate方法正好是一对反操作。
     */
    @Override
    public boolean deleteStatusOverride(String appName, String id,
                                        InstanceStatus newStatus,
                                        String lastDirtyTimestamp,
                                        boolean isReplication) {
        try {
            read.lock();
            STATUS_OVERRIDE_DELETE.increment(isReplication);
            Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
            Lease<InstanceInfo> lease = null;
            if (gMap != null) {
                lease = gMap.get(id);
            }
            if (lease == null) {
                return false;
            } else {
                lease.renew();
                InstanceInfo info = lease.getHolder();

                // Lease is always created with its instance info object.
                // This log statement is provided as a safeguard, in case this invariant is violated.
                if (info == null) {
                    logger.error("Found Lease without a holder for instance id {}", id);
                }

                InstanceStatus currentOverride = overriddenInstanceStatusMap.remove(id);
                if (currentOverride != null && info != null) {
                    //将实例的overriddenStatus状态重新设置为设置为UNKNOWN
                    info.setOverriddenStatus(InstanceStatus.UNKNOWN);
                    //设置实例的status，如果没有明确传入newStatus，该处的newStatus参数为UNKNOWN
                    info.setStatusWithoutDirty(newStatus);
                    long replicaDirtyTimestamp = 0;
                    if (lastDirtyTimestamp != null) {
                        replicaDirtyTimestamp = Long.valueOf(lastDirtyTimestamp);
                    }
                    // If the replication's dirty timestamp is more than the existing one, just update
                    // it to the replica's.
                    if (replicaDirtyTimestamp > info.getLastDirtyTimestamp()) {
                        info.setLastDirtyTimestamp(replicaDirtyTimestamp);
                    }
                    info.setActionType(ActionType.MODIFIED);
                    recentlyChangedQueue.add(new RecentlyChangedItem(lease));
                    info.setLastUpdatedTimestamp();
                    //缓存失效
                    invalidateCache(appName, info.getVIPAddress(), info.getSecureVipAddress());
                }
                return true;
            }
        } finally {
            read.unlock();
        }
    }
}
```
通过上面statusUpdate、deleteStatusOverride 两个方法可以看出来一个用来设置overriddenStatus一个用来移除overriddenStatus。  
下面看下官方给的这两个rest操作解释：  
[官方定义地址](https://github.com/Netflix/eureka/wiki/Eureka-REST-operations)  

|Operation|HTTP action|Description|
|:----|:----|:----|
|Take instance out of service|PUT /eureka/v2/apps/appID/instanceID/status?value=OUT_OF_SERVICE|HTTP Code: 200 on success; 500 on failure|
|Move instance back into service (remove override)|DELETE /eureka/v2/apps/appID/instanceID/status?value=UP (The value=UP is optional, it is used as a suggestion for the fallback status due to removal of the override)|HTTP Code: 200 on success; 500 on failure|
问题：remove override的时候，value=UP这个参数不是必须的？后面通过代码来看一下为什么

通过rest接口可以来操作这两个方法，可以正常的更新实例的状态：  
1. statusUpdate `curl -X PUT -H 'x-netflix-discovery-replication:false' localhost:7001/eureka/apps/TEST-SERVER/20.0.28.153:9002/status?value=OUT_OF_SERVICE&lastDirtyTimestamp=0`
2. deleteStatusOverride `curl -X DELETE -H 'x-netflix-discovery-replication:false' localhost:7001/eureka/apps/TEST-SERVER/20.0.28.153:9002/status?value=UP&lastDirtyTimestamp=0`

##statusUpdate设置OUT_OF_SERVICE之后有哪些影响
通过statusUpdate方法的梳理，InstanceInfo的status和overriddenStatus都已经变成了OUT_OF_SERVICE了，所以该服务不会再有流量分配。  
那该服务还是在运行中的，renew（续约）、register（注册）或者cancel(取消)操作会有什么影响呢？再看下这两个操作：  
```java
public abstract class AbstractInstanceRegistry implements InstanceRegistry {
    protected final ConcurrentMap<String, InstanceStatus> overriddenInstanceStatusMap = CacheBuilder
            .newBuilder().initialCapacity(500)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .<String, InstanceStatus>build().asMap();
    
    public boolean renew(String appName, String id, boolean isReplication) {
        //省略部分代码。。。
        
        if (instanceInfo != null) {
            //通过statusUpdate设置了OUT_OF_SERVICE了，
            //这时会匹配到OverrideExistsRule规则拿到了状态是OUT_OF_SERVICE
            //InstanceStatusOverrideRule这个规则的说明在其它篇章里单独说了。
            InstanceStatus overriddenInstanceStatus = this.getOverriddenInstanceStatus(
                    instanceInfo, leaseToRenew, isReplication);
            if (overriddenInstanceStatus == InstanceStatus.UNKNOWN) {
                //这个条件什么时候触发呢？什么时候getOverriddenInstanceStatus获取到的状态是UNKNOWN呢？
                //下面的日志打印说的很清楚了，当执行deleteStatusOverride时，如果没有传入新的状态，instanceInfo的status就会被设置为UNKNOWN，
                //那么这时就会通过DownOrStartingRule规则获取到InstanceInfo本身的UNKNOWN状态。
                //然后返回给客户端404出发从新注册请求
                //也解释了为什么在deleteStatusOverride时value=UP不是必填的了。
                logger.info("Instance status UNKNOWN possibly due to deleted override for instance {}"
                        + "; re-register required", instanceInfo.getId());
                RENEW_NOT_FOUND.increment(isReplication);
                return false;
            }
            if (!instanceInfo.getStatus().equals(overriddenInstanceStatus)) {
                //这个条件什么时候触发呢？
                logger.info(
                        "The instance status {} is different from overridden instance status {} for instance {}. "
                                + "Hence setting the status to overridden status", instanceInfo.getStatus().name(),
                        instanceInfo.getOverriddenStatus().name(),
                        instanceInfo.getId());
                instanceInfo.setStatusWithoutDirty(overriddenInstanceStatus);

            }
        }
        renewsLastMin.increment();
        leaseToRenew.renew();
        return true;
    }

    public void register(InstanceInfo registrant, int leaseDuration, boolean isReplication) {

        //省略了部分代码。。。

        //statusUpdate设置OUT_OF_SERVICE之后。
        //情况1：
        //服务正常下线，调用register设置为DOWN状态（匹配到DownOrStartingRule），然后再cancel，
        //然后服务再正常上线调用register，通过AlwaysMatchInstanceStatusRule配到到状态UP。
        //情况2：
        //服务异常下线，未正常cancel掉数据，所以overriddenInstanceStatusMap还保持有该实例的覆盖状态。
        //服务又再次启动来进行注册，虽然客户端发送过来的实例状态为UP，但是会匹配到OverrideExistsRule返回OUT_OF_SERVICE状态，
        //服务启动后还是会保持OUT_OF_SERVICE状态
        InstanceStatus overriddenInstanceStatus = getOverriddenInstanceStatus(registrant, existingLease, isReplication);
        registrant.setStatusWithoutDirty(overriddenInstanceStatus);

        if (InstanceStatus.UP.equals(registrant.getStatus())) {
            lease.serviceUp();
        }
        registrant.setActionType(ActionType.ADDED);
        recentlyChangedQueue.add(new RecentlyChangedItem(lease));
        registrant.setLastUpdatedTimestamp();
        invalidateCache(registrant.getAppName(), registrant.getVIPAddress(), registrant.getSecureVipAddress());
        logger.info("Registered instance {}/{} with status {} (replication={})",
                registrant.getAppName(), registrant.getId(), registrant.getStatus(), isReplication);
    }

    protected boolean internalCancel(String appName, String id, boolean isReplication) {
        try {
            //省略部分代码。。。
            
            //cancel是直接将实例的overriddenStatus从map中移除掉
            InstanceStatus instanceStatus = overriddenInstanceStatusMap.remove(id);
            if (instanceStatus != null) {
                logger.debug("Removed instance id {} from the overridden map which has value {}", id, instanceStatus.name());
            }

            //省略部分代码。。。
            return true;
        } finally {
            read.unlock();
        }
    }
}
```

##问题：
如果说statusUpdate将状态设置为OUT_OF_SERVICE后，是否可以再使用statusUpdate将状态设置为UP呢，而不使用deleteStatusOverride？  
顺着逻辑捋下来也是没问题的，服务续约会通过OverrideExistsRule规则获取到UP状态，正常下线后重新上线也没问题，异常中断下线再次注册上来也可以通过OverrideExistsRule规则获取到UP状态。  