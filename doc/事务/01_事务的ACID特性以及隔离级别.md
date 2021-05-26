#事务的ACID特性以及隔离级别

##一：事务（Transaction）
事务（Transaction）是由一些利对系统中数据进行访问与更新的操作所组成的一个程序执行逻辑单元，
狭义上的事务特指数据库事务。一方面，当多个应用程序并发访问数据库时，事务可以在这些方法之间提供
一个隔离方法，以防止彼此的操作互相干扰。另一方面，事务为数据库操作序列提供了一个从失败中恢复到
正常状态的方法，同时提供了数据库即使在异常状态下仍能保持数据一致性的方法。  

事务具有四大特性：原子性（Atomicity）、一致性（Consistency）、隔离性（Isolation）和
持久性（Durability），简称为事务的ACID特性。

### 1. 原子性
原子性是指事务必须是一个原子的操作序列单元。事务中包含的各项操作在一次执行过程中，只允许出现
**全部成功执行**和**全部不执行**两种状态。任何一项操作失败都将导致整个事务失败，同时其它已经
被执行的操作都将被撤销并回滚，只有所有的操作都成功，整个事务才算是成功完成。

### 2. 一致性
一致性是指事务的执行不能破坏数据库数据的完整性和一致性，一个事务在执行前和执行后，数据库都必须
处于一致性状态。也就是说，事务的执行结果，必须是使数据库从一个一致性状态变到另外一个一致性状态，
因此数据库只包含成功事务提交的结果时，就能说数据库处于一致性状态。而如果数据库系统在运行过程中
发生故障，有些事务尚未完成就被迫中断，这些未完成的事务对数据库所做的修改有一部分已经写入物理数
据库，这时数据库就处于一种不正确的状态，或者说是不一致的状态。

### 3. 隔离性
隔离性是指在并发环境中，并发事务是相互隔离的，一个事务的执行不能被其它事务干扰。也就是说，不同
的事务并发操作相同的数据时，每个事务都有各自完整的数据空间，既一个事务内部的操作及使用的数据对
其它并发事务是隔离的，并发执行的各个事务之间不能互相干扰。SQL规范中定义了4个隔离级别：
- 未授权读取（Read Uncommitted）
- 授权读取（Read Committed）
- 可重复读取（Repeatable Read）
- 串行化（Serializable）

### 4. 持久性
持久性是指一个事务一旦提交，它对数据库中数据的状态变更应该是永久性的。换句话说，一旦某个事务成功
结束，那么它对数据库所做的更新就必须被永久保存下来，即使发生系统崩溃或机器宕机，只要数据库能够重
启，那么一定能够将其恢复到事务成功结束时的状态。

##二： 隔离级别
隔离级别参考下SQL规范文件里的说明，地址：[SQL规范](http://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt)

An SQL-transaction has an isolation level that is READ UNCOMMITTED,
READ COMMITTED, REPEATABLE READ, or SERIALIZABLE. The isolation
level of an SQL-transaction defines the degree to which the opera-
tions on SQL-data or schemas in that SQL-transaction are affected
by the effects of and can affect operations on SQL-data or schemas
in concurrent SQL-transactions. The isolation level of a SQL-
transaction is SERIALIZABLE by default. The level can be explicitly
set by the <set transaction statement>.
```
SQL事务的隔离级别为READ UNCOMMITTED、READ COMMITTED、REPEATABLE READ或SERIALIZABLE。
SQL事务的隔离级别定义了在该SQL事务中对SQL数据或模式的操作受并发SQL事务中对SQL数据或模式的操作
的影响的程度，并且可以影响并发SQL事务中对SQL数据或模式的操作。默认情况下，SQL事务的隔离级别是
SERIALIZABLE。级别可以通过<set transaction statement>显式设置。
```

The isolation level specifies the kind of phenomena that can occur
during the execution of concurrent SQL-transactions. The following
phenomena are possible:  

```隔离级别指定在执行并发SQL事务时可能发生的现象类型。可能出现以下现象：```

1. P1 ("Dirty read"): SQL-transaction T1 modifies a row. SQL-
   transaction T2 then reads that row before T1 performs a COMMIT.
   If T1 then performs a ROLLBACK, T2 will have read a row that was
   never committed and that may thus be considered to have never
   existed.
   ```
   脏读：SQL事务T1修改一行。然后，SQL事务T2在T1执行提交之前读取该行。如果T1
   随后执行回滚，T2将读取从未提交的行，因此可以认为该行从未存在。
   ```

2. P2 ("Non-repeatable read"): SQL-transaction T1 reads a row. SQL-
   transaction T2 then modifies or deletes that row and performs
   a COMMIT. If T1 then attempts to reread the row, it may receive
   the modified value or discover that the row has been deleted.
   ```
   不可重复读取：SQL事务T1读取一行。然后，SQL事务T2修改或删除该行并执行提交。
   如果T1随后尝试重新读取该行，它可能会收到修改后的值或发现该行已被删除。
   ```

3. P3 ("Phantom"): SQL-transaction T1 reads the set of rows N
   that satisfy some <search condition>. SQL-transaction T2 then
   executes SQL-statements that generate one or more rows that
   satisfy the <search condition> used by SQL-transaction T1. If
   SQL-transaction T1 then repeats the initial read with the same
   <search condition>, it obtains a different collection of rows.
   ```
   幻读:SQL事务T1读取满足某些<搜索条件>的N行集合。然后，SQL事务T2执行SQL语句，
   这些SQL语句生成了满足SQL事务T1使用的<搜索条件>的一行或多行。如果SQL事务T1
   再次以相同的<搜索条件>重复初始读取，它将获得不同的行集合。
   ```

The four isolation levels guarantee that each SQL-transaction will
be executed completely or not at all, and that no updates will be
lost. The isolation levels are different with respect to phenomena
P1, P2, and P3. Table 9, "SQL-transaction isolation levels and the
three phenomena" specifies the phenomena that are possible and not
possible for a given isolation level.

```
这四个隔离级别保证每个SQL事务将完全执行，或者根本不会执行，并且不会丢失任何更新。
隔离级别对于现象P1、P2和P3有不同的表现。表9，"SQL-transaction isolation levels and the three phenomena"
指定了给定隔离级别可能出现的、不可能的现象。
```

|隔离等级          |P1（脏读）   |P2（不可重复读）|P3（幻读）   |
|----------------|------------|-------------|------------|
|READ UNCOMMITTED|Possible    |Possible     |Possible    |
|READ COMMITTED  |Not Possible|Possible     |Possible    |
|REPEATABLE READ |Not Possible|Not Possible |Possible    |
|SERIALIZABLE    |Not Possible|Not Possible |Not Possible|

### 1. 隔离级别：读未提交（Read Uncommitted）
从名字来看就是可以读取到未提交的数据，典型的脏读。会发生脏读、不可重复度和幻读。  
T1将数据A从1改为2，但是还未提交，T2读取A，读取到了值为2，随后T1操作回滚了，就导致了T2读取到了T1
未提交的脏数据。

### 2. 隔离级别：读已提交（Read Committed）
只能读取到已提交的数据，那就不可能产生脏读。但是会产生不可重复读和幻读。  
T1将数据A从1改为2，但是还未提交，T2读取A，读取到的结果为1，不会读取到未提交的事务，
所以不会产生脏读，但是T2事务再次做读取A的操作，这时T1已经将事务提交了，A的值为2，
所以T2本次读取的A为2，导致T1发现同一个事务读取两次A的值不一样，产生了不可重复读。

### 3. 隔离级别：可重复读取（Repeatable Read）
通过名字来看，可重复读，那就不会出现不可重复读，一个事务在处理过程中，多次读取同一个数据时，
其值都和事务开始时是一致的。因此此级别禁止了脏读和不可重复读，但是会出现幻读。

### 4. 隔离级别：串行化（Serializable）
最严格的事务级别，所有的事务都串行执行，不能并发执行，所以不会产生脏读、不可重复度和幻读。