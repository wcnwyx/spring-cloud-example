##ResponseCache接口
先看下这个接口有哪些方法。 
```java
public interface ResponseCache {

    //将某个appName的缓存失效掉
    void invalidate(String appName, @Nullable String vipAddress, @Nullable String secureVipAddress);

    //获取增量的数据版本号
    AtomicLong getVersionDelta();
    
    //获取包含其它region的增量数据版本号
    AtomicLong getVersionDeltaWithRegions();

    /**
     * Get the cached information about applications.
     * 根据key获取相关应用的缓存信息。
     * 
     * <p>
     * If the cached information is not available it is generated on the first
     * request. After the first request, the information is then updated
     * periodically by a background thread.
     * </p>
     * 如果缓存信息不可用，将在第一次访问的时候生成。
     * 之后信息由后台线程定期更新。
     *
     */
     String get(Key key);

    /**
     * Get the compressed information about the applications.
     * 根据key获取相关应用的压缩过的缓存信息。
     */
    byte[] getGZIP(Key key);
}
```

##Key
```java
public class Key {
    //key类型支持JSON与XML
    public enum KeyType {
        JSON, XML
    }

    //表示该key再缓存中对应数据的实体类型
    //Application、VIP和SVIP：就是InstanceInfo里的字段，表示虚拟互联网协议地址
    public enum EntityType {
        Application, VIP, SVIP
    }

    //entityName包括ALL_APPS、ALL_APPS_DELTA、其它
    private final String entityName;
    //region数组
    private final String[] regions;
    private final KeyType requestType;
    private final Version requestVersion;
    private final String hashKey;
    private final EntityType entityType;
    private final EurekaAccept eurekaAccept;

    public Key(EntityType entityType, String entityName, KeyType type, Version v, EurekaAccept eurekaAccept, @Nullable String[] regions) {
        this.regions = regions;
        this.entityType = entityType;
        this.entityName = entityName;
        this.requestType = type;
        this.requestVersion = v;
        this.eurekaAccept = eurekaAccept;
        hashKey = this.entityType + this.entityName + (null != this.regions ? Arrays.toString(this.regions) : "")
                + requestType.name() + requestVersion.name() + this.eurekaAccept.name();
    }
}
```

##ResponseCacheImpl
```java
/**
 * The class that is responsible for caching registry information that will be
 * queried by the clients.
 * 负责缓存客户端查询的注册表信息的类。
 *
 * <p>
 * The cache is maintained in compressed and non-compressed form for three
 * categories of requests - all applications, delta changes and for individual
 * applications. The compressed form is probably the most efficient in terms of
 * network traffic especially when querying all applications.
 * 对于以下三种请求缓存保持有压缩和非压缩的数据： 整个Applications、增量的变化、单独的Application
 * 就网络流量而言，压缩形式是很有效的，尤其是查询整个Applications
 *
 * The cache also maintains separate pay load for <em>JSON</em> and <em>XML</em>
 * formats and for multiple versions too.
 * </p>
 * 缓存也支持JSON和XML格式，和多个版本。
 *
 */
public class ResponseCacheImpl implements ResponseCache {
    //字符串来表示Key中的entityName
    public static final String ALL_APPS = "ALL_APPS";
    public static final String ALL_APPS_DELTA = "ALL_APPS_DELTA";

    private final AtomicLong versionDelta = new AtomicLong(0);
    private final AtomicLong versionDeltaWithRegions = new AtomicLong(0);

    /**
     * This map holds mapping of keys without regions to a list of keys with region (provided by clients)
     * Since, during invalidation, triggered by a change in registry for local region, we do not know the regions
     * requested by clients, we use this mapping to get all the keys with regions to be invalidated.
     * If we do not do this, any cached user requests containing region keys will not be invalidated and will stick
     * around till expiry. Github issue: https://github.com/Netflix/eureka/issues/118
     * 这个map映射了一个不包活region信息的key和一组包括region信息的可以（key的其它信息都一样，只是region不一样）。
     * 因为失效缓存的动作（租约管理相关的接口都会在最后将缓存失效），都是本server自己触发的，那本server就不知道客户端请求过哪些region的key，因为这些key是客户端请求时才生成的，
     * 通过这个map将所有因为客户端请求过而生成的带有region的key全部映射保存起来，就可以在失效key的时候将带有region信息的key也失效掉。
     * 如果不这么做的话，同样一个key，带有region信息的数据就会一直存在缓存，不会被更新，只能等自己过期失效。
     */
    private final Multimap<Key, Key> regionSpecificKeys =
            Multimaps.newListMultimap(new ConcurrentHashMap<Key, Collection<Key>>(), new Supplier<List<Key>>() {
                @Override
                public List<Key> get() {
                    return new CopyOnWriteArrayList<Key>();
                }
            });

    //只读缓存
    private final ConcurrentMap<Key, Value> readOnlyCacheMap = new ConcurrentHashMap<Key, Value>();
    //读写缓存
    private final LoadingCache<Key, Value> readWriteCacheMap;
    //表示是否只是用只读缓存
    private final boolean shouldUseReadOnlyResponseCache;
    private final AbstractInstanceRegistry registry;
    private final EurekaServerConfig serverConfig;
    private final ServerCodecs serverCodecs;

    ResponseCacheImpl(EurekaServerConfig serverConfig, ServerCodecs serverCodecs, AbstractInstanceRegistry registry) {
        this.serverConfig = serverConfig;
        this.serverCodecs = serverCodecs;
        this.shouldUseReadOnlyResponseCache = serverConfig.shouldUseReadOnlyResponseCache();
        this.registry = registry;

        long responseCacheUpdateIntervalMs = serverConfig.getResponseCacheUpdateIntervalMs();
        this.readWriteCacheMap =
                CacheBuilder.newBuilder().initialCapacity(serverConfig.getInitialCapacityOfResponseCache())
                        .expireAfterWrite(serverConfig.getResponseCacheAutoExpirationInSeconds(), TimeUnit.SECONDS)
                        .removalListener(new RemovalListener<Key, Value>() {
                            @Override
                            public void onRemoval(RemovalNotification<Key, Value> notification) {
                                Key removedKey = notification.getKey();
                                if (removedKey.hasRegions()) {
                                    //如果key中包含有regions信息，将regionSpecificKeys集合中的数据也移除掉
                                    Key cloneWithNoRegions = removedKey.cloneWithoutRegions();
                                    regionSpecificKeys.remove(cloneWithNoRegions, removedKey);
                                }
                            }
                        })
                        .build(new CacheLoader<Key, Value>() {
                            @Override
                            public Value load(Key key) throws Exception {
                                if (key.hasRegions()) {
                                    //如果key中有regions信息，将没有region的可以和有region的key保存到regionSpecificKeys
                                    Key cloneWithNoRegions = key.cloneWithoutRegions();
                                    regionSpecificKeys.put(cloneWithNoRegions, key);
                                }
                                //生成该key对应的数据
                                Value value = generatePayload(key);
                                return value;
                            }
                        });

        if (shouldUseReadOnlyResponseCache) {
            //如果只允许使用只读缓存的话，要启动一个定时任务定时的更新只读缓存readOnlyCacheMap
            timer.schedule(getCacheUpdateTask(),
                    new Date(((System.currentTimeMillis() / responseCacheUpdateIntervalMs) * responseCacheUpdateIntervalMs)
                            + responseCacheUpdateIntervalMs),
                    responseCacheUpdateIntervalMs);
        }

        try {
            Monitors.registerObject(this);
        } catch (Throwable e) {
            logger.warn("Cannot register the JMX monitor for the InstanceRegistry", e);
        }
    }


    /*
     * 生成该key对应的数据以进行缓存
     */
    private Value generatePayload(Key key) {
        Stopwatch tracer = null;
        try {
            String payload;
            switch (key.getEntityType()) {
                case Application:
                    //判断key中是否包含regions信息
                    boolean isRemoteRegionRequested = key.hasRegions();

                    if (ALL_APPS.equals(key.getName())) {
                        //该key表示整个Applications
                        if (isRemoteRegionRequested) {
                            //包含regions数据
                            tracer = serializeAllAppsWithRemoteRegionTimer.start();
                            //通过registry.getApplicationsFromMultipleRegions(key.getRegions())来获取本地和远端regions的Applications数据
                            //根据Key中的json/xml表示生成最终的数据
                            payload = getPayLoad(key, registry.getApplicationsFromMultipleRegions(key.getRegions()));
                        } else {
                            tracer = serializeAllAppsTimer.start();
                            //不包含regions数据，只获取本服务的Applications信息进行数据生成
                            payload = getPayLoad(key, registry.getApplications());
                        }
                    } else if (ALL_APPS_DELTA.equals(key.getName())) {
                        if (isRemoteRegionRequested) {
                            tracer = serializeDeltaAppsWithRemoteRegionTimer.start();
                            //将版本号加1
                            versionDeltaWithRegions.incrementAndGet();
                            payload = getPayLoad(key,
                                    registry.getApplicationDeltasFromMultipleRegions(key.getRegions()));
                        } else {
                            tracer = serializeDeltaAppsTimer.start();
                            //将版本号加1
                            versionDelta.incrementAndGet();
                            payload = getPayLoad(key, registry.getApplicationDeltas());
                        }
                    } else {
                        tracer = serializeOneApptimer.start();
                        payload = getPayLoad(key, registry.getApplication(key.getName()));
                    }
                    break;
                case VIP:
                case SVIP:
                    tracer = serializeViptimer.start();
                    payload = getPayLoad(key, getApplicationsForVip(key, registry));
                    break;
                default:
                    logger.error("Unidentified entity type: {} found in the cache key.", key.getEntityType());
                    payload = "";
                    break;
            }
            return new Value(payload);
        } finally {
            if (tracer != null) {
                tracer.stop();
            }
        }
    }

    //根据keyType与accept来生成对应的JSON_full、JSON_compact、XML_full、XML_compact编码类型的Applications数据
    private String getPayLoad(Key key, Applications apps) {
        EncoderWrapper encoderWrapper = serverCodecs.getEncoder(key.getType(), key.getEurekaAccept());
        String result;
        try {
            result = encoderWrapper.encode(apps);
        } catch (Exception e) {
            logger.error("Failed to encode the payload for all apps", e);
            return "";
        }
        if(logger.isDebugEnabled()) {
            logger.debug("New application cache entry {} with apps hashcode {}", key.toStringCompact(), apps.getAppsHashCode());
        }
        return result;
    }
}
```