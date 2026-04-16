# 实体类基类迁移指南

本文档用于指导将旧版实体基类迁移到新版基类体系。

## 基类体系概览

### 新版基类继承关系

```
AbstractBaseEntity (id + uid + createdTime + updatedTime)
├── AbstractAuditableEntity (+ createdBy + updatedBy)
│   └── AbstractTenantAuditableEntity (+ tenantId)
└── AbstractTenantBaseEntity (+ tenantId)
```

### 基类对比表

| 基类 | 包含字段 | 适用场景 |
|------|----------|----------|
| `AbstractBaseEntity` | id, uid, createdTime, updatedTime | 日志表、关联表、C端数据、无需操作人审计 |
| `AbstractAuditableEntity` | 上述 + createdBy, updatedBy | 后台管理数据、需要追溯操作人 |
| `AbstractTenantBaseEntity` | AbstractBaseEntity + tenantId | 多租户系统，仅需时间审计 |
| `AbstractTenantAuditableEntity` | AbstractAuditableEntity + tenantId | 多租户系统，完整审计 |

**字段说明：**
- `id`：自增主键（Long 类型），用于数据库内部关联
- `uid`：逻辑主键（String 类型），用于业务层外键关联，避免暴露自增 ID

### 废弃的旧基类

| 旧基类 | 迁移目标 | 说明 |
|--------|----------|------|
| `AbstractEntity` | `AbstractAuditableEntity` | 已标记 @Deprecated |
| `AbstractBaseLogicDeleteEntity` | 见下方逻辑删除说明 | 已废弃 |
| `AbstractBaseLogicDeleteVersionEntity` | 见下方逻辑删除说明 | 已废弃 |
| `AbstractTenantEntity` | `AbstractTenantAuditableEntity` | 已废弃 |

---

## 迁移步骤

### 步骤 1：确定目标基类

根据业务场景选择合适的基类：

**选择 `AbstractBaseEntity` 的场景：**
- 登录日志、操作日志等系统日志表
- 用户-角色、角色-菜单等关联表（如果不需要审计）
- C端用户数据（不需要后台操作人追溯）
- 系统通知等自动生成的数据

**选择 `AbstractAuditableEntity` 的场景：**
- 后台管理的业务数据（用户、角色、菜单、部门等）
- 需要追溯"谁创建"、"谁修改"的数据
- 配置类数据（系统配置、字典等）

**选择多租户基类的场景：**
- 多租户 SaaS 系统中的业务数据
- 需要按租户隔离的数据

### 步骤 2：处理逻辑删除

**新规范：逻辑删除字段需要在实体类中手动添加**

```java
import com.baomidou.mybatisplus.annotation.TableLogic;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleDO extends AbstractAuditableEntity {

    /**
     * 删除标记（0-未删除，非0-已删除）
     */
    @TableLogic(value = "0", delval = "id")
    private Long deleted;
    
    // 其他字段...
}
```

**逻辑删除策略说明：**
- `@TableLogic(value = "0", delval = "id")`：删除时将 deleted 设为记录 id，确保唯一约束不冲突
- `@TableLogic(value = "0", delval = "UNIX_TIMESTAMP()")`：删除时使用时间戳（适用于乐观锁场景）

### 步骤 3：处理外键关联（使用 uid）

**新规范：外键关联统一使用 uid 字段**

所有外键关联字段应使用 `*Uid` 后缀，类型为 `String`：

```java
// ❌ 旧方式：使用 id 作为外键
private Long userId;
private Long roleId;
private Long deptId;

// ✅ 新方式：使用 uid 作为外键
private String userUid;
private String roleUid;
private String deptUid;
```

**查询示例：**

```java
// 通过 uid 查询
SysUserDO user = sysUserDao.selectOne(
    Wrappers.lambdaQuery(SysUserDO.class)
        .eq(SysUserDO::getUid, userUid)
);

// 通过 uid 关联查询
LambdaQueryWrapperX<SysUserRoleDO> wrapperX = new LambdaQueryWrapperX<>();
wrapperX.eqIfPresent(SysUserRoleDO::getUserUid, userUid)
    .eqIfPresent(SysUserRoleDO::getRoleUid, roleUid);
List<SysUserRoleDO> userRoles = sysUserRoleDao.selectList(wrapperX);
```

### 步骤 4：修改实体类

**迁移前：**
```java
import top.sephy.infra.entity.AbstractEntity;
// 或
import top.sephy.infra.entity.AbstractBaseLogicDeleteEntity;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleDO extends AbstractBaseLogicDeleteEntity {
    private static final long serialVersionUID = 1;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String roleCode;
    // ...
}
```

**迁移后：**
```java
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.sephy.infra.entity.AbstractAuditableEntity;

/**
 * 系统角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleDO extends AbstractAuditableEntity {

    /**
     * 删除标记（0-未删除，非0-已删除）
     */
    @TableLogic(value = "0", delval = "id")
    private Long deleted;

    private String roleCode;
    // ...
}
```

**关键变更点：**
1. 修改 import 语句
2. 修改 extends 的基类
3. 删除 `private static final long serialVersionUID = 1;`（基类已提供）
4. 删除 `@TableId` 注解的 id 字段（基类已提供）
5. 删除 `@ToString(callSuper = true)`（lombok.config 已配置）
6. 如需逻辑删除，手动添加 `@TableLogic` 字段
7. 更新类注释

---

## 迁移检查清单

### 迁移前检查

- [ ] 确认当前实体类使用的旧基类
- [ ] 确认是否需要操作人审计（createdBy/updatedBy）
- [ ] 确认是否需要逻辑删除
- [ ] 确认是否是多租户数据

### 迁移操作

- [ ] 修改 import 语句
- [ ] 修改 extends 基类
- [ ] 删除冗余字段（id、serialVersionUID）
- [ ] 删除冗余注解（@ToString）
- [ ] 添加逻辑删除字段（如需要）
- [ ] 更新类注释

### 迁移后验证

- [ ] 无编译错误
- [ ] 无 linter 警告
- [ ] 数据库字段与实体类匹配
- [ ] 业务功能正常

---

## 数据库表结构参考

### AbstractBaseEntity 对应的字段

```sql
`id` bigint NOT NULL AUTO_INCREMENT,
`uid` varchar(64) NOT NULL COMMENT '逻辑主键',
`created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
UNIQUE KEY `uk_uid` (`uid`)
```

**uid 字段说明：**
- 类型：`VARCHAR(64)`（建议长度 32-64）
- 唯一索引：`uk_uid`
- 自动生成：在 INSERT 操作时自动生成（通过 `@Uid` 注解）
- 用途：用于业务层外键关联，避免暴露自增 ID 的业务信息

### AbstractAuditableEntity 对应的字段

```sql
`id` bigint NOT NULL AUTO_INCREMENT,
`created_by` bigint DEFAULT NULL COMMENT '创建人ID',
`created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
`updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
```

### 逻辑删除字段

```sql
`deleted` bigint NOT NULL DEFAULT '0' COMMENT '删除标记（0-未删除，非0-已删除）',
```

### 多租户字段

```sql
`tenant_id` bigint NOT NULL COMMENT '租户ID',
```

---

## 已完成迁移的模块

### infra-system 模块（2026-01-30）

| 实体类 | 表名 | 新基类 | 逻辑删除 |
|--------|------|--------|---------|
| `SysLoginLogDO` | sys_login_log | `AbstractBaseEntity` | 否 |
| `SysLongTextDO` | sys_long_text | `AbstractAuditableEntity` | 否 |
| `SysFileDO` | sys_file | `AbstractAuditableEntity` | 否 |
| `SysConfigDO` | sys_config | `AbstractAuditableEntity` | 否 |
| `NotifyDO` | sys_notify | `AbstractBaseEntity` | 否 |
| `SysUserDO` | sys_user | `AbstractAuditableEntity` | 否 |
| `SysDictDO` | sys_dict | `AbstractAuditableEntity` | 否 |
| `SysUserRoleDO` | sys_user_role | `AbstractAuditableEntity` | 否 |
| `SysRoleDO` | sys_role | `AbstractAuditableEntity` | 是 |
| `SysMenuDO` | sys_menu | `AbstractAuditableEntity` | 是 |
| `SysDeptDO` | sys_dept | `AbstractAuditableEntity` | 是 |
| `SysPositionDO` | sys_position | `AbstractAuditableEntity` | 是 |
| `SysRoleMenuDO` | sys_role_menu | `AbstractAuditableEntity` | 是 |
| `SysUserDeptDO` | sys_user_dept | `AbstractAuditableEntity` | 是 |
| `SysUserPositionDO` | sys_user_position | `AbstractAuditableEntity` | 是 |
| `SmsMessageDO` | sys_sms_message | `AbstractAuditableEntity` | 否 |
| `SmsChannelDO` | sys_sms_channel | `AbstractAuditableEntity` | 是 |
| `SmsTemplateDO` | sys_sms_template | `AbstractAuditableEntity` | 是 |
| `SmsTplChannelDO` | sys_sms_tpl_channel | `AbstractAuditableEntity` | 是 |
| `SmsBlacklistDO` | sys_sms_blacklist | `AbstractAuditableEntity` | 是 |
| `SmsWhitelistDO` | sys_sms_whitelist | `AbstractAuditableEntity` | 是 |

---

## 待迁移模块

在此处记录待迁移的模块和实体类：

### 模块名称（待迁移）

| 实体类 | 表名 | 当前基类 | 目标基类 | 逻辑删除 | 状态 |
|--------|------|----------|----------|---------|------|
| 示例 | example | AbstractEntity | AbstractAuditableEntity | 是 | 待处理 |

---

## 常见问题

### Q: 如何判断是否需要操作人审计？

A: 如果数据需要在后台管理系统中被创建或修改，且需要追溯是"谁"进行了操作，则需要操作人审计，选择 `AbstractAuditableEntity`。

### Q: 关联表（如 sys_user_role）是否需要审计？

A: 视业务需求而定。如果需要记录"谁给用户分配了角色"，则使用 `AbstractAuditableEntity`；如果不需要，使用 `AbstractBaseEntity`。

### Q: 为什么逻辑删除使用 id 作为 delval？

A: 使用 `delval = "id"` 可以确保删除后的记录 deleted 字段值唯一，避免唯一约束冲突。例如：同一个角色代码可以被删除后重新创建。

### Q: 旧数据如何处理？

A: 
1. 如果表已有 `created_by`/`updated_by` 字段，直接迁移即可
2. 如果表没有这些字段，需要先执行 DDL 添加字段：
   ```sql
   ALTER TABLE your_table 
   ADD COLUMN created_by bigint DEFAULT NULL COMMENT '创建人ID',
   ADD COLUMN updated_by bigint DEFAULT NULL COMMENT '更新人ID';
   ```

### Q: uid 字段如何迁移？

A: 
1. **添加字段**：执行 DDL 脚本添加 `uid` 字段（允许为空）
   ```sql
   ALTER TABLE your_table 
   ADD COLUMN uid VARCHAR(64) NULL COMMENT '逻辑主键';
   ```

2. **回填数据**：为现有记录生成并回填 uid
   ```sql
   UPDATE your_table SET uid = REPLACE(UUID(), '-', '') WHERE uid IS NULL;
   ```

3. **设置非空**：设置 uid 为 NOT NULL
   ```sql
   ALTER TABLE your_table 
   MODIFY COLUMN uid VARCHAR(64) NOT NULL COMMENT '逻辑主键';
   ```

4. **添加索引**：添加唯一索引
   ```sql
   ALTER TABLE your_table 
   ADD UNIQUE INDEX uk_uid (uid);
   ```

详细迁移脚本请参考：`docs/sql/migration-add-uid-field.sql` 和 `docs/sql/backfill-uid-data.sql`

### Q: 外键关联必须使用 uid 吗？

A: 是的，新规范要求外键关联统一使用 `uid` 字段（`*Uid` 命名），原因：
1. 避免暴露自增 ID 的业务信息（如用户数量、订单数量）
2. 分布式环境下更安全
3. 数据迁移时 ID 可能变化，但 uid 保持不变
4. 更好的业务隔离性

---

## uid 字段迁移指南

### 数据库迁移步骤

1. **执行 DDL 脚本**：为所有表添加 `uid` 字段
   - 参考：`docs/sql/migration-add-uid-field.sql`

2. **回填现有数据**：为现有记录生成 uid
   - 参考：`docs/sql/backfill-uid-data.sql`
   - 大数据量表建议分批处理

3. **验证数据**：确保 uid 唯一且无 NULL 值
   ```sql
   -- 检查重复
   SELECT uid, COUNT(*) as cnt FROM your_table GROUP BY uid HAVING cnt > 1;
   -- 检查 NULL
   SELECT COUNT(*) FROM your_table WHERE uid IS NULL;
   ```

### 代码迁移步骤

1. **更新外键字段**：将 `*Id` 改为 `*Uid`（String 类型）
   ```java
   // 旧代码
   private Long userId;
   
   // 新代码
   private String userUid;
   ```

2. **更新查询逻辑**：使用 uid 进行关联查询
   ```java
   // 旧代码
   .eq(SysUserRoleDO::getUserId, userId)
   
   // 新代码
   .eq(SysUserRoleDO::getUserUid, userUid)
   ```

3. **更新 Service 层**：使用 uid 进行业务操作
   ```java
   // 旧代码
   SysUserDO user = sysUserDao.selectById(userId);
   
   // 新代码
   SysUserDO user = sysUserDao.selectOne(
       Wrappers.lambdaQuery(SysUserDO.class)
           .eq(SysUserDO::getUid, userUid)
   );
   ```

详细使用指南请参考：`docs/uid-usage-guide.md`
