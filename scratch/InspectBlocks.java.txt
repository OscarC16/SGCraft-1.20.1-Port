package gcewing.sgcraft;

import java.lang.reflect.Method;

public class InspectBlocks {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("net.minecraft.world.level.block.state.BlockBehaviour");
            System.out.println("=================================================================");
            System.out.println("COMPACT BlockBehaviour methods taking BlockState:");
            System.out.println("=================================================================");
            
            for (Method method : clazz.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length > 0 && params[0].getName().contains("BlockState")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(method.getReturnType().getSimpleName()).append(" ").append(method.getName()).append("(");
                    for (int i = 0; i < params.length; i++) {
                        sb.append(params[i].getSimpleName());
                        if (i < params.length - 1) sb.append(", ");
                    }
                    sb.append(")");
                    System.out.println("  " + sb.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
