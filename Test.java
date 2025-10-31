import org.bukkit.entity.EntityType; public class Test { public static void main(String[] args) { for (EntityType type : EntityType.values()) { if (type.name().contains(\
TNT\)) System.out.println(type.name()); } } }
