# winter-validation 架构设计

深入了解校验框架的核心设计理念与技术实现。

## 设计目标

传统校验工具类（如 `ValidatorUtil`）存在以下问题：
- 静态方法无法依赖 Spring 容器中的 Bean
- 难以集成国际化（i18n）功能
- 无法在不同场景下灵活切换校验策略

winter-validation 旨在解决这些问题，提供：
- 完全基于 Spring 容器管理
- 完整的国际化支持
- 灵活的双模式校验策略
- 支持依赖注入的自定义校验器

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户项目                                  │
│  ┌─────────────────┐    ┌─────────────────────────────────┐   │
│  │   Controller    │───▶│  @Valid 触发校验                  │   │
│  └─────────────────┘    └─────────────────────────────────┘   │
│                                      │                          │
│                                      ▼                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ValidationAutoConfiguration                │   │
│  │  ┌───────────────────┐      ┌───────────────────────┐   │   │
│  │  │ fastFalseValidator │      │ fastTrueValidator    │   │   │
│  │  │  (全量校验)         │      │ (快速失败校验)         │   │   │
│  │  └───────────────────┘      └───────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                      │                          │
│            ┌─────────────────────────┼─────────────────────┐   │
│            ▼                         ▼                     ▼   │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │@DynamicEnum     │    │@SpelValid       │    │ 自定义校验器   │ │
│  │ 动态枚举校验    │    │ SpEL表达式校验  │    │ (可注入Bean)  │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│            │                         │                │         │
│            ▼                         │                │         │
│  ┌─────────────────┐                │                │         │
│  │ DictDataProvider│                │                │         │
│  │ (用户提供)       │◀───────────────┘                │         │
│  └─────────────────┘                                 │         │
│            │                                         │         │
│            ▼                                         ▼         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              外部数据源 (数据库/Redis/配置中心)           │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心组件

### 1. 注解层

| 注解 | 职责 | 适用场景 |
|------|------|----------|
| `@DynamicEnum` | 动态枚举值校验 | 字段值必须在指定字典范围内 |
| `@SpelValid` | SpEL 表达式校验 | 复杂业务规则、跨字段校验 |

### 2. 校验器层

```
┌─────────────────────┐     ┌─────────────────────┐
│ ConstraintValidator │◀────│ DynamicEnum         │
│ (JSR-303 接口)       │     │ (实现类)             │
└─────────────────────┘     └─────────────────────┘
            ▲
            │ 继承
            │     ┌─────────────────────┐
            └─────│ SpelValidator       │
                  │ (实现类)             │
                  └─────────────────────┘
```

### 3. 配置层

`ValidationAutoConfiguration` 提供两个 Bean：

| Bean 名称 | 模式 | fail_fast | 适用场景 |
|-----------|------|-----------|----------|
| `fastFalseValidator` | 全量校验 | false | 表单提交，一次性显示所有错误 |
| `fastTrueValidator` | 快速失败 | true | 批量导入，遇到首个错误即返回 |

### 4. 扩展层

用户通过实现 `DictDataProvider` 接口提供动态字典数据：

```java
public interface DictDataProvider {
    Collection<String> getDictValues(String dictType, boolean reverse);
}
```

---

## 核心流程

### 校验流程图

```
用户请求
    │
    ▼
@Valid 注解触发
    │
    ▼
Spring 加载 LocalValidatorFactoryBean
    │
    ├──▶ 查找 @Constraint 注解
    │
    ├──▶ 获取对应的 ConstraintValidator
    │
    ├──▶ 调用 initialize() 初始化
    │
    ├──▶ 调用 isValid() 执行校验
    │
    ▼
返回校验结果
```

### @DynamicEnum 校验流程

```
@DynamicEnum(dictType = "user_status")
    │
    ▼
DynamicEnumValidator.initialize()
    │
    ├── 读取 dictType
    ├── 读取 fixedValues
    └── 缓存参数
    │
    ▼
isValid() 被调用
    │
    ├── DictDataProvider.getDictValues("user_status")
    │       │
    │       ▼
    │   从数据库/Redis获取字典值
    │
    ├── 合并动态值 + 固定值
    │
    ├── 校验输入是否在允许范围内
    │
    └── 返回校验结果
```

---

## 设计模式

### 1. 策略模式

两种校验策略（全量/快速失败）通过不同的 Bean 实现：

```java
@Bean("fastFalseValidator")  // 策略A
public LocalValidatorFactoryBean fastFalseValidator(...) {}

@Bean("fastTrueValidator")  // 策略B  
public LocalValidatorFactoryBean fastTrueValidator(...) {}
```

用户可按需注入不同策略。

### 2. 责任链模式（扩展）

`@SpelValid` 支持重复注解，可链式添加多个校验规则：

```java
@SpelValid.List({
    @SpelValid(value = "#this > 0", description = "必须为正数"),
    @SpelValid(value = "#this < 10000", description = "不能超过10000")
})
private BigDecimal price;
```

### 3. 模板方法模式

校验器实现 `ConstraintValidator` 接口，定义了校验的骨架：

```java
public interface ConstraintValidator<A extends Annotation, T> {
    void initialize(A constraintAnnotation);  // 初始化模板
    boolean isValid(T value, Context context); // 校验模板
}
```

---

## 关键设计决策

### 1. 解决循环依赖

使用 `@Lazy` 解决 `MessageSource → Repository → Validator` 死循环：

```java
public LocalValidatorFactoryBean fastFalseValidator(
    @Lazy MessageSource messageSource  // 延迟注入
) {
    validatorFactoryBean.setValidationMessageSource(messageSource);
}
```

### 2. SpEL 表达式缓存

表达式在初始化时解析并缓存，避免重复解析：

```java
private Expression cachedExpression;

public void initialize(SpelValid annotation) {
    // 启动时解析一次，后续复用
    this.cachedExpression = PARSER.parseExpression(annotation.value());
}
```

### 3. 灵活的空值处理

支持独立配置 `allowNull` 和 `allowEmpty`：

| 值 | allowNull | allowEmpty | 说明 |
|----|-----------|------------|------|
| `null` | true | - | 允许 |
| `null` | false | - | 不允许 |
| `""` | - | true | 允许空字符串 |
| `""` | - | false | 不允许空字符串 |

---

## 扩展点

### 1. 自定义校验注解

```java
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface CheckPhone {
    String message() default "手机号格式错误";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@Component
public class PhoneValidator 
    implements ConstraintValidator<CheckPhone, String> {
    
    @Autowired  // 可注入 Service
    private UserService userService;
    
    @Override
    public boolean isValid(String value, Context context) {
        // 校验逻辑
    }
}
```

### 2. 自定义字典数据源

```java
@Service
public class RedisDictProvider implements DictDataProvider {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    @Override
    public Collection<String> getDictValues(String dictType, boolean reverse) {
        return redis.opsForSet().members("dict:" + dictType);
    }
}
```

---

## 依赖关系

```
spring-boot-starter-validation
    │
    ├── javax.validation (JSR-303)
    │
    └── hibernate-validator
            │
            └── @Constraint, ConstraintValidator

spring-context
    │
    ├── @Configuration
    │
    ├── @Bean
    │
    └── LocalValidatorFactoryBean

spring-expression
    │
    ├── SpelExpressionParser
    │
    └── StandardEvaluationContext
```

---

## 版本历史

| 版本 | 主要变更 |
|------|----------|
| 0.0.3 | 添加 allowEmpty 参数支持 |
| 0.0.2 | 支持 SpEL 表达式校验 |
| 0.0.1 | 初始版本，支持动态枚举 |
