# winter-validation-spring-boot-starter

增强 Spring Boot 参数校验的 Starter，提供动态枚举校验和 SpEL 表达式校验能力。

## 特性

- **@DynamicEnum** - 动态枚举值校验，支持从外部字典服务获取允许值
- **@SpelValid** - SpEL 表达式校验，支持复杂业务规则和跨字段校验
- **双模式校验器** - 全量校验（默认）和快速失败校验
- **完整国际化支持** - 集成 Spring MessageSource
- **依赖注入支持** - 自定义校验器可注入 Service/Mapper

## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.hahaha-zsq</groupId>
    <artifactId>winter-validation-spring-boot-starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.hahaha-zsq:winter-validation-spring-boot-starter:0.0.3'
```

## 快速开始

### 1. 定义实体类

```java
import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

@Data
public class UserRequest {
    
    @DynamicEnum(dictType = "user_status", allowNull = false)
    private String status;
    
    @SpelValid(value = "#root.age >= #root.minAge", description = "年龄必须达到最低要求")
    private Integer age;
    
    private Integer minAge;
}
```

### 2. 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public String createUser(@Valid @RequestBody UserRequest request) {
        return "success";
    }
}
```

---

## @DynamicEnum 动态枚举校验

用于校验字段值是否在允许的字典值列表中，支持从外部动态获取。

### 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dictType` | String | - | 字典类型标识（必填） |
| `reverse` | boolean | false | 是否反转枚举值含义 |
| `fixedValues` | String[] | {} | 固定的枚举值补充 |
| `allowNull` | boolean | false | 是否允许为 null |
| `allowEmpty` | boolean | false | 是否允许空字符串 |

### 实现字典数据提供者

```java
@Service
public class DictService implements DictDataProvider {
    
    @Override
    public Collection<String> getDictValues(String dictType, boolean reverse) {
        // 从数据库、Redis 或配置中心获取字典值
        if ("user_status".equals(dictType)) {
            return Arrays.asList("active", "inactive", "pending");
        }
        return Collections.emptyList();
    }
}
```

### 完整示例

```java
@Data
public class OrderRequest {
    
    // 校验值必须在字典 "order_status" 中，且不能为空
    @DynamicEnum(dictType = "order_status", allowNull = false)
    private String orderStatus;
    
    // 校验值必须在字典 "payment_method" 中，允许为空
    @DynamicEnum(dictType = "payment_method", allowEmpty = true)
    private String paymentMethod;
    
    // 固定值 + 动态字典值
    @DynamicEnum(dictType = "product_type", fixedValues = {"CUSTOM", "SPECIAL"})
    private String productType;
}
```

---

## @SpelValid SpEL 表达式校验

使用 Spring Expression Language 进行复杂业务规则校验。

### 适用场景

- 跨字段校验（如结束时间 > 开始时间）
- 条件逻辑校验（如 VIP 用户无限制，普通用户有上限）
- 复杂计算校验（如金额 = 单价 × 数量）

### 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | - | SpEL 表达式（必填） |
| `description` | String | "" | 描述信息 |

### 表达式上下文

| 变量 | 说明 |
|------|------|
| `#root` | 当前校验对象（整个实体） |
| `#this` | 当前字段值 |
| `#this['fieldName']` | 访问当前对象的其他字段 |

### 字段级别校验

```java
@Data
public class UserRequest {
    
    @SpelValid(value = "#this >= 18", description = "年龄必须大于等于18")
    private Integer age;
    
    @SpelValid(value = "#this.length() >= 6 && #this.length() <= 20", 
               description = "用户名长度6-20位")
    private String username;
}
```

### 类级别校验（跨字段）

```java
@SpelValid.List({
    @SpelValid(value = "#root.startDate.before(#root.endDate)", 
               description = "开始时间必须早于结束时间"),
    @SpelValid(value = "#root.minAmount <= #root.maxAmount", 
               description = "最小金额不能大于最大金额")
})
@Data
public class QueryRequest {
    
    private Date startDate;
    private Date endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
```

### 重复注解

```java
@Data
public class ProductRequest {
    
    @SpelValid.List({
        @SpelValid(value = "#this > 0", description = "价格必须大于0"),
        @SpelValid(value = "#this < 100000", description = "价格不能超过10万")
    })
    private BigDecimal price;
}
```

---

## 校验器选择

### 全量校验器（默认）

返回所有字段的错误信息，适用于表单提交场景。

```java
@RestController
public class UserController {
    
    @Autowired
    private Validator validator;  // 默认注入 fastFalseValidator
    
    public String createUser(@RequestBody UserRequest request) {
        Set<ConstraintViolation<UserRequest>> errors = validator.validate(request);
        // 返回所有错误
    }
}
```

### 快速失败校验器

遇到第一个错误即返回，适用于批量导入场景。

```java
@RestController
public class ImportController {
    
    @Autowired
    @Qualifier("fastTrueValidator")
    private Validator fastValidator;
    
    public String importData(@RequestBody List<DataRow> rows) {
        for (DataRow row : rows) {
            Set<ConstraintViolation<DataRow>> errors = fastValidator.validate(row);
            if (!errors.isEmpty()) {
                return errors.iterator().next().getMessage();  // 返回第一个错误
            }
        }
    }
}
```

---

## 国际化配置

在 `messages.properties`（或 messages_zh_CN.properties）中配置错误消息：

```properties
# 默认消息
jakarta.validation.constraints.DynamicEnum.message=值不在允许的范围内
jakarta.validation.constraints.SpelValid.message=SpEL 表达式校验失败

# 带参数的动态消息
custom.status.message=状态必须是以下值之一: {allowedValues}
```

---

## 常见问题

### 校验不生效

确保在 Controller 方法参数上添加 `@Valid` 注解：

```java
public String createUser(@Valid @RequestBody UserRequest request) {
```

### 动态字典值为空

检查 `DictDataProvider` 实现是否正确注册为 Spring Bean，且 `dictType` 是否匹配。

### SpEL 表达式启动报错

检查 SpEL 表达式语法，确保字段名与实体类属性一致。

---

## License

Apache License 2.0
