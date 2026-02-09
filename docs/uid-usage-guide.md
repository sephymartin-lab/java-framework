# UID 使用指南

## 概述

`uid`（逻辑主键）是实体的唯一标识符，用于业务层的外键关联。与数据库自增主键 `id` 不同，`uid` 具有以下优势：

1. **安全性**：避免暴露自增 ID 的业务信息（如用户数量、订单数量）
2. **分布式友好**：不依赖数据库自增，适合分布式环境
3. **迁移友好**：数据迁移时 ID 可能变化，但 uid 保持不变
4. **业务隔离**：更好的业务层隔离性

## 字段说明

### uid 字段特性

- **类型**：`String`
- **数据库字段**：`VARCHAR(64)`（建议长度 32-64）
- **唯一索引**：`uk_uid`
- **非空约束**：`NOT NULL`
- **自动生成**：在 INSERT 操作时通过 `@Uid` 注解自动生成
- **生成策略**：默认使用 UUID v4（去除横线），可通过 `UidGenerator` 自定义

### id vs uid

| 特性 | id（自增主键） | uid（逻辑主键） |
|------|---------------|----------------|
| 类型 | `Long` | `String` |
| 生成方式 | 数据库自增 | 应用层生成（UUID） |
| 用途 | 数据库内部关联 | 业务层外键关联 |
| 暴露风险 | 高（暴露业务信息） | 低（无业务含义） |
| 迁移友好 | 低（可能变化） | 高（保持不变） |

## 基类支持

所有继承 `AbstractBaseEntity` 的实体类都自动包含 `uid` 字段：

```java
@Data
public abstract class AbstractBaseEntity implements Serializable, Identifiable<Long>, UidIdentifiable, TimeAuditable {
    
    @TableId(type = IdType.AUTO)
    protected Long id;
    
    @Uid
    @TableField("uid")
    protected String uid;
    
    // ... 其他字段
}
```

## 外键关联规范

### 命名规范

**新规范：外键字段统一使用 `*Uid` 后缀**

```java
// ❌ 旧方式：使用 id 作为外键
private Long userId;
private Long roleId;
private Long deptId;
private Long menuId;

// ✅ 新方式：使用 uid 作为外键
private String userUid;
private String roleUid;
private String deptUid;
private String menuUid;
```

### 数据库字段

外键字段类型统一为 `VARCHAR(64)`：

```sql
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(64) NOT NULL,
    user_uid VARCHAR(64) NOT NULL COMMENT '用户UID',
    role_uid VARCHAR(64) NOT NULL COMMENT '角色UID',
    -- ...
    PRIMARY KEY (id),
    UNIQUE KEY uk_uid (uid),
    INDEX idx_user_uid (user_uid),
    INDEX idx_role_uid (role_uid)
);
```

## 使用示例

### 1. 实体类定义

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRoleDO extends AbstractAuditableEntity {
    
    /**
     * 用户UID
     */
    private String userUid;
    
    /**
     * 角色UID
     */
    private String roleUid;
    
    // ... 其他字段
}
```

### 2. 查询单个实体

```java
// 通过 uid 查询
public SysUserDO getUserByUid(String userUid) {
    return sysUserDao.selectOne(
        Wrappers.lambdaQuery(SysUserDO.class)
            .eq(SysUserDO::getUid, userUid)
    );
}

// 通过 uid 加载（不存在时抛异常）
public SysUserDO loadUserByUid(String userUid) {
    SysUserDO user = getUserByUid(userUid);
    if (user == null) {
        throw new ServiceException("用户不存在：" + userUid);
    }
    return user;
}
```

### 3. 关联查询

```java
// 查询用户的所有角色
public List<SysRoleDO> getUserRoles(String userUid) {
    // 1. 查询用户-角色关联
    List<SysUserRoleDO> userRoles = sysUserRoleDao.selectList(
        Wrappers.lambdaQuery(SysUserRoleDO.class)
            .eq(SysUserRoleDO::getUserUid, userUid)
    );
    
    // 2. 提取角色 UID 列表
    List<String> roleUids = userRoles.stream()
        .map(SysUserRoleDO::getRoleUid)
        .toList();
    
    // 3. 查询角色详情
    if (roleUids.isEmpty()) {
        return Collections.emptyList();
    }
    
    return sysRoleDao.selectList(
        Wrappers.lambdaQuery(SysRoleDO.class)
            .in(SysRoleDO::getUid, roleUids)
    );
}
```

### 4. 条件查询

```java
// 使用 LambdaQueryWrapperX 进行条件查询
public PagingResult<SysUserRoleDTO> queryUserRoles(UserRoleQueryDTO queryDTO) {
    LambdaQueryWrapperX<SysUserRoleDO> wrapperX = new LambdaQueryWrapperX<>();
    wrapperX.eqIfPresent(SysUserRoleDO::getUserUid, queryDTO.getUserUid())
        .eqIfPresent(SysUserRoleDO::getRoleUid, queryDTO.getRoleUid())
        .orderByDesc(SysUserRoleDO::getId);
    
    Page<SysUserRoleDO> mpPage = queryDTO.toMpPage();
    Page<SysUserRoleDO> result = sysUserRoleDao.selectPage(mpPage, wrapperX);
    return PagingResult.from(result);
}
```

### 5. 批量操作

```java
// 批量插入用户角色关联
public void batchAssignRoles(String userUid, List<String> roleUids) {
    List<SysUserRoleDO> userRoles = roleUids.stream()
        .map(roleUid -> {
            SysUserRoleDO userRole = new SysUserRoleDO();
            userRole.setUserUid(userUid);
            userRole.setRoleUid(roleUid);
            return userRole;
        })
        .toList();
    
    sysUserRoleDao.insertBatchSomeColumn(userRoles);
}
```

### 6. 外键关联查询（JOIN）

```java
// 使用 MyBatis XML 进行 JOIN 查询
// UserRoleMapper.xml
<select id="selectUserRolesWithDetails" resultType="UserRoleDetailDTO">
    SELECT 
        ur.id,
        ur.uid,
        ur.user_uid,
        ur.role_uid,
        u.username,
        u.nickname,
        r.role_name,
        r.role_code
    FROM sys_user_role ur
    LEFT JOIN sys_user u ON ur.user_uid = u.uid
    LEFT JOIN sys_role r ON ur.role_uid = r.uid
    WHERE ur.user_uid = #{userUid}
</select>
```

## UidGenerator 自定义

### 默认实现

默认使用 `DefaultUidGenerator`，基于 UUID v4：

```java
public class DefaultUidGenerator implements UidGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### 自定义实现

如果需要自定义生成策略（如 NanoID、雪花算法等），可以实现 `UidGenerator` 接口：

```java
@Component
public class NanoIdUidGenerator implements UidGenerator {
    
    @Override
    public String generate() {
        // 使用 NanoID 生成短唯一ID
        return NanoIdUtils.randomNanoId();
    }
}
```

然后在配置中注入自定义的 `UidGenerator`：

```java
@Configuration
public class MyBatisConfig {
    
    @Bean
    public AutoFillInterceptor autoFillInterceptor(
        ConversionService conversionService,
        CurrentUserExtractor<Object> currentUserExtractor,
        UidGenerator uidGenerator) {
        return new AutoFillInterceptor(conversionService, currentUserExtractor, uidGenerator);
    }
}
```

## 数据库迁移

### 添加 uid 字段

参考迁移脚本：`docs/sql/migration-add-uid-field.sql`

```sql
-- 1. 添加字段（允许为空）
ALTER TABLE your_table 
ADD COLUMN uid VARCHAR(64) NULL COMMENT '逻辑主键';

-- 2. 回填数据（参考 backfill-uid-data.sql）
UPDATE your_table SET uid = REPLACE(UUID(), '-', '') WHERE uid IS NULL;

-- 3. 设置非空
ALTER TABLE your_table 
MODIFY COLUMN uid VARCHAR(64) NOT NULL COMMENT '逻辑主键';

-- 4. 添加唯一索引
ALTER TABLE your_table 
ADD UNIQUE INDEX uk_uid (uid);
```

### 外键字段迁移

```sql
-- 1. 添加新的 uid 外键字段
ALTER TABLE sys_user_role 
ADD COLUMN user_uid VARCHAR(64) NULL COMMENT '用户UID',
ADD COLUMN role_uid VARCHAR(64) NULL COMMENT '角色UID';

-- 2. 回填数据（通过 JOIN 查询关联）
UPDATE sys_user_role ur
INNER JOIN sys_user u ON ur.user_id = u.id
SET ur.user_uid = u.uid;

UPDATE sys_user_role ur
INNER JOIN sys_role r ON ur.role_id = r.id
SET ur.role_uid = r.uid;

-- 3. 设置非空
ALTER TABLE sys_user_role 
MODIFY COLUMN user_uid VARCHAR(64) NOT NULL COMMENT '用户UID',
MODIFY COLUMN role_uid VARCHAR(64) NOT NULL COMMENT '角色UID';

-- 4. 添加索引
ALTER TABLE sys_user_role 
ADD INDEX idx_user_uid (user_uid),
ADD INDEX idx_role_uid (role_uid);

-- 5. （可选）删除旧的外键字段
-- ALTER TABLE sys_user_role DROP COLUMN user_id;
-- ALTER TABLE sys_user_role DROP COLUMN role_id;
```

## 常见问题

### Q: uid 和 id 有什么区别？

A: 
- `id`：数据库自增主键，用于数据库内部关联，类型为 `Long`
- `uid`：逻辑主键，用于业务层外键关联，类型为 `String`，不暴露业务信息

### Q: 什么时候使用 id，什么时候使用 uid？

A:
- **使用 id**：数据库内部关联、性能敏感的内部查询
- **使用 uid**：业务层外键关联、API 接口参数、跨系统数据交换

### Q: uid 生成策略可以自定义吗？

A: 可以，实现 `UidGenerator` 接口并注入到 `AutoFillInterceptor` 即可。

### Q: 现有数据如何迁移？

A: 参考 `docs/sql/migration-add-uid-field.sql` 和 `docs/sql/backfill-uid-data.sql` 脚本。

### Q: 外键字段必须使用 `*Uid` 命名吗？

A: 是的，这是新规范的要求，统一使用 `*Uid` 后缀，类型为 `String`。

### Q: uid 字段的性能如何？

A: 
- `uid` 字段已建立唯一索引，查询性能良好
- 字符串类型外键相比 Long 类型，索引占用空间稍大，但影响可忽略
- 建议在外键字段上也建立索引以优化 JOIN 查询

### Q: 如何确保 uid 的唯一性？

A:
1. 数据库层面：通过 `UNIQUE INDEX uk_uid` 确保唯一性
2. 应用层面：`@Uid` 注解在 INSERT 时自动生成，避免重复
3. 如果手动设置 uid，需要确保唯一性

## 最佳实践

1. **统一使用 uid 进行外键关联**：避免混用 id 和 uid
2. **外键字段建立索引**：优化关联查询性能
3. **API 接口使用 uid**：避免暴露自增 ID
4. **数据迁移时保留 id**：过渡期可以同时保留 id 和 uid
5. **批量操作时注意性能**：大数据量时考虑分批处理

## 相关文档

- [实体类基类迁移指南](./entity-migration-guide.md)
- [数据库迁移脚本](./sql/migration-add-uid-field.sql)
- [数据回填脚本](./sql/backfill-uid-data.sql)
