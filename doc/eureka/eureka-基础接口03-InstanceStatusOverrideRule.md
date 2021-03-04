##1: InstanceStatusOverrideRule接口描述：  
这个接口主要是用在AbstractInstanceRegistry中，用于获取实例的状态信息。  
后面通过分析具体的规则实现来理解吧。  
```java
/**
 * A single rule that if matched it returns an instance status.
 * The idea is to use an ordered list of such rules and pick the first result that matches.
 * 一个规则，如果匹配到了就返回一个实例的状态。
 * 使用一个有序的规则列表，使用第一个匹配到的规则结果。
 * 
 * It is designed to be used by
 * {@link AbstractInstanceRegistry#getOverriddenInstanceStatus(InstanceInfo, Lease, boolean)}
 * 这个设计主要用于AbstractInstanceRegistry。getOverriddenInstanceStatus(InstanceInfo, Lease, boolean)方法
 *
 */
public interface InstanceStatusOverrideRule {

    /**
     * Match this rule.
     * 通过入参来匹配该条规则。
     * 
     * @param instanceInfo The instance info whose status we care about.
     * @param existingLease Does the instance have an existing lease already? If so let's consider that.
     * @param isReplication When overriding consider if we are under a replication mode from other servers.
     * @return A result with whether we matched and what we propose the status to be overriden to.
     */
    StatusOverrideResult apply(final InstanceInfo instanceInfo,
                               final Lease<InstanceInfo> existingLease,
                               boolean isReplication);

}
```

###1.1: 实现类AlwaysMatchInstanceStatusRule
```java
/**
 * This rule matches always and returns the current status of the instance.
 * 该规则总是被匹配到，返回实例当前的状态，就是InstanceInfo对象自身保存的status字段。
 */
public class AlwaysMatchInstanceStatusRule implements InstanceStatusOverrideRule {
    private static final Logger logger = LoggerFactory.getLogger(AlwaysMatchInstanceStatusRule.class);

    @Override
    public StatusOverrideResult apply(InstanceInfo instanceInfo,
                                      Lease<InstanceInfo> existingLease,
                                      boolean isReplication) {
        logger.debug("Returning the default instance status {} for instance {}", instanceInfo.getStatus(),
                instanceInfo.getId());
        return StatusOverrideResult.matchingStatus(instanceInfo.getStatus());
    }
}
```

###1.2: 实现类FirstMatchWinsCompositeRule
```java
/**
 * This rule takes an ordered list of rules and returns the result of the first match or the
 * result of the {@link AlwaysMatchInstanceStatusRule}.
 *
 * 这个规则需要一个有序的规则列表，返回第一个匹配到的规则结果或者AlwaysMatchInstanceStatusRule的结果。
 * 可以看出这个规则是一个复核规则类，它内部就存在了一个有序的规则集合，然后一个一个匹配，匹配不到用默认的AlwaysMatchInstanceStatusRule规则来获取。
 */
public class FirstMatchWinsCompositeRule implements InstanceStatusOverrideRule {

    private final InstanceStatusOverrideRule[] rules;
    private final InstanceStatusOverrideRule defaultRule;
    private final String compositeRuleName;

    public FirstMatchWinsCompositeRule(InstanceStatusOverrideRule... rules) {
        this.rules = rules;
        this.defaultRule = new AlwaysMatchInstanceStatusRule();
        // Let's build up and "cache" the rule name to be used by toString();
        List<String> ruleNames = new ArrayList<>(rules.length+1);
        for (int i = 0; i < rules.length; ++i) {
            ruleNames.add(rules[i].toString());
        }
        ruleNames.add(defaultRule.toString());
        compositeRuleName = ruleNames.toString();
    }

    @Override
    public StatusOverrideResult apply(InstanceInfo instanceInfo,
                                      Lease<InstanceInfo> existingLease,
                                      boolean isReplication) {
        for (int i = 0; i < this.rules.length; ++i) {
            StatusOverrideResult result = this.rules[i].apply(instanceInfo, existingLease, isReplication);
            if (result.matches()) {
                return result;
            }
        }
        return defaultRule.apply(instanceInfo, existingLease, isReplication);
    }

}
```

###1.3: 实现类DownOrStartingRule
```java
/**
 * This rule matches if the instance is DOWN or STARTING.
 *
 * 如果实例的状态未DOWN或者STARTING，则该规则被匹配到。
 */
public class DownOrStartingRule implements InstanceStatusOverrideRule {
    private static final Logger logger = LoggerFactory.getLogger(DownOrStartingRule.class);

    @Override
    public StatusOverrideResult apply(InstanceInfo instanceInfo,
                                      Lease<InstanceInfo> existingLease,
                                      boolean isReplication) {
        // ReplicationInstance is DOWN or STARTING - believe that, but when the instance says UP, question that
        // The client instance sends STARTING or DOWN (because of heartbeat failures), then we accept what
        // the client says. The same is the case with replica as well.
        // The OUT_OF_SERVICE from the client or replica needs to be confirmed as well since the service may be
        // currently in SERVICE
        //InstanceStatus总共有五个状态：UP、DOWN、STARTING、OUT_OF_SERVICE、UNKNOWN
        //如果实例的状态不是UP也不是OUT_OF_SERVICE就直接使用实例对象自身的状态。
        //也就是说如果实例状态是DOWN、STARTING、UNKNOWN就直接使用实例自身的状态。
        //也就是说如果是UP或者OUT_OF_SERVICE就再用其它的规则匹配。
        if ((!InstanceInfo.InstanceStatus.UP.equals(instanceInfo.getStatus()))
                && (!InstanceInfo.InstanceStatus.OUT_OF_SERVICE.equals(instanceInfo.getStatus()))) {
            logger.debug("Trusting the instance status {} from replica or instance for instance {}",
                    instanceInfo.getStatus(), instanceInfo.getId());
            return StatusOverrideResult.matchingStatus(instanceInfo.getStatus());
        }
        return StatusOverrideResult.NO_MATCH;
    }

}
```

###1.4: 实现类OverrideExistsRule
```java
/**
 * This rule checks to see if we have overrides for an instance and if we do then we return those.
 *
 * 这个规则检查是否对于一个实例对象设置了覆盖状态（overriddenStatus），如果设置了，就用覆盖状态。
 * overriddenStatus其它篇章里有介绍具体的用法，简单来说就是当前实例处于UP状态正在接收流量，
 * 可以直接添加覆盖状态OUT_OF_SERVICE来使该实例不再接收流量。
 */
public class OverrideExistsRule implements InstanceStatusOverrideRule {

    private static final Logger logger = LoggerFactory.getLogger(OverrideExistsRule.class);

    private Map<String, InstanceInfo.InstanceStatus> statusOverrides;

    //初始化时通过构造函数将overriddenStatus的map传入进来以供使用
    public OverrideExistsRule(Map<String, InstanceInfo.InstanceStatus> statusOverrides) {
        this.statusOverrides = statusOverrides;
    }

    @Override
    public StatusOverrideResult apply(InstanceInfo instanceInfo, Lease<InstanceInfo> existingLease, boolean isReplication) {
        InstanceInfo.InstanceStatus overridden = statusOverrides.get(instanceInfo.getId());
        // If there are instance specific overrides, then they win - otherwise the ASG status
        if (overridden != null) {
            logger.debug("The instance specific override for instance {} and the value is {}",
                    instanceInfo.getId(), overridden.name());
            return StatusOverrideResult.matchingStatus(overridden);
        }
        return StatusOverrideResult.NO_MATCH;
    }

}

```

###1.5: 实现类LeaseExistsRule
```java
/**
 * This rule matches if we have an existing lease for the instance that is UP or OUT_OF_SERVICE.
 * 如果该实例已经存在一个租约，并且实例的状态为UP或者OUT_OF_SERVICE，则匹配上该规则。
 */
public class LeaseExistsRule implements InstanceStatusOverrideRule {

    private static final Logger logger = LoggerFactory.getLogger(LeaseExistsRule.class);

    @Override
    public StatusOverrideResult apply(InstanceInfo instanceInfo,
                                      Lease<InstanceInfo> existingLease,
                                      boolean isReplication) {
        // This is for backward compatibility until all applications have ASG
        // names, otherwise while starting up
        // the client status may override status replicated from other servers
        if (!isReplication) {
            InstanceInfo.InstanceStatus existingStatus = null;
            if (existingLease != null) {
                existingStatus = existingLease.getHolder().getStatus();
            }
            // Allow server to have its way when the status is UP or OUT_OF_SERVICE
            if ((existingStatus != null)
                    && (InstanceInfo.InstanceStatus.OUT_OF_SERVICE.equals(existingStatus)
                    || InstanceInfo.InstanceStatus.UP.equals(existingStatus))) {
                logger.debug("There is already an existing lease with status {}  for instance {}",
                        existingLease.getHolder().getStatus().name(),
                        existingLease.getHolder().getId());
                return StatusOverrideResult.matchingStatus(existingLease.getHolder().getStatus());
            }
        }
        return StatusOverrideResult.NO_MATCH;
    }

}
```

##2: InstanceStatusOverrideRule的使用：

###2.1: 初始化
```java
public class PeerAwareInstanceRegistryImpl extends AbstractInstanceRegistry implements PeerAwareInstanceRegistry {
    @Inject
    public PeerAwareInstanceRegistryImpl(
            EurekaServerConfig serverConfig,
            EurekaClientConfig clientConfig,
            ServerCodecs serverCodecs,
            EurekaClient eurekaClient
    ) {
        super(serverConfig, clientConfig, serverCodecs);
        this.eurekaClient = eurekaClient;
        this.numberOfReplicationsLastMin = new MeasuredRate(1000 * 60 * 1);
        // We first check if the instance is STARTING or DOWN, then we check explicit overrides,
        // then we check the status of a potentially existing lease.
        // 构造函数中进行初始化，使用FirstMatchWinsCompositeRule封装了一组规则来使用
        // 先检查实例是否是STARTING或者DOWN
        // 再检查显示的覆盖状态
        // 再检查已经存在的租约状态
        // 最后如果还未匹配到，再使用FirstMatchWinsCompositeRule获取实例对象自身的状态
        this.instanceStatusOverrideRule = new FirstMatchWinsCompositeRule(new DownOrStartingRule(),
                new OverrideExistsRule(overriddenInstanceStatusMap), new LeaseExistsRule());
    }

    //实现父类AbstractInstanceRegistry定义的抽象方法，返回使用的规则
    @Override
    protected InstanceStatusOverrideRule getInstanceInfoOverrideRule() {
        return this.instanceStatusOverrideRule;
    }
}
```

###2.2: 使用情况
使用规则来获取状态的方法在register和renew中都有使用到。  
```java
public abstract class AbstractInstanceRegistry implements InstanceRegistry {
    //抽象方法，子类PeerAwareInstanceRegistryImpl中实现了该方法，返回了FirstMatchWinsCompositeRule
    protected abstract InstanceStatusOverrideRule getInstanceInfoOverrideRule();

    protected InstanceInfo.InstanceStatus getOverriddenInstanceStatus(InstanceInfo r,
                                                                      Lease<InstanceInfo> existingLease,
                                                                      boolean isReplication) {
        InstanceStatusOverrideRule rule = getInstanceInfoOverrideRule();
        logger.debug("Processing override status using rule: {}", rule);
        return rule.apply(r, existingLease, isReplication).status();
    }

    public void register(InstanceInfo registrant, int leaseDuration, boolean isReplication) {

        //省略了部分代码。。。
        
        //此处会调用该方法来获取实例状。
        //服务第一次上线时，会通过AlwaysMatchInstanceStatusRule获取到UP状态。
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

    public boolean renew(String appName, String id, boolean isReplication) {
        RENEW.increment(isReplication);
        Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
        Lease<InstanceInfo> leaseToRenew = null;
        if (gMap != null) {
            leaseToRenew = gMap.get(id);
        }
        if (leaseToRenew == null) {
            RENEW_NOT_FOUND.increment(isReplication);
            logger.warn("DS: Registry: lease doesn't exist, registering resource: {} - {}", appName, id);
            return false;
        } else {
            InstanceInfo instanceInfo = leaseToRenew.getHolder();
            if (instanceInfo != null) {
                //此处也会调用获取实例的状态，
                //正常情况下续约会通过LeaseExistsRule获取到UP状态
                InstanceStatus overriddenInstanceStatus = this.getOverriddenInstanceStatus(
                        instanceInfo, leaseToRenew, isReplication);
                if (overriddenInstanceStatus == InstanceStatus.UNKNOWN) {
                    logger.info("Instance status UNKNOWN possibly due to deleted override for instance {}"
                            + "; re-register required", instanceInfo.getId());
                    RENEW_NOT_FOUND.increment(isReplication);
                    return false;
                }
                if (!instanceInfo.getStatus().equals(overriddenInstanceStatus)) {
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
    }
}
```

##总结：
1. InstanceStatusOverrideRule是一个用来匹配实例状态的规则接口，通常使用一组规则来匹配，第一个匹配到的就使用，具体的规则有一下几个：
   - DownOrStartingRule，如果实例的状态为DOWN 或者 STARTING，则该规则被匹配到，返回实例的状态。
   - OverrideExistsRule，如果实例存在overriddenStatus（覆盖状态），则该规则被匹配到，返回overriddenStatus。
   - LeaseExistsRule，如果实例有对应的租约存在，并且实例的状态为UP或者OUT_OF_SERVICE，则改规则被匹配到，返回实例的状态。
   - AlwaysMatchInstanceStatusRule，直接返回实例对象自身的状态。
2. FirstMatchWinsCompositeRule是一个规则组合类的规则，组合的规则及使用顺序为：DownOrStartingRule>OverrideExistsRule>LeaseExistsRule>AlwaysMatchInstanceStatusRule
3. 通过规则的顺序我们可以推测出不通状态是通过那个规则来获取到的。
   - DOWN、STARTING、UNKNOWN 通过DownOrStartingRule获取到
   - OUT_OF_SERVICE 通过OverrideExistsRule或者LeaseExistsRule或者AlwaysMatchInstanceStatusRule获取到
   - UP 通过LeaseExistsRule或者AlwaysMatchInstanceStatusRule获取到
