# SweetData

Minecraft 通用数值同步插件

## 简介

这个插件用于储存和读取自定义数值，方便服主或者其它插件进行读取和写入操作，并且支持全服同步。

## 命令

根命令为 `/sweetdata`，别名为 `/sdata`, `/data`，不建议给玩家任何权限。

**全局数据命令**
| 命令 | 描述 | 权限 |
| --- | --- | --- |
| `/data <global/g> get <玩家名> <键>` | 获取全局数值 | `sweet.data.global.get` |
| `/data <global/g> set <玩家名> <键> <值>` | 设置全局数值 | `sweet.data.global.set` |
| `/data <global/g> plus <玩家名> <键> <值>` | 如果全局数值是整数，增加数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作 | `sweet.data.global.plus` |
| `/data <global/g> add <玩家名> <键> <值>` | 增加全局整数数值(可以为负数)，如果数值不存在，或者不是整数，则取0再操作 | `sweet.data.global.add` |
| `/data <global/g> <remove/del> <玩家名> <键>` | 移除全局数值 | `sweet.data.global.del` |

**玩家数据命令**
| 命令 | 描述 | 权限 |
| --- | --- | --- |
| `/data get <玩家名> <键>` | 获取玩家的数值 | `sweet.data.player.get` |
| `/data set <玩家名> <键> <值>` | 设置玩家的数值 | `sweet.data.player.set` |
| `/data plus <玩家名> <键> <值>` | 如果数值是整数，增加玩家的数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作 | `sweet.data.player.plus` |
| `/data add <玩家名> <键> <值>` | 增加玩家的整数数值(可以为负数)，如果数值不存在，或者不是整数，则取0再操作 | `sweet.data.player.add` |
| `/data <remove/del> <玩家名> <键>` | 移除玩家的数值 | `sweet.data.player.del` |

**通用管理命令**
| 命令 | 描述 | 权限 |
| --- | --- | --- |
| `/data` | 查看帮助命令 | `sweet.data.help` |
| `/data reload database` | 重新连接数据库，并刷新所有缓存 | `sweet.data.reload` |
| `/data reload` | 重载配置文件 | `sweet.data.reload` |

请尽量不要以 `global_` 开头作为玩家数据键名。

## 已知问题

如果在`子服A`设置离线玩家的数值，而该玩家在`子服B`在线，那么将会出现数据不同步的情况。

我在我的服务器中没有设置离线玩家数值的需要，仅公示这个问题，这个设计缺陷暂时不会去解决。

## PAPI 变量

+ `%sweetdata_<键>[;<默认值>]%` 获取玩家数值
+ `%sweetdata_$range,<最小值>,<最大值>$<键>[;<默认值>]%` 判定玩家数值是否是整数，且是否在范围内
+ `%sweetdata_@<玩家名>@_<键>[;<默认值>]%` 获取指定玩家数值
+ `%sweetdata_@<玩家名>@_$range,<最小值>,<最大值>$<键>[;<默认值>]%` 判定指定玩家数值是否是整数，且是否在范围内
+ `%sweetdata_global_<键>[;<默认值>]%` 获取全局数值
+ `%sweetdata_global_$range,<最小值>,<最大值>$<键>[;<默认值>]%` 判定全局数值是否是整数，且是否在范围内

示例
```
%sweetdata_my-key%               如果 my-key 为 1，则输出 1，未设置则输出空字符串
%sweetdata_my-key;0%             如果 my-key 未设置，则输出 0
%sweetdata_$range,0,9$my-key;0%  如果 my-key 未设置，或者不是整数，或者不在 0-9 范围内，则输出 no；反之输出 yes
输出的 yes 和 no 可以在 PlaceholderAPI 的 config.yml 中修改

%sweetdata_@LittleCatX@_my-key%  同上，获取玩家 LittleCatX 的数值
%sweetdata_global_my-key%`       同上，获取全局数值
```
