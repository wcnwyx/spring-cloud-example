##InstanceStatusOverrideRule接口描述：  
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

###实现类AlwaysMatchInstanceStatusRule
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

##实现类FirstMatchWinsCompositeRule
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

##实现类DownOrStartingRule
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
        //也就是说如果是UP或者OUT_OF_SERVICE就在用其它的规则匹配。
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

##实现类OverrideExistsRule
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

##实现类LeaseExistsRule
```java
/**
 * This rule matches if we have an existing lease for the instance that is UP or OUT_OF_SERVICE.
 *
 * Created by Nikos Michalakis on 7/13/16.
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