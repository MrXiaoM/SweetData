package top.mrxiaom.sweetdata;

import top.mrxiaom.pluginbase.func.language.IHolderAccessor;
import top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder;

import java.util.List;

import static top.mrxiaom.pluginbase.func.language.LanguageEnumAutoHolder.wrap;

public enum Messages implements IHolderAccessor {
    command__global__get__success("&a全局数值 &e%key%=%value%&a."),
    command__global__get__not_found("&a没有设置全局数值&e %key%&a."),
    command__global__unsafe("&e已在 spigot.yml 开启 bungeecord 模式但未开启 unsafe-mode，不允许在无人情况下对全局数值进行操作"),
    command__global__set__success("&a已设置全局数值&e %key%=%value%&a."),
    command__global__remove__success("&a已移除全局数值&e %key%&a."),
    command__global__plus__not_integer("&e无效的数值 %input%"),
    command__global__plus__success("&a已为全局数值增加&e %added%&a，最终&e %key%&f=&e%value%&a."),
    command__global__plus__fail("&e为全局数值&b %key% &e增加&b %added% &e失败，找不到已设置的数值"),
    command__global__add__not_integer("&e无效的数值 %input%"),
    command__global__add__success("&a已为全局数值增加&e %added%&a，最终&e %key%&f=&e%value%&a."),
    command__player_not_found("&e指定的玩家不存在 &7(%player%)"),
    command__get__success("&a玩家&e %player% &a的数值&e %key%=%value%&a."),
    command__get__not_found("&a玩家&e %player% &a没有设置数值&e %key%"),
    command__set__success("&a已设置玩家&e %player% &a的数值&e %key%=%value%&a."),
    command__remove__success("&a已移除玩家&e %player% &a的数值&e %key%&a."),
    command__clear__success("&a已清空玩家&e %player% &a的数值."),
    command__plus__not_integer("&e无效的数值 %input%"),
    command__plus__success("&a已为玩家&e %player% &a的数值增加&e %added%&a，最终&e %key%&f=&e%value%&a."),
    command__plus__fail("&e为玩家&b %player% &e的数值&b %key% &e增加&b %added% &e失败，找不到已设置的数值"),
    command__add__not_integer("&e无效的数值 %input%"),
    command__add__success("&a已为玩家&e %player% &a的数值增加&e %added%&a，最终&e %key%&f=&e%value%&a."),
    command__reload__database("&a已重新连接数据库."),
    command__reload__normal("&a配置文件已重载."),
    command__help("&e&lSweetData 全局数据命令&r",
            "  &f/data <global/g> get <键> &7获取全局数值",
            "  &f/data <global/g> set <键> <值> &7设置全局数值",
            "  &f/data <global/g> plus <键> <值> &7如果全局数值是整数，增加玩家的数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作",
            "  &f/data <global/g> add <键> <值> &7增加全局整数数值(可以为负数)，如果数值不存在，或者不是整数，则取0再操作",
            "  &f/data <global/g> <remove/del> <键> &7移除全局数值",
            "&e&lSweetData 玩家数据命令&r",
            "  &f/data get <玩家名> <键> &7获取玩家的数值",
            "  &f/data set <玩家名> <键> <值> &7设置玩家的数值",
            "  &f/data plus <玩家名> <键> <值> &7如果数值是整数，增加玩家的数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作",
            "  &f/data add <键> <值> &7增加玩家的整数数值(可以为负数)，如果数值不存在，或者不是整数，则取0再操作",
            "  &f/data <remove/del> <玩家名> <键> &7移除玩家的数值",
            "&e&lSweetData 管理命令&r",
            "  &f/data reload database &7重新连接数据库，并刷新所有缓存",
            "  &f/data reload &7重载配置文件");

    ;
    Messages(String defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(String... defaultValue) {
        holder = wrap(this, defaultValue);
    }
    Messages(List<String> defaultValue) {
        holder = wrap(this, defaultValue);
    }
    private final LanguageEnumAutoHolder<Messages> holder;
    public LanguageEnumAutoHolder<Messages> holder() {
        return holder;
    }
}
